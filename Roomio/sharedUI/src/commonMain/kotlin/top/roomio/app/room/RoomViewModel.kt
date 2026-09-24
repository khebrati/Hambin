package top.roomio.app.room

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import top.roomio.app.room.player.VideoPlayerState
import top.roomio.app.room.player.VideoPlayerStatus
import top.roomio.domain.party.Member
import top.roomio.domain.party.PartyException
import top.roomio.domain.party.RealtimeClient
import top.roomio.domain.party.RealtimeEvent
import top.roomio.domain.party.RealtimeSession
import top.roomio.domain.party.RoomRepository
import top.roomio.domain.party.RoomSnapshot
import top.roomio.domain.party.SessionKeeper
import top.roomio.domain.party.SessionRepository
import top.roomio.domain.party.StreamState
import top.roomio.domain.party.SyncStatus
import top.roomio.domain.party.VoiceClient
import top.roomio.domain.party.VoiceEvent
import top.roomio.domain.party.VoiceSession
import top.roomio.domain.party.isConnectivity

internal enum class InviteCopyTarget { CODE, LINK }

internal enum class InviteCopyState { IDLE, CODE, LINK, ERROR }

internal enum class VoiceConnectionState { IDLE, CONNECTING, CONNECTED, FAILED }

internal data class RoomUiState(
    val model: RoomScreenModel,
    val playback: RoomPlaybackState = model.playback,
    val videoUrl: String = "",
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedMs: Long = 0L,
    val isLive: Boolean = false,
    val videoAspectRatio: Float = 0f,
    val playerError: String? = null,
    val volume: Float = 0.72f,
    val micMuted: Boolean = false,
    val voiceState: VoiceConnectionState = VoiceConnectionState.IDLE,
    val fullscreenMode: Boolean = false,
    val inviteOpen: Boolean = false,
    val inviteCopyState: InviteCopyState = InviteCopyState.IDLE,
    val linkOpen: Boolean = false,
    val leaveOpen: Boolean = false,
    val abortOpen: Boolean = false,
) {
    val isOwner: Boolean
        get() = model.role == RoomRole.OWNER && model.ownerPresent

    val hasPlayer: Boolean
        get() = playback in setOf(
            RoomPlaybackState.PLAYING,
            RoomPlaybackState.PAUSED,
            RoomPlaybackState.BUFFERING,
        )
}

internal sealed interface RoomAction {
    data class VideoUrlChanged(val url: String) : RoomAction
    data class VideoUrlPasted(val url: String) : RoomAction
    data class PlaybackPositionChanged(val positionMs: Long) : RoomAction
    data class PlaybackSkipped(val direction: Float) : RoomAction
    data class VolumeChanged(val volume: Float) : RoomAction
    data class InviteCopySucceeded(val target: InviteCopyTarget) : RoomAction
    data class PlayerStateChanged(val state: VideoPlayerState) : RoomAction
    data object InviteCopyFailed : RoomAction
    data object StartPlaybackClicked : RoomAction
    data object RetryPlaybackClicked : RoomAction
    data object TogglePlaybackClicked : RoomAction
    data object ToggleVolumeClicked : RoomAction
    data object ToggleFullscreenClicked : RoomAction
    data object ToggleMicClicked : RoomAction
    data object JoinVoiceClicked : RoomAction
    data object SyncClicked : RoomAction
    data object InviteClicked : RoomAction
    data object InviteDismissed : RoomAction
    data object LinkClicked : RoomAction
    data object LinkDismissed : RoomAction
    data object LeaveClicked : RoomAction
    data object LeaveDismissed : RoomAction
    data object LeaveConfirmed : RoomAction
    data object EndStreamClicked : RoomAction
    data object EndStreamDismissed : RoomAction
    data object EndStreamConfirmed : RoomAction
}

internal sealed interface RoomEffect {
    data class SeekTo(val positionMs: Long) : RoomEffect
    data object SyncCompleted : RoomEffect
    data object SyncUnavailable : RoomEffect
    data object PartyCodeCopied : RoomEffect
    data object InviteLinkCopied : RoomEffect
    data object CopyUnavailable : RoomEffect
    data class Error(val connectivity: Boolean) : RoomEffect
}

@AssistedInject
internal class RoomViewModel(
    @Assisted private val roomId: String,
    @Assisted private val asOwner: Boolean,
    private val sessionRepository: SessionRepository,
    private val roomRepository: RoomRepository,
    private val realtimeClient: RealtimeClient,
    private val voiceClient: VoiceClient,
    private val sessionKeeper: SessionKeeper,
) : ViewModel() {

    private val mutableState = MutableStateFlow(RoomUiState(emptyRoomModel()))
    val state = mutableState.asStateFlow()

    private val mutableEffects = MutableSharedFlow<RoomEffect>(extraBufferCapacity = 4)
    val effects = mutableEffects.asSharedFlow()

    private var realtime: RealtimeSession? = null
    private var voice: VoiceSession? = null
    private var voiceWanted = false
    private var identityId: String? = null
    private var streamSessionId: String? = null
    private var members: List<Member> = emptyList()
    private var speakingMembershipIds: Set<String> = emptySet()
    private var closedByServer = false
    private val log = Logger.withTag("RoomVM")

    init {
        viewModelScope.launch { connect() }
        viewModelScope.launch { reportLoop() }
    }

    @AssistedFactory
    fun interface Factory {
        fun create(roomId: String, asOwner: Boolean): RoomViewModel
    }

    fun onAction(action: RoomAction) {
        when (action) {
            is RoomAction.VideoUrlChanged -> mutableState.update { it.copy(videoUrl = action.url) }
            is RoomAction.VideoUrlPasted -> mutableState.update { it.copy(videoUrl = action.url) }
            is RoomAction.PlaybackPositionChanged -> mutableState.update { it.copy(positionMs = it.clamp(action.positionMs)) }
            is RoomAction.PlaybackSkipped -> mutableState.update {
                it.copy(positionMs = it.clamp(it.positionMs + (action.direction * SKIP_STEP_MS).toLong()))
            }
            is RoomAction.VolumeChanged -> mutableState.update { it.copy(volume = action.volume.coerceIn(0f, 1f)) }
            is RoomAction.InviteCopySucceeded -> {
                mutableState.update {
                    it.copy(inviteCopyState = if (action.target == InviteCopyTarget.CODE) InviteCopyState.CODE else InviteCopyState.LINK)
                }
                mutableEffects.tryEmit(
                    if (action.target == InviteCopyTarget.CODE) RoomEffect.PartyCodeCopied else RoomEffect.InviteLinkCopied,
                )
            }
            is RoomAction.PlayerStateChanged -> mutableState.update { current ->
                val playerState = action.state
                if (playerState.status == VideoPlayerStatus.ERROR) {
                    log.e("player error: ${playerState.error}")
                }
                current.copy(
                    playback = playerState.status.toRoomPlaybackState(current.playback),
                    positionMs = playerState.positionMs.coerceAtLeast(0L),
                    durationMs = playerState.durationMs.coerceAtLeast(0L),
                    bufferedMs = playerState.bufferedMs.coerceAtLeast(0L),
                    isLive = playerState.isLive,
                    videoAspectRatio = playerState.videoAspectRatio,
                    playerError = playerState.error,
                )
            }
            RoomAction.InviteCopyFailed -> {
                mutableState.update { it.copy(inviteCopyState = InviteCopyState.ERROR) }
                mutableEffects.tryEmit(RoomEffect.CopyUnavailable)
            }
            RoomAction.StartPlaybackClicked -> startStream()
            RoomAction.RetryPlaybackClicked -> mutableState.update {
                val playback = when {
                    it.videoUrl.isNotBlank() -> RoomPlaybackState.PLAYING
                    asOwner -> RoomPlaybackState.OWNER_EMPTY
                    else -> RoomPlaybackState.WAITING_FOR_OWNER
                }
                it.copy(playback = playback, playerError = null)
            }
            RoomAction.TogglePlaybackClicked -> mutableState.update {
                it.copy(playback = if (it.playback == RoomPlaybackState.PLAYING) RoomPlaybackState.PAUSED else RoomPlaybackState.PLAYING)
            }
            RoomAction.ToggleVolumeClicked -> mutableState.update { it.copy(volume = if (it.volume == 0f) 0.72f else 0f) }
            RoomAction.ToggleFullscreenClicked -> mutableState.update { it.copy(fullscreenMode = !it.fullscreenMode) }
            RoomAction.ToggleMicClicked -> toggleMic()
            RoomAction.JoinVoiceClicked -> joinVoice()
            RoomAction.SyncClicked -> requestSync()
            RoomAction.InviteClicked -> mutableState.update { it.copy(inviteOpen = true, inviteCopyState = InviteCopyState.IDLE) }
            RoomAction.InviteDismissed -> mutableState.update { it.copy(inviteOpen = false) }
            RoomAction.LinkClicked -> mutableState.update { it.copy(linkOpen = true) }
            RoomAction.LinkDismissed -> mutableState.update { it.copy(linkOpen = false) }
            RoomAction.LeaveClicked -> mutableState.update { it.copy(leaveOpen = true) }
            RoomAction.LeaveDismissed -> mutableState.update { it.copy(leaveOpen = false) }
            RoomAction.LeaveConfirmed -> {
                mutableState.update { it.copy(leaveOpen = false, fullscreenMode = false) }
                leave()
            }            RoomAction.EndStreamClicked -> mutableState.update { it.copy(abortOpen = true) }
            RoomAction.EndStreamDismissed -> mutableState.update { it.copy(abortOpen = false) }
            RoomAction.EndStreamConfirmed -> {
                mutableState.update { it.copy(abortOpen = false, playback = RoomPlaybackState.ABORTED, fullscreenMode = false) }
                abortStream()
            }
        }
    }

    override fun onCleared() {
        val session = realtime
        realtime = null
        viewModelScope.launch { runCatching { session?.close() } }
        disconnectVoice()
        sessionKeeper.stop()
    }

    private suspend fun connect() {
        identityId = sessionRepository.currentSession()?.identity?.id
            ?: runCatching { sessionRepository.ensureSession("Guest", "COMET").identity.id }.getOrNull()
        log.i("connect identity=$identityId room=$roomId asOwner=$asOwner")
        try {
            applySnapshot(roomRepository.snapshot(roomId))
            // Keep the session alive in the background once the room is joined.
            sessionKeeper.start(voiceActive = false)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            val connectivity = error is PartyException && error.isConnectivity()
            // The network snapshot can fail while offline (DNS, connection, timeout).
            // Map it to an error state and an offline snackbar instead of crashing.
            log.e("snapshot failed: ${error.message ?: error::class.simpleName} connectivity=$connectivity")
            mutableState.update { it.copy(playback = RoomPlaybackState.ERROR) }
            mutableEffects.tryEmit(RoomEffect.Error(connectivity))
            return
        }
        // Keep the realtime connection alive: the transport can be dropped by
        // idle timeouts or network changes, and a dropped socket must never
        // crash the app or leave the room unusable.
        while (!closedByServer && currentCoroutineContext().isActive) {
            try {
                val session = realtimeClient.connect(roomId)
                realtime = session
                log.i("realtime connected room=$roomId, awaiting events")
                session.events.collect { event -> handleEvent(event) }
                log.w("realtime event flow ended room=$roomId")
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                // Retry below; the server resends a snapshot on reconnect.
                log.e("realtime connect failed room=$roomId: ${error.message ?: error::class.simpleName}")
            } finally {
                if (realtime != null) log.w("realtime detached room=$roomId")
                realtime = null
            }
            if (!closedByServer) {
                log.i("reconnecting room=$roomId in ${RECONNECT_DELAY_MS}ms")
                delay(RECONNECT_DELAY_MS)
            }
        }
        log.w("connect loop ended room=$roomId closedByServer=$closedByServer")
    }

    private fun handleEvent(event: RealtimeEvent) {
        when (event) {
            is RealtimeEvent.Snapshot -> applySnapshot(event.value)
            is RealtimeEvent.MemberJoined -> upsertMember(event.member)
            is RealtimeEvent.MemberReturned -> upsertMember(event.member)
            is RealtimeEvent.MemberLeft -> removeMember(event.membershipId)
            is RealtimeEvent.MemberAbsent -> upsertMember(event.member)
            is RealtimeEvent.OwnerPresence -> mutableState.update {
                it.copy(model = it.model.copy(ownerPresent = asOwner || event.present))
            }
            is RealtimeEvent.StreamStarted -> {
                streamSessionId = event.sessionId
                log.i("stream.started session=${event.sessionId} url=${event.url}")
                mutableState.update {
                    it.copy(
                        videoUrl = event.url,
                        playback = RoomPlaybackState.PLAYING,
                    )
                }
            }
            is RealtimeEvent.StreamAborted -> mutableState.update {
                it.copy(playback = RoomPlaybackState.ABORTED)
            }
            RealtimeEvent.RoomClosed -> {
                closedByServer = true
                mutableState.update { it.copy(playback = RoomPlaybackState.ABORTED) }
            }
            is RealtimeEvent.SyncResult -> when (event.status) {
                SyncStatus.APPLIED -> {
                    log.i("sync.result APPLIED target=${event.targetPositionMs}ms → SeekTo + SyncCompleted")
                    mutableState.update { it.copy(positionMs = event.targetPositionMs) }
                    mutableEffects.tryEmit(RoomEffect.SeekTo(event.targetPositionMs))
                    mutableEffects.tryEmit(RoomEffect.SyncCompleted)
                }
                SyncStatus.ALREADY_LEADING -> {
                    log.i("sync.result ALREADY_LEADING → SyncCompleted")
                    mutableEffects.tryEmit(RoomEffect.SyncCompleted)
                }
                SyncStatus.UNAVAILABLE -> {
                    log.i("sync.result UNAVAILABLE → SyncUnavailable")
                    mutableEffects.tryEmit(RoomEffect.SyncUnavailable)
                }
            }
            is RealtimeEvent.Failure -> log.w("realtime failure code=${event.code} message=${event.message}")
            RealtimeEvent.Ignored -> Unit
        }
    }

    private fun applySnapshot(snapshot: RoomSnapshot) {
        streamSessionId = snapshot.stream.sessionId.ifEmpty { null }
        members = snapshot.members
        val owner = members.firstOrNull { it.isOwner }
        val playback = when (snapshot.stream.state) {
            StreamState.ACTIVE -> RoomPlaybackState.PLAYING
            StreamState.ABORTED -> RoomPlaybackState.ABORTED
            StreamState.EMPTY -> if (asOwner) RoomPlaybackState.OWNER_EMPTY else RoomPlaybackState.WAITING_FOR_OWNER
        }
        log.i("snapshot room=${snapshot.room.code} stream=${snapshot.stream.state} streamSessionId=$streamSessionId playback=$playback url=${snapshot.stream.url}")
        mutableState.update { current ->
            current.copy(
                model = RoomScreenModel(
                    title = snapshot.room.title,
                    partyCode = snapshot.room.code,
                    role = if (asOwner) RoomRole.OWNER else RoomRole.GUEST,
                    ownerName = owner?.displayName.orEmpty(),
                    ownerPresent = asOwner || (owner?.present ?: false),
                    playback = playback,
                    participants = members.map { it.toParticipant() },
                ),
                playback = playback,
                videoUrl = snapshot.stream.url,
            )
        }
    }

    private fun upsertMember(member: Member) {
        members = members.filterNot { it.membershipId == member.membershipId } + member
        refreshParticipants()
    }

    private fun removeMember(membershipId: String) {
        members = members.filterNot { it.membershipId == membershipId }
        refreshParticipants()
    }

    private fun refreshParticipants() {
        val owner = members.firstOrNull { it.isOwner }
        mutableState.update { current ->
            current.copy(
                model = current.model.copy(
                    participants = members.map { it.toParticipant() },
                    ownerPresent = asOwner || (owner?.present ?: current.model.ownerPresent),
                ),
            )
        }
    }

    private fun Member.toParticipant() = RoomParticipant(
        name = displayName,
        avatar = avatar.toRoomAvatar(),
        isSelf = this@RoomViewModel.identityId == identityId,
        isHost = isOwner,
        isPresent = present,
        isSpeaking = membershipId in this@RoomViewModel.speakingMembershipIds,
    )

    private fun startStream() {
        val url = mutableState.value.videoUrl
        if (url.isBlank()) {
            log.w("startStream skipped: no video URL")
            return
        }
        log.i("startStream requested url=$url")
        viewModelScope.launch {
            try {
                val stream = roomRepository.startStream(roomId, url)
                streamSessionId = stream.sessionId
                log.i("startStream applied session=${stream.sessionId}")
                mutableState.update { it.copy(playback = RoomPlaybackState.PLAYING) }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                val connectivity = error is PartyException && error.isConnectivity()
                log.e("startStream failed: ${error.message ?: error::class.simpleName} connectivity=$connectivity")
                mutableState.update { it.copy(playback = RoomPlaybackState.ERROR) }
                mutableEffects.tryEmit(RoomEffect.Error(connectivity))
            }
        }
    }

    private fun abortStream() {
        mutableState.update { it.copy(abortOpen = false) }
        viewModelScope.launch {
            runCatching { roomRepository.abortStream(roomId) }
        }
    }

    private fun leave() {
        mutableState.update { it.copy(leaveOpen = false) }
        disconnectVoice()
        sessionKeeper.stop()
        viewModelScope.launch { runCatching { roomRepository.leave(roomId) } }
    }

    private fun joinVoice() {
        if (voiceWanted) return
        voiceWanted = true
        // Voice is opt-in and starts muted.
        mutableState.update { it.copy(voiceState = VoiceConnectionState.CONNECTING, micMuted = true) }
        viewModelScope.launch { runVoiceLoop() }
    }

    /**
     * Keeps voice alive for as long as the user wants it. LiveKit reconnects on
     * its own for transient drops, but an expired token or a failed reconnect
     * surfaces here, where a fresh room-scoped token is fetched and the session
     * is re-established with the current mic preference.
     */
    private suspend fun runVoiceLoop() {
        while (voiceWanted && currentCoroutineContext().isActive) {
            try {
                val token = roomRepository.voiceToken(roomId)
                val session = voiceClient.connect(token)
                voice = session
                session.setMicrophoneEnabled(!mutableState.value.micMuted)
                mutableState.update { it.copy(voiceState = VoiceConnectionState.CONNECTED) }
                // A microphone-capable foreground service keeps voice alive off-screen.
                sessionKeeper.start(voiceActive = true)
                log.i("voice connected room=$roomId")
                session.events.collect { event -> handleVoiceEvent(event) }
                log.w("voice event flow ended room=$roomId")
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                log.e("voice session failed room=$roomId: ${error.message ?: error::class.simpleName}")
            } finally {
                voice = null
            }
            if (voiceWanted) {
                sessionKeeper.start(voiceActive = false)
                mutableState.update { it.copy(voiceState = VoiceConnectionState.CONNECTING) }
                log.w("voice reconnecting room=$roomId in ${VOICE_RECONNECT_DELAY_MS}ms")
                delay(VOICE_RECONNECT_DELAY_MS)
            }
        }
        mutableState.update { it.copy(voiceState = VoiceConnectionState.IDLE, micMuted = true) }
        log.i("voice loop ended room=$roomId")
    }

    private fun toggleMic() {
        val session = voice
        if (session == null) {
            mutableState.update { it.copy(micMuted = !it.micMuted) }
            return
        }
        val next = !mutableState.value.micMuted
        mutableState.update { it.copy(micMuted = next) }
        session.setMicrophoneEnabled(!next)
    }

    private fun handleVoiceEvent(event: VoiceEvent) {
        when (event) {
            VoiceEvent.Connected -> mutableState.update { it.copy(voiceState = VoiceConnectionState.CONNECTED) }
            // The loop owns reconnection, so a drop only needs logging here.
            VoiceEvent.Disconnected -> log.w("voice disconnected room=$roomId")
            is VoiceEvent.Failed -> log.w("voice failed room=$roomId: ${event.reason}")
            is VoiceEvent.SpeakersChanged -> {
                speakingMembershipIds = event.speakingMembershipIds
                refreshParticipants()
            }
        }
    }

    private fun disconnectVoice() {
        voiceWanted = false
        val session = voice
        voice = null
        speakingMembershipIds = emptySet()
        mutableState.update { it.copy(voiceState = VoiceConnectionState.IDLE, micMuted = true) }
        if (session != null) {
            viewModelScope.launch { runCatching { session.disconnect() } }
        }
    }

    private fun requestSync() {
        val session = realtime
        val streamId = streamSessionId
        val position = mutableState.value.positionMs
        when {
            session == null -> log.w("sync skipped: realtime not connected room=$roomId")
            streamId == null -> log.w("sync skipped: no active streamSessionId room=$roomId")
            else -> {
                log.i("sync requested stream=$streamId position=${position}ms playback=${mutableState.value.playback}")
                viewModelScope.launch {
                    runCatching { session.requestSync(streamId, position) }
                        .onFailure { error -> log.e("sync request failed: ${error.message ?: error::class.simpleName}") }
                }
            }
        }
    }

    private suspend fun reportLoop() {
        var skipLogged = false
        while (true) {
            delay(REPORT_INTERVAL_MS)
            val session = realtime
            val streamId = streamSessionId
            val current = mutableState.value
            if (session == null || streamId == null || !current.hasPlayer) {
                if (!skipLogged) {
                    log.d("reportLoop skip: realtime=${session != null} streamSessionId=$streamId playback=${current.playback}")
                    skipLogged = true
                }
                continue
            }
            skipLogged = false
            val reportedPositionMs = current.positionMs + REPORT_ROUND_TRIP_COMPENSATION_MS
            log.d("report> stream=$streamId pos=${current.positionMs}ms reported=${reportedPositionMs}ms playing=${current.playback == RoomPlaybackState.PLAYING}")
            runCatching {
                session.reportPlayback(streamId, reportedPositionMs, current.playback == RoomPlaybackState.PLAYING)
            }.onFailure { error -> log.e("report failed: ${error.message ?: error::class.simpleName}") }
        }
    }

    private companion object {
        const val SKIP_STEP_MS = 15_000f
        const val REPORT_INTERVAL_MS = 5_000L
        const val REPORT_ROUND_TRIP_COMPENSATION_MS = 2_000L
        const val RECONNECT_DELAY_MS = 3_000L
        const val VOICE_RECONNECT_DELAY_MS = 2_000L
    }
}

private fun RoomUiState.clamp(positionMs: Long): Long {
    val max = if (durationMs > 0L) durationMs else Long.MAX_VALUE
    return positionMs.coerceIn(0L, max)
}

private fun String.toRoomAvatar(): RoomAvatar =
    RoomAvatar.entries.firstOrNull { it.name == uppercase() } ?: RoomAvatar.COMET

private fun VideoPlayerStatus.toRoomPlaybackState(fallback: RoomPlaybackState): RoomPlaybackState = when (this) {
    VideoPlayerStatus.IDLE, VideoPlayerStatus.READY -> fallback
    VideoPlayerStatus.BUFFERING -> RoomPlaybackState.BUFFERING
    VideoPlayerStatus.PLAYING -> RoomPlaybackState.PLAYING
    VideoPlayerStatus.PAUSED, VideoPlayerStatus.ENDED -> RoomPlaybackState.PAUSED
    VideoPlayerStatus.ERROR -> RoomPlaybackState.ERROR
}

private fun emptyRoomModel() = RoomScreenModel(
    title = "",
    partyCode = "",
    role = RoomRole.GUEST,
    ownerName = "",
    ownerPresent = false,
    playback = RoomPlaybackState.WAITING_FOR_OWNER,
    participants = emptyList(),
)
