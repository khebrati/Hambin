package top.roomio.data.party

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.RTCStatsCollectorCallback
import org.webrtc.RTCStatsReport
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoTrack
import org.webrtc.audio.AudioDeviceModule
import org.webrtc.audio.JavaAudioDeviceModule
import top.roomio.data.party.dto.VoiceConfigurationDto
import top.roomio.data.party.dto.VoiceConfigurationRequest
import top.roomio.data.party.dto.toDomain
import top.roomio.domain.party.PartyException
import top.roomio.domain.party.SessionRepository
import top.roomio.domain.party.VoiceClient
import top.roomio.domain.party.VoiceConfiguration
import top.roomio.domain.party.VoiceEvent
import top.roomio.domain.party.VoiceSession
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val voiceLog = Logger.withTag("MeshVoice")

internal actual fun createVoiceClient(
    http: HttpClient,
    config: BackendConfig,
    json: Json,
    sessions: SessionRepository,
    tokens: TokenStore,
): VoiceClient = MeshVoiceClient(AppContextHolder.appContext, http, config, json, sessions, tokens)

/** Android's microphone-only, room-local peer mesh. No media service is involved. */
@SingleIn(AppScope::class)
@Inject
internal class MeshVoiceClient(
    private val context: Context,
    private val http: HttpClient,
    private val backend: BackendConfig,
    private val json: Json,
    private val sessions: SessionRepository,
    private val tokens: TokenStore,
) : VoiceClient {
    private val activeMutex = Mutex()
    private var active: MeshVoiceSession? = null

    override suspend fun connect(configuration: VoiceConfiguration): VoiceSession = activeMutex.withLock {
        active?.disconnect()
        require(configuration.mode == "MESH") { "Voice mesh is unavailable" }
        require(configuration.namespace == "/hambin/voice/${configuration.roomId}") {
            "Backend returned an unexpected voice namespace"
        }
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            throw PartyException("VOICE_PERMISSION_REQUIRED", "Allow microphone access to join voice")
        }
        val session = MeshVoiceSession(context.applicationContext, http, backend, json, sessions, tokens, configuration) {
            if (active === it) active = null
        }
        try {
            session.open()
            active = session
            session
        } catch (error: Throwable) {
            session.disconnect()
            throw error
        }
    }
}

private data class VoiceMember(val membershipId: String, val generation: String, val muted: Boolean)

private class PeerState(
    val member: VoiceMember,
    var peer: PeerConnection,
    val pendingIce: ArrayDeque<IceCandidate> = ArrayDeque(),
    var remoteDescriptionSet: Boolean = false,
    var negotiationGeneration: Long = 0,
    var retries: Int = 0,
    @Volatile var connected: Boolean = false,
    @Volatile var iceState: PeerConnection.IceConnectionState = PeerConnection.IceConnectionState.NEW,
    val energyDelta: AudioEnergyDelta = AudioEnergyDelta(),
)

private class MeshVoiceSession(
    private val context: Context,
    private val http: HttpClient,
    private val backend: BackendConfig,
    private val json: Json,
    private val sessions: SessionRepository,
    private val tokens: TokenStore,
    private var descriptor: VoiceConfiguration,
    private val onReleased: (MeshVoiceSession) -> Unit,
) : VoiceSession {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val output = Channel<VoiceEvent>(capacity = 32)
    override val events: Flow<VoiceEvent> = output.receiveAsFlow()
    private val peers = linkedMapOf<String, PeerState>()
    private val memberStates = linkedMapOf<String, VoiceMember>()
    private val retryingPeers = mutableSetOf<String>()
    private val activityTracker = VoiceActivityTracker()
    private val speakingLock = Any()
    private val eventMutex = Mutex()
    private val initialRoster = CompletableDeferred<Unit>()
    private val ready = CompletableDeferred<Unit>()
    private var connection: SocketIoConnection? = null
    private var namespace: SocketIoNamespace? = null
    private var factory: PeerConnectionFactory? = null
    private var audioDevice: AudioDeviceModule? = null
    private var source: AudioSource? = null
    private var track: AudioTrack? = null
    @Volatile private var microphoneEnabled = false
    @Volatile private var closed = false

    suspend fun open() {
        ensureRtc()
        val socket = http.webSocketSession(socketIoUrl(backend.baseUrl))
        val engine = SocketIoConnection(socket, json)
        connection = engine
        try {
            engine.awaitOpen()
            namespace = engine.connectNamespace(descriptor.namespace, descriptor.ticket)
        } catch (error: Throwable) {
            engine.close()
            throw error
        }
        scope.launch { receiveEvents() }
        scope.launch { refreshCredentials() }
        scope.launch { publishSpeakingLevels() }
        withTimeout(MESH_CONNECT_TIMEOUT_MS) { ready.await() }
    }

    private fun ensureRtc() {
        synchronized(RTC_LOCK) {
            if (factory != null) return
            PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(context).createInitializationOptions(),
            )
            audioDevice = JavaAudioDeviceModule.builder(context).createAudioDeviceModule()
            (audioDevice as? JavaAudioDeviceModule)?.setSpeakerMute(false)
            factory = PeerConnectionFactory.builder().setAudioDeviceModule(audioDevice).createPeerConnectionFactory()
            source = factory!!.createAudioSource(MediaConstraints())
            track = factory!!.createAudioTrack("hambin-microphone", source).apply { setEnabled(false) }
        }
    }

    private suspend fun receiveEvents() {
        val ns = namespace ?: return
        try {
            for (event in ns.events) {
                when (event.name) {
                    "voice.roster" -> applyRoster(event.payload)
                    "signal" -> receiveSignal(event.payload)
                    "error" -> {
                        val code = event.payload.jsonObject["code"]?.jsonPrimitive?.contentOrNull
                        if (code == "SESSION_REVOKED" || code == "INVALID_TICKET") fail("Voice session expired")
                    }
                }
            }
            if (!closed) fail("Voice connection ended")
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (error: Throwable) {
            if (!closed) fail("Voice connection was interrupted")
        }
    }

    private suspend fun applyRoster(payload: JsonElement) = eventMutex.withLock {
        val roster = payload.jsonObject["members"]?.jsonArray.orEmpty().mapNotNull { item ->
            val obj = item.jsonObject
            val id = obj["membershipId"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val generation = obj["voiceSessionId"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val muted = obj["muted"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: true
            VoiceMember(id, generation, muted)
        }.filter { it.membershipId != descriptor.membershipId }
        if (roster.size > MAX_REMOTE_PEERS) {
            throw PartyException("VOICE_CAPACITY_EXCEEDED", "This voice room has too many participants")
        }
        val authoritative = roster.associateBy { it.membershipId }
        peers.keys.toList().filter { it !in authoritative }.forEach { id ->
            removePeer(id)
            clearSpeaking(id)
        }
        memberStates.keys.toList().filter { it !in authoritative }.forEach(memberStates::remove)
        for (member in roster) {
            val previous = memberStates[member.membershipId]
            if (previous != null && previous.generation != member.generation) removePeer(member.membershipId)
            memberStates[member.membershipId] = member
            if (member.membershipId !in peers) createPeer(member)
        }
        initialRoster.complete(Unit)
        publishConnectedWhenMeshReady()
    }

    private fun createPeer(member: VoiceMember, retries: Int = 0) {
        if (peers.size >= MAX_REMOTE_PEERS) throw PartyException("VOICE_CAPACITY_EXCEEDED", "This voice room has too many participants")
        val rtc = factory ?: throw PartyException("VOICE_UNAVAILABLE", "Voice is unavailable")
        val rtcConfig = rtcConfiguration()
        var state: PeerState? = null
        val peer = rtc.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState) = Unit
            override fun onIceConnectionChange(stateValue: PeerConnection.IceConnectionState) {
                state?.iceState = stateValue
                if (stateValue == PeerConnection.IceConnectionState.CONNECTED || stateValue == PeerConnection.IceConnectionState.COMPLETED) {
                    state?.connected = true
                    state?.retries = 0
                    synchronized(retryingPeers) { retryingPeers.remove(member.membershipId) }
                    publishConnectedWhenMeshReady()
                } else if (stateValue == PeerConnection.IceConnectionState.FAILED) {
                    state?.connected = false
                    scope.launch { retryPeer(member.membershipId, notifyRemote = true) }
                } else if (stateValue == PeerConnection.IceConnectionState.DISCONNECTED) {
                    scope.launch { retryIfStillDisconnected(member.membershipId, state) }
                } else if (stateValue == PeerConnection.IceConnectionState.CLOSED && peers[member.membershipId] === state) {
                    scope.launch { retryPeer(member.membershipId, notifyRemote = true) }
                }
            }
            override fun onStandardizedIceConnectionChange(stateValue: PeerConnection.IceConnectionState) = Unit
            override fun onConnectionChange(stateValue: PeerConnection.PeerConnectionState) {
                if (stateValue == PeerConnection.PeerConnectionState.CONNECTED) {
                    state?.connected = true
                    state?.retries = 0
                    synchronized(retryingPeers) { retryingPeers.remove(member.membershipId) }
                    publishConnectedWhenMeshReady()
                } else if (stateValue == PeerConnection.PeerConnectionState.FAILED) {
                    state?.connected = false
                    scope.launch { retryPeer(member.membershipId, notifyRemote = true) }
                } else if (stateValue == PeerConnection.PeerConnectionState.DISCONNECTED) {
                    state?.iceState = PeerConnection.IceConnectionState.DISCONNECTED
                    scope.launch { retryIfStillDisconnected(member.membershipId, state) }
                } else if (stateValue == PeerConnection.PeerConnectionState.CLOSED && peers[member.membershipId] === state) {
                    scope.launch { retryPeer(member.membershipId, notifyRemote = true) }
                }
            }
            override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit
            override fun onIceGatheringChange(stateValue: PeerConnection.IceGatheringState) = Unit
            override fun onIceCandidate(candidate: IceCandidate) {
                val current = state ?: return
                val generation = current.negotiationGeneration
                scope.launch { sendSignal(member, ice = candidate) }
            }
            override fun onIceCandidatesRemoved(candidates: Array<IceCandidate>) = Unit
            override fun onAddStream(stream: org.webrtc.MediaStream) = Unit
            override fun onRemoveStream(stream: org.webrtc.MediaStream) = Unit
            override fun onDataChannel(channel: DataChannel) { channel.dispose() }
            override fun onRenegotiationNeeded() = Unit
            override fun onAddTrack(receiver: RtpReceiver, streams: Array<out org.webrtc.MediaStream>) = Unit
            override fun onTrack(transceiver: org.webrtc.RtpTransceiver) {
                if (transceiver.receiver.track() is VideoTrack) transceiver.receiver.track()?.setEnabled(false)
            }
            override fun onRemoveTrack(receiver: RtpReceiver) = Unit
        }) ?: throw PartyException("VOICE_PEER_FAILED", "Could not create a voice connection")
        val audioTrack = track ?: throw PartyException("VOICE_AUDIO_UNAVAILABLE", "Microphone audio is unavailable")
        val sender = peer.addTrack(audioTrack, listOf("hambin-audio"))
        if (sender == null) {
            peer.close(); peer.dispose()
            throw PartyException("VOICE_PEER_FAILED", "Could not add microphone audio to the voice connection")
        }
        val peerState = PeerState(member, peer, retries = retries)
        state = peerState
        peers[member.membershipId] = peerState
        if (descriptor.membershipId < member.membershipId) scope.launch { negotiateOffer(member.membershipId) }
    }

    private fun rtcConfiguration(): PeerConnection.RTCConfiguration {
        val servers = descriptor.iceServers.flatMap { server ->
            server.urls.map { url ->
                val builder = PeerConnection.IceServer.builder(url)
                server.username?.let(builder::setUsername)
                server.credential?.let(builder::setPassword)
                builder.createIceServer()
            }
        }
        return PeerConnection.RTCConfiguration(servers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }
    }

    private suspend fun negotiateOffer(id: String) {
        val state = peers[id] ?: return
        val generation = ++state.negotiationGeneration
        try {
            val offer = state.peer.createDescription(isOffer = true)
            state.peer.setDescription(local = true, offer)
            sendSignal(state.member, sdp = offer)
        } catch (_: Throwable) {
            retryPeer(id, notifyRemote = true)
        }
    }

    private suspend fun receiveSignal(payload: JsonElement) {
        val envelope = payload.jsonObject
        val from = envelope["from"]?.jsonPrimitive?.contentOrNull ?: return
        val message = envelope["msg"]?.jsonPrimitive?.contentOrNull ?: return
        if (message.length > MAX_SIGNAL_CHARS) return
        val parsed = runCatching { json.parseToJsonElement(message).jsonObject }.getOrNull() ?: return
        val state = peers[from] ?: return
        val senderGeneration = parsed["generation"]?.jsonPrimitive?.contentOrNull ?: return
        val targetGeneration = parsed["targetGeneration"]?.jsonPrimitive?.contentOrNull ?: return
        if (senderGeneration != state.member.generation || targetGeneration != descriptor.voiceSessionId) return
        val sdpObject = parsed["sdp"] as? JsonObject
        if (sdpObject != null) {
            val type = sdpObject["type"]?.jsonPrimitive?.contentOrNull ?: return
            val sdp = sdpObject["sdp"]?.jsonPrimitive?.contentOrNull ?: return
            if (sdp.length > MAX_SIGNAL_CHARS || sdp.contains("m=video")) return
            val sessionType = when (type.lowercase()) {
                "offer" -> SessionDescription.Type.OFFER
                "answer" -> SessionDescription.Type.ANSWER
                else -> return
            }
            val description = SessionDescription(sessionType, sdp)
            state.negotiationGeneration++
            state.peer.setDescription(local = false, description)
            state.remoteDescriptionSet = true
            while (state.pendingIce.isNotEmpty()) state.peer.addIceCandidate(state.pendingIce.removeFirst())
            if (description.type == SessionDescription.Type.OFFER) {
                val answer = state.peer.createDescription(isOffer = false)
                state.peer.setDescription(local = true, answer)
                sendSignal(state.member, sdp = answer)
            }
        } else {
            val ice = parsed["ice"] as? JsonObject
            if (ice != null) {
                val candidate = ice["candidate"]?.jsonPrimitive?.contentOrNull ?: return
                val mid = ice["sdpMid"]?.jsonPrimitive?.contentOrNull
                val line = ice["sdpMLineIndex"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0
                if (candidate.length > 4_096) return
                val value = IceCandidate(mid, line, candidate)
                if (state.remoteDescriptionSet) state.peer.addIceCandidate(value)
                else if (state.pendingIce.size < MAX_PENDING_ICE) state.pendingIce.addLast(value)
            } else if (parsed["restart"]?.jsonPrimitive?.contentOrNull == "true") {
                retryPeer(from, notifyRemote = false)
            }
        }
    }

    private suspend fun sendSignal(
        member: VoiceMember,
        sdp: SessionDescription? = null,
        ice: IceCandidate? = null,
        restart: Boolean = false,
    ) {
        val message = buildJsonObject {
            put("generation", JsonPrimitive(descriptor.voiceSessionId))
            put("targetGeneration", JsonPrimitive(member.generation))
            if (sdp != null) put("sdp", buildJsonObject {
                put("type", JsonPrimitive(sdp.type.canonicalForm()))
                put("sdp", JsonPrimitive(sdp.description))
            })
            if (ice != null) put("ice", buildJsonObject {
                put("candidate", JsonPrimitive(ice.sdp))
                put("sdpMid", ice.sdpMid?.let(::JsonPrimitive) ?: JsonNull)
                put("sdpMLineIndex", JsonPrimitive(ice.sdpMLineIndex))
            })
            if (restart) put("restart", JsonPrimitive(true))
        }
        val encoded = json.encodeToString(JsonObject.serializer(), message)
        if (encoded.length > MAX_SIGNAL_CHARS) return
        val payload = buildJsonObject {
            put("to", JsonPrimitive(member.membershipId))
            put("msg", JsonPrimitive(encoded))
        }
        runCatching { namespace?.emit("signal", payload) }
    }

    private suspend fun retryPeer(id: String, notifyRemote: Boolean) {
        if (!synchronized(retryingPeers) { retryingPeers.add(id) }) return
        val state = peers[id] ?: run {
            synchronized(retryingPeers) { retryingPeers.remove(id) }
            return
        }
        if (state.retries >= MAX_PEER_RETRIES) {
            fail("Could not connect to every voice participant")
            return
        }
        state.retries++
        val member = memberStates[id] ?: state.member
        if (notifyRemote) sendSignal(member, restart = true)
        peers.remove(id)
        state.peer.close(); state.peer.dispose()
        createPeer(member, retries = state.retries)
        delay(RETRY_DELAY_MS * state.retries)
        if (descriptor.membershipId < member.membershipId) negotiateOffer(id)
    }

    private suspend fun retryIfStillDisconnected(id: String, observed: PeerState?) {
        if (observed == null) return
        delay(DISCONNECTED_GRACE_MS)
        if (!closed && peers[id] === observed &&
            observed.iceState == PeerConnection.IceConnectionState.DISCONNECTED
        ) retryPeer(id, notifyRemote = true)
    }

    private fun removePeer(id: String) {
        peers.remove(id)?.let { state ->
            state.peer.close()
            state.peer.dispose()
            state.pendingIce.clear()
        }
    }

    override fun setMicrophoneEnabled(enabled: Boolean) {
        microphoneEnabled = enabled
        track?.setEnabled(enabled)
        if (!enabled) clearSpeaking(descriptor.membershipId)
        scope.launch {
            // Muting capture must not mute the remote speaker path or request
            // audio focus from the movie player.
            (audioDevice as? JavaAudioDeviceModule)?.setSpeakerMute(false)
            runCatching {
                namespace?.emit("voice.mute", buildJsonObject { put("muted", JsonPrimitive(!enabled)) })
            }.onFailure { if (!closed) voiceLog.w("mute state could not be sent") }
        }
    }

    private suspend fun refreshCredentials() {
        while (scope.isActive && !closed) {
            delay(5 * 60_000L)
            try {
                sessions.refresh()
                val credentials = tokens.credentials() ?: throw PartyException("SESSION_EXPIRED", "Voice session expired")
                val authAck = namespace?.emit("auth.refresh", buildJsonObject {
                    put("idToken", JsonPrimitive(credentials.idToken))
                })?.jsonObject
                val authError = authAck?.get("code")?.jsonPrimitive?.contentOrNull
                if (authError != null) throw PartyException(authError, "Voice session authentication failed")
                val dto = http.post("${backend.baseUrl}/v1/rooms/${descriptor.roomId}/voice/config") {
                    contentType(ContentType.Application.Json)
                    setBody(VoiceConfigurationRequest(descriptor.voiceSessionId))
                }.bodyOrThrow<VoiceConfigurationDto>()
                if (dto.voiceSessionId != descriptor.voiceSessionId || dto.membershipId != descriptor.membershipId) {
                    fail("Voice session was replaced")
                    return
                }
                descriptor = dto.toDomain()
                // Apply refreshed TURN credentials without interrupting active
                // audio; the next ICE restart uses the new server credentials.
                peers.values.forEach { peer ->
                    if (!peer.peer.setConfiguration(rtcConfiguration()))
                        voiceLog.w("refreshed ICE server settings could not be applied to an active peer")
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Throwable) {
                // Existing media remains available through a short API outage.
                delay(30_000L)
            }
        }
    }

    private fun publishConnectedWhenMeshReady() {
        if (closed || ready.isCompleted || !initialRoster.isCompleted) return
        if (memberStates.size != peers.size || peers.values.any { !it.connected }) return
        if (ready.complete(Unit)) output.trySend(VoiceEvent.Connected)
    }

    private suspend fun publishSpeakingLevels() {
        while (scope.isActive && !closed) {
            delay(SPEAKING_POLL_MS)
            val observed = mutableMapOf<String, Boolean>()
            for ((membershipId, state) in peers.toMap()) {
                if (!state.connected) continue
                val levels = withTimeoutOrNull(STATS_CALLBACK_TIMEOUT_MS) { state.peer.readAudioLevels(state.energyDelta) }
                    ?: AudioLevels()
                observed[membershipId] = levels.remote >= SPEAKING_LEVEL_THRESHOLD
                if (microphoneEnabled) {
                    observed[descriptor.membershipId] = observed[descriptor.membershipId] == true ||
                        levels.local >= SPEAKING_LEVEL_THRESHOLD
                }
            }
            if (!microphoneEnabled) clearSpeaking(descriptor.membershipId)
            val ids = synchronized(speakingLock) { (observed.keys + activityTracker.speakers()).toSet() }
            ids.forEach { updateSpeaking(it, observed[it] == true) }
        }
    }

    private fun updateSpeaking(membershipId: String, aboveThreshold: Boolean) = synchronized(speakingLock) {
        if (activityTracker.sample(membershipId, aboveThreshold, nowMs()))
            output.trySend(VoiceEvent.SpeakersChanged(activityTracker.speakers()))
    }

    private fun clearSpeaking(membershipId: String) = synchronized(speakingLock) {
        if (activityTracker.clear(membershipId))
            output.trySend(VoiceEvent.SpeakersChanged(activityTracker.speakers()))
    }

    override suspend fun disconnect() {
        if (closed) return
        closed = true
        microphoneEnabled = false
        track?.setEnabled(false)
        synchronized(speakingLock) {
            if (activityTracker.clearAll()) output.trySend(VoiceEvent.SpeakersChanged(emptySet()))
        }
        scope.cancel()
        runCatching { namespace?.emit("voice.mute", buildJsonObject { put("muted", JsonPrimitive(true)) }) }
        runCatching { namespace?.close() }
        connection?.close()
        peers.keys.toList().forEach(::removePeer)
        track?.dispose(); track = null
        source?.dispose(); source = null
        factory?.dispose(); factory = null
        audioDevice?.release(); audioDevice = null
        output.close()
        onReleased(this)
    }

    private suspend fun fail(reason: String) {
        if (closed) return
        ready.completeExceptionally(PartyException("VOICE_CONNECTION_FAILED", reason))
        output.trySend(VoiceEvent.Failed(reason))
        disconnect()
    }

    private suspend fun PeerConnection.createDescription(isOffer: Boolean): SessionDescription =
        suspendCancellableCoroutine { continuation ->
            val observer = object : SdpObserver {
                override fun onCreateSuccess(description: SessionDescription) {
                    if (continuation.isActive) continuation.resume(description)
                }
                override fun onSetSuccess() = Unit
                override fun onCreateFailure(error: String) {
                    if (continuation.isActive) continuation.resumeWithException(IllegalStateException("SDP creation failed"))
                }
                override fun onSetFailure(error: String) = Unit
            }
            if (isOffer) createOffer(observer, MediaConstraints()) else createAnswer(observer, MediaConstraints())
        }

    private suspend fun PeerConnection.setDescription(local: Boolean, description: SessionDescription) =
        suspendCancellableCoroutine { continuation ->
            val observer = object : SdpObserver {
                override fun onCreateSuccess(description: SessionDescription) = Unit
                override fun onSetSuccess() { if (continuation.isActive) continuation.resume(Unit) }
                override fun onCreateFailure(error: String) = Unit
                override fun onSetFailure(error: String) {
                    if (continuation.isActive) continuation.resumeWithException(IllegalStateException("SDP setup failed"))
                }
            }
            if (local) setLocalDescription(observer, description) else setRemoteDescription(observer, description)
        }

    private companion object {
        const val MAX_REMOTE_PEERS = 7
        const val MAX_PENDING_ICE = 64
        const val MAX_SIGNAL_CHARS = 64 * 1024
        const val MAX_PEER_RETRIES = 3
        const val RETRY_DELAY_MS = 750L
        const val MESH_CONNECT_TIMEOUT_MS = 30_000L
        const val SPEAKING_POLL_MS = 300L
        const val SPEAKING_LEVEL_THRESHOLD = 0.035
        const val STATS_CALLBACK_TIMEOUT_MS = 1_000L
        const val DISCONNECTED_GRACE_MS = 3_000L
        val RTC_LOCK = Any()
    }
}

private data class AudioLevels(val local: Double = 0.0, val remote: Double = 0.0)

private fun nowMs(): Long = kotlin.time.Clock.System.now().toEpochMilliseconds()

private suspend fun PeerConnection.readAudioLevels(energyDelta: AudioEnergyDelta): AudioLevels = suspendCancellableCoroutine { continuation ->
    getStats(RTCStatsCollectorCallback { report: RTCStatsReport ->
        if (!continuation.isActive) return@RTCStatsCollectorCallback
        var localLevel = 0.0
        var remoteLevel = 0.0
        report.statsMap.values.forEach { stat ->
            if (stat.type != "inbound-rtp" && stat.type != "media-source") return@forEach
            val members = stat.members
            val level = energyDelta.level(
                statId = stat.id,
                audioLevel = (members["audioLevel"] as? Number)?.toDouble(),
                totalAudioEnergy = (members["totalAudioEnergy"] as? Number)?.toDouble(),
                totalSamplesDuration = (members["totalSamplesDuration"] as? Number)?.toDouble(),
            )
            when (stat.type) {
                "media-source" -> localLevel = maxOf(localLevel, level)
                "inbound-rtp" -> remoteLevel = maxOf(remoteLevel, level)
            }
        }
        continuation.resume(AudioLevels(localLevel, remoteLevel))
    })
}

private fun socketIoUrl(baseUrl: String): String = when {
    baseUrl.startsWith("https://") -> "wss://${baseUrl.removePrefix("https://").trimEnd('/')}/socket.io/?EIO=4&transport=websocket"
    baseUrl.startsWith("http://") -> "ws://${baseUrl.removePrefix("http://").trimEnd('/')}/socket.io/?EIO=4&transport=websocket"
    else -> throw IllegalArgumentException("Backend URL must use HTTP or HTTPS")
}
