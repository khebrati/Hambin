package top.roomio.app.room

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random
import top.roomio.app.room.player.VideoPlayerState
import top.roomio.app.room.player.VideoPlayerStatus
import top.roomio.app.room.player.AudioTrack
import top.roomio.app.room.player.SubtitleTrack
import top.roomio.domain.party.GroupSyncAttempt
import top.roomio.domain.party.GroupSyncResponse
import top.roomio.domain.party.Member
import top.roomio.domain.party.PartyException
import top.roomio.domain.party.RealtimeClient
import top.roomio.domain.party.RealtimeEvent
import top.roomio.domain.party.RealtimeSession
import top.roomio.domain.party.RoomRepository
import top.roomio.domain.party.RoomSession
import top.roomio.domain.party.RoomSnapshot
import top.roomio.domain.party.SessionRepository
import top.roomio.domain.party.StreamState
import top.roomio.domain.party.SyncStatus
import top.roomio.domain.party.VoiceCoordinator
import top.roomio.domain.party.VoiceSessionState
import top.roomio.domain.party.VoicePeerStatus
import top.roomio.domain.party.isConnectivity

internal enum class InviteCopyTarget { CODE, LINK }

internal enum class InviteCopyState { IDLE, CODE, LINK, ERROR }

internal enum class VoiceConnectionState { IDLE, CONNECTING, CONNECTED, DEGRADED, RECONNECTING, FAILED }

internal data class RoomUiState(
    val model: RoomScreenModel,
    val groupSync: GroupSyncAttempt? = null,
    val syncDialogOpen: Boolean = false,
    val syncBusy: Boolean = false,
    val syncLocked: Boolean = false,
    val syncCountdown: String? = null,
    val syncMembers: List<Member> = emptyList(),
    val syncSelfId: String = "",

    val chat: RoomChatState = RoomChatState(),
    val playback: RoomPlaybackState = model.playback,
    val videoUrl: String = "",
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedMs: Long = 0L,
    val isLive: Boolean = false,
    val videoAspectRatio: Float = 0f,
    val playerError: String? = null,
    val subtitleTracks: List<SubtitleTrack> = emptyList(),
    val audioTracks: List<AudioTrack> = emptyList(),
    val audioTrackSelectionId: String? = null,
    val volume: Float = 0.72f,
    val micMuted: Boolean = false,
    val voiceState: VoiceConnectionState = VoiceConnectionState.IDLE,
    val voicePeers: Map<String, VoicePeerStatus> = emptyMap(),
    val voiceFailureCode: String? = null,
    val fullscreenMode: Boolean = false,
    val inviteOpen: Boolean = false,
    val inviteCopyState: InviteCopyState = InviteCopyState.IDLE,
    val linkOpen: Boolean = false,
    val leaveOpen: Boolean = false,
    val abortOpen: Boolean = false,
    val bugReportOpen: Boolean = false,
    val bugDescription: String = "",
    val bugSending: Boolean = false,
    val bugError: String? = null,
    val bugSent: Boolean = false,
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
    data class ChatDraftChanged(val text: String) : RoomAction
    data object ChatSend : RoomAction
    data class ChatRetry(val messageId: String) : RoomAction
    data class ChatDiscard(val messageId: String) : RoomAction
    data object ChatToggle : RoomAction
    data class ChatAtLatest(val value: Boolean) : RoomAction
    data object ChatLoadEarlier : RoomAction
    data object ChatRefresh : RoomAction

    data object BugReportClicked : RoomAction
    data object BugReportDismissed : RoomAction
    data class BugDescriptionChanged(val description: String) : RoomAction
    data object BugReportSend : RoomAction
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
    data object RetryVoiceConnections : RoomAction
    data object SyncAsk : RoomAction
    data class SyncRespond(val response: GroupSyncResponse) : RoomAction
    data object SyncCommit : RoomAction
    data object SyncCancel : RoomAction
    data object SyncDismiss : RoomAction
    data object SyncPlayerUnavailable : RoomAction
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
    data class PlayerPlaying(val playing: Boolean) : RoomEffect
    data class GroupSyncNotice(val notice: top.roomio.app.room.GroupSyncNotice) : RoomEffect
    data class SeekTo(val positionMs: Long) : RoomEffect
    data object SyncCompleted : RoomEffect
    data object SyncUnavailable : RoomEffect
    data object PartyCodeCopied : RoomEffect
    data object InviteLinkCopied : RoomEffect
    data object CopyUnavailable : RoomEffect
    data class Error(val connectivity: Boolean) : RoomEffect
    data object Left : RoomEffect
}

@AssistedInject
internal class RoomViewModel(
    @Assisted private val roomId: String,
    @Assisted private val asOwner: Boolean,
    private val sessionRepository: SessionRepository,
    private val roomRepository: RoomRepository,
    private val realtimeClient: RealtimeClient,
    private val voiceCoordinator: VoiceCoordinator,
) : ViewModel() {

    private val mutableState = MutableStateFlow(RoomUiState(emptyRoomModel()))
    val state = mutableState.asStateFlow()

    private val mutableEffects = MutableSharedFlow<RoomEffect>(extraBufferCapacity = 4)
    val effects = mutableEffects.asSharedFlow()

    private var realtime: RealtimeSession? = null
    private var identityId: String? = null
    private var streamSessionId: String? = null
    private var streamRevision = -1L
    private var pendingSyncRequestId: String? = null
    private var syncTimeoutJob: Job? = null
    private var members: List<Member> = emptyList()
    private var speakingMembershipIds: Set<String> = emptySet()
    private var closedByServer = false
    private val log = Logger.withTag("RoomVM")
    private var chatVersion = 0L
    private var connectionJob: Job? = null

    private val groupSync = NativeGroupSync(viewModelScope, { mutableState.value }, { change -> mutableState.update(change) }, { realtime }, { streamSessionId }, { members.find { it.identityId == identityId }?.membershipId }, { mutableEffects.tryEmit(it) })

    init {
        connectionJob = viewModelScope.launch { connect() }
        viewModelScope.launch { reportLoop() }
        viewModelScope.launch {
            voiceCoordinator.state.collect { voice ->
                if (voice.roomId == roomId || voice.roomId == null) {
                    speakingMembershipIds = voice.speakers
                    mutableState.update { it.copy(
                        voiceState = when (voice.state) {
                            VoiceSessionState.OFF -> VoiceConnectionState.IDLE
                            VoiceSessionState.JOINING -> VoiceConnectionState.CONNECTING
                            VoiceSessionState.CONNECTED -> VoiceConnectionState.CONNECTED
                            VoiceSessionState.DEGRADED -> VoiceConnectionState.DEGRADED
                            VoiceSessionState.RECONNECTING -> VoiceConnectionState.RECONNECTING
                            VoiceSessionState.FAILED -> VoiceConnectionState.FAILED
                        },
                        micMuted = !voice.microphoneEnabled,
                        voicePeers = voice.peers,
                        voiceFailureCode = voice.failureCode,
                    ) }
                    refreshParticipants()
                }
            }
        }
        viewModelScope.launch { voiceCoordinator.leaveRequests.collect { id ->
            if (id == roomId) { clearSpeakingMembers(); mutableEffects.tryEmit(RoomEffect.Left) }
        } }
    }

    @AssistedFactory
    fun interface Factory {
        fun create(roomId: String, asOwner: Boolean): RoomViewModel
    }

    fun onAction(action: RoomAction) {
        if (state.value.syncLocked && (action == RoomAction.TogglePlaybackClicked || action is RoomAction.PlaybackPositionChanged || action is RoomAction.PlaybackSkipped)) return
        when (action) {
            RoomAction.SyncAsk -> groupSync.ask()
            is RoomAction.SyncRespond -> groupSync.respond(action.response)
            RoomAction.SyncCommit -> groupSync.commit()
            RoomAction.SyncCancel -> groupSync.cancel()
            RoomAction.SyncDismiss -> groupSync.dismiss()
            RoomAction.SyncPlayerUnavailable -> groupSync.playerUnavailable()
            is RoomAction.ChatDraftChanged -> if (action.text.length <= 500) mutableState.update { it.copy(chat = it.chat.copy(draft = action.text)) }
            RoomAction.ChatSend -> sendChat()
            is RoomAction.ChatRetry -> sendChat(action.messageId)
            is RoomAction.ChatDiscard -> mutableState.update { it.copy(chat = it.chat.copy(pending = it.chat.pending.filterNot { pending -> pending.id == action.messageId })) }
            RoomAction.ChatToggle -> mutableState.update { it.copy(chat = it.chat.copy(open = !it.chat.open, unread = if (!it.chat.open) 0 else it.chat.unread, atLatest = if (!it.chat.open) true else it.chat.atLatest)) }
            is RoomAction.ChatAtLatest -> mutableState.update { it.copy(chat = it.chat.copy(atLatest = action.value, unread = if (action.value && it.chat.open) 0 else it.chat.unread)) }
            RoomAction.ChatLoadEarlier -> loadChatHistory()
            RoomAction.ChatRefresh -> {
                if (realtime == null && connectionJob?.isActive != true) {
                    mutableState.update { it.copy(chat = it.chat.copy(loading = true, error = null)) }
                    connectionJob = viewModelScope.launch { connect() }
                } else loadChatHistory(refresh = true)
            }

            RoomAction.BugReportClicked -> mutableState.update { it.copy(bugReportOpen = true, bugSent = false) }
            RoomAction.BugReportDismissed -> if (!state.value.bugSending) mutableState.update { it.copy(bugReportOpen = false) }
            is RoomAction.BugDescriptionChanged -> if (!state.value.bugSending) mutableState.update { it.copy(bugDescription = action.description.take(8000), bugError = null, bugSent = false) }
            RoomAction.BugReportSend -> sendBugReport()
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
            is RoomAction.PlayerStateChanged -> { groupSync.player(action.state); mutableState.update { current ->
                val playerState = action.state
                if (playerState.status == VideoPlayerStatus.ERROR) {
                    log.e("player error: ${playerState.error}")
                }
                current.copy(
                    playback = if(current.syncLocked) RoomPlaybackState.PAUSED else playerState.status.toRoomPlaybackState(current.playback),
                    positionMs = playerState.positionMs.coerceAtLeast(0L),
                    durationMs = playerState.durationMs.coerceAtLeast(0L),
                    bufferedMs = playerState.bufferedMs.coerceAtLeast(0L),
                    isLive = playerState.isLive,
                    videoAspectRatio = playerState.videoAspectRatio,
                    playerError = playerState.error,
                    subtitleTracks = playerState.subtitles,
                    audioTracks = playerState.audioTracks,
                    audioTrackSelectionId = playerState.audioTrackSelectionId,
                )
            }
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
            RoomAction.JoinVoiceClicked -> voiceCoordinator.join(roomId)
            RoomAction.RetryVoiceConnections -> voiceCoordinator.retryFailedConnections(roomId)
            RoomAction.SyncClicked -> if(state.value.groupSync?.active == true || state.value.isOwner) groupSync.open() else requestSync()
            RoomAction.InviteClicked -> mutableState.update { it.copy(inviteOpen = true, inviteCopyState = InviteCopyState.IDLE) }
            RoomAction.InviteDismissed -> mutableState.update { it.copy(inviteOpen = false) }
            RoomAction.LinkClicked -> mutableState.update { it.copy(linkOpen = true) }
            RoomAction.LinkDismissed -> mutableState.update { it.copy(linkOpen = false) }
            RoomAction.LeaveClicked -> mutableState.update { it.copy(leaveOpen = true) }
            RoomAction.LeaveDismissed -> mutableState.update { it.copy(leaveOpen = false) }
            RoomAction.LeaveConfirmed -> leave()
            RoomAction.EndStreamClicked -> mutableState.update { it.copy(abortOpen = true) }
            RoomAction.EndStreamDismissed -> mutableState.update { it.copy(abortOpen = false) }
            RoomAction.EndStreamConfirmed -> {
                mutableState.update { it.copy(abortOpen = false, playback = RoomPlaybackState.ABORTED, fullscreenMode = false) }
                abortStream()
            }
        }
    }

    @OptIn(kotlin.uuid.ExperimentalUuidApi::class)
    private fun sendChat(retryId: String? = null) {
        val current = state.value.chat
        val previous = retryId?.let { id -> current.pending.firstOrNull { it.id == id && it.state == ChatSendState.FAILED } }
        if (retryId != null && previous == null) return
        val text = previous?.text ?: top.roomio.domain.party.normalizeChatText(current.draft)
        if (!current.connected || current.loading || !top.roomio.domain.party.validChatText(text) || (previous == null && current.pending.size >= 20)) return
        val pending = previous?.copy(state = ChatSendState.SENDING, error = null)
            ?: PendingChatMessage(kotlin.uuid.Uuid.random().toString(), text)
        mutableState.update { it.copy(chat = it.chat.copy(
            draft = if (previous == null) "" else it.chat.draft,
            pending = it.chat.pending.filterNot { message -> message.id == pending.id } + pending,
            atLatest = true,
        )) }
        viewModelScope.launch {
            try {
                val accepted = roomRepository.sendChatMessage(roomId, pending.id, text)
                if (accepted.roomId == roomId) mutableState.update { it.copy(chat = it.chat.receive(accepted)) }
                else throw PartyException("UNAVAILABLE", "Unexpected chat room")
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (error: Throwable) {
                val code = (error as? PartyException)?.code ?: "NETWORK"
                mutableState.update { it.copy(chat = it.chat.copy(pending = it.chat.pending.map { message ->
                    if (message.id == pending.id) message.copy(state = ChatSendState.FAILED, error = code) else message
                })) }
            }
        }
    }

    private fun loadChatHistory(refresh: Boolean = false) {
        val current = state.value.chat
        if (current.loadingMore || (!refresh && (!current.hasMore || current.messages.size >= 500))) return
        val before = if (refresh) null else current.messages.firstOrNull()?.sequence
        val version = chatVersion
        mutableState.update { it.copy(chat = it.chat.copy(loadingMore = true, error = null, atLatest = if (refresh) true else false)) }
        viewModelScope.launch {
            try {
                val page = roomRepository.chatHistory(roomId, before)
                if (version != chatVersion) return@launch
                mutableState.update {
                    if (!refresh && it.chat.messages.firstOrNull()?.sequence != before) it
                    else {
                        // Refresh pages may race a newer room event: retain only known messages newer than the page.
                        val newer = if (refresh) it.chat.messages.filter { message -> message.sequence > (page.messages.lastOrNull()?.sequence ?: 0L) } else it.chat.messages
                        val all = (page.messages + newer).distinctBy { message -> message.id }.sortedBy { message -> message.sequence }
                        it.copy(chat = it.chat.copy(messages = all.takeLast(500), hasMore = page.hasMore || all.size > 500,
                            pending = it.chat.pending.filterNot { pending -> all.any { message -> message.id == pending.id } }, loading = false))
                    }
                }
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (error: Throwable) {
                if (version == chatVersion) mutableState.update { it.copy(chat = it.chat.copy(error = (error as? PartyException)?.code ?: "NETWORK", loading = false)) }
            } finally {
                if (version == chatVersion) mutableState.update { it.copy(chat = it.chat.copy(loadingMore = false)) }
            }
        }
    }

    private fun sendBugReport() {
        val current = state.value
        if (current.bugSending || current.bugDescription.isBlank() || current.bugSent) return
        mutableState.update { it.copy(bugSending = true, bugError = null) }
        viewModelScope.launch {
            try {
                roomRepository.reportBug(roomId, current.bugDescription)
                mutableState.update { it.copy(bugSending = false, bugDescription = "", bugSent = true) }
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (error: Throwable) {
                mutableState.update { it.copy(bugSending = false, bugError = (error as? PartyException)?.code ?: "UNAVAILABLE") }
            }
        }
    }

    override fun onCleared() {
        val session = realtime
        realtime = null
        viewModelScope.launch { runCatching { session?.close() } }
        voiceCoordinator.leave(roomId)
    }

    private suspend fun connect() {
        identityId = sessionRepository.currentSession()?.identity?.id
            ?: runCatching { sessionRepository.ensureSession("Guest", "COMET").identity.id }.getOrNull()
        log.i("room.connect asOwner=$asOwner")
        try {
            applySnapshot(roomRepository.snapshot(roomId))
            // Keep the session alive in the background once the room is joined.
            voiceCoordinator.enter(RoomSession(roomId, asOwner, voiceActive = false, micMuted = true))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            val connectivity = error is PartyException && error.isConnectivity()
            // The network snapshot can fail while offline (DNS, connection, timeout).
            // Map it to an error state and an offline snackbar instead of crashing.
            log.e("snapshot failed: ${(error as? PartyException)?.code ?: "UNKNOWN"} connectivity=$connectivity")
            mutableState.update { it.copy(playback = RoomPlaybackState.ERROR, chat = it.chat.copy(loading = false, connected = false, error = (error as? PartyException)?.code ?: "NETWORK")) }
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
                viewModelScope.launch { groupSync.calibrate(session) }
                log.i("realtime connected, awaiting events")
                session.events.collect { event -> handleEvent(event) }
                log.w("realtime event flow ended")
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                // Retry below; the server resends a snapshot on reconnect.
                log.e("realtime connect failed: ${(error as? PartyException)?.code ?: "UNKNOWN"}")
            } finally {
                if (realtime != null) log.w("realtime detached")
                realtime = null
                mutableState.update { it.copy(chat = it.chat.copy(connected = false)) }
            }
            if (!closedByServer) {
                log.i("reconnecting in ${RECONNECT_DELAY_MS}ms")
                delay(RECONNECT_DELAY_MS)
            }
        }
        log.w("connect loop ended closedByServer=$closedByServer")
    }

    private fun handleEvent(event: RealtimeEvent) {
        when (event) {
            is RealtimeEvent.GroupSyncChanged -> groupSync.apply(event.value, event.serverTimeMs)
            is RealtimeEvent.Snapshot -> applySnapshot(event.value)
            is RealtimeEvent.MemberJoined -> upsertMember(event.member)
            is RealtimeEvent.MemberReturned -> upsertMember(event.member)
            is RealtimeEvent.MemberLeft -> handleMemberLeft(event.membershipId)
            is RealtimeEvent.MemberAbsent -> upsertMember(event.member)
            is RealtimeEvent.OwnerPresence -> mutableState.update {
                it.copy(model = it.model.copy(ownerPresent = asOwner || event.present))
            }
            is RealtimeEvent.StreamStarted -> {
                if (event.revision <= streamRevision) {
                    log.w("stream.started ignored: stale revision=${event.revision} current=$streamRevision")
                    return
                }
                groupSync.reset()
                streamRevision = event.revision
                streamSessionId = event.sessionId
                log.i("stream.started session=${event.sessionId} revision=${event.revision}")
                mutableState.update {
                    it.copy(
                        videoUrl = event.url,
                        playback = RoomPlaybackState.PLAYING,
                    )
                }
            }
            is RealtimeEvent.StreamAborted -> {
                if (event.revision <= streamRevision) {
                    log.w("stream.aborted ignored: stale revision=${event.revision} current=$streamRevision")
                    return
                }
                groupSync.reset()
                streamRevision = event.revision
                // The aborted session must never be a sync or report target.
                streamSessionId = null
                log.i("stream.aborted session=${event.sessionId} revision=${event.revision}")
                mutableState.update { it.copy(playback = RoomPlaybackState.ABORTED) }
            }
            is RealtimeEvent.ChatMessageReceived -> if (event.message.roomId == roomId) {
                mutableState.update { it.copy(chat = it.chat.receive(event.message)) }
            }
            RealtimeEvent.RoomClosed -> {
                closedByServer = true
                mutableState.update { it.copy(playback = RoomPlaybackState.ABORTED, chat = it.chat.copy(connected = false)) }
            }
            is RealtimeEvent.SyncResult -> {
                val pending = pendingSyncRequestId
                // An echoed requestId that does not match the in-flight request
                // is a late reply for an earlier sync; applying it would yank the
                // player unexpectedly. Replies must match the current request.
                if (event.requestId != pending) {
                    log.w("sync.result ignored: stale requestId=${event.requestId} pending=$pending")
                    return
                }
                pendingSyncRequestId = null
                syncTimeoutJob?.cancel()
                when (event.status) {
                    SyncStatus.APPLIED -> {
                        log.i("sync.result APPLIED target=${event.targetPositionMs}ms → SeekTo + SyncCompleted")
                        mutableState.update { it.copy(positionMs = event.targetPositionMs, playback = if(event.playing) RoomPlaybackState.PLAYING else RoomPlaybackState.PAUSED) }
                        mutableEffects.tryEmit(RoomEffect.SeekTo(event.targetPositionMs))
                        mutableEffects.tryEmit(RoomEffect.PlayerPlaying(event.playing))
                        mutableEffects.tryEmit(RoomEffect.SyncCompleted)
                    }
                    SyncStatus.UNAVAILABLE -> {
                        log.i("sync.result UNAVAILABLE → SyncUnavailable")
                        mutableEffects.tryEmit(RoomEffect.SyncUnavailable)
                    }
                }
            }
            is RealtimeEvent.Failure -> {
                groupSync.disconnected()
                log.w("realtime failure code=${event.code}")
                if (event.code in setOf("NETWORK", "UNAVAILABLE", "AUTH_UNAVAILABLE", "SESSION_EXPIRED", "INVALID_TOKEN", "UNAUTHENTICATED", "SESSION_REVOKED", "NOT_A_MEMBER", "ROOM_ENDED")) {
                    mutableState.update { it.copy(chat = it.chat.copy(connected = false)) }
                }
            }
            RealtimeEvent.Ignored -> Unit
        }
    }

    private fun applySnapshot(snapshot: RoomSnapshot) {
        chatVersion++
        val incomingSessionId = snapshot.stream.sessionId.takeIf { snapshot.stream.state == StreamState.ACTIVE && it.isNotEmpty() }
        val sameActiveStream = incomingSessionId != null && incomingSessionId == streamSessionId
        if (!sameActiveStream) groupSync.reset()
        streamRevision = snapshot.stream.revision
        // Only an active stream may be reported or synced to. An aborted
        // session stays in the snapshot but must not be a target.
        streamSessionId = if (snapshot.stream.state == StreamState.ACTIVE) {
            snapshot.stream.sessionId.ifEmpty { null }
        } else {
            null
        }
        members = snapshot.members
        mutableState.update { it.copy(syncMembers=members,syncSelfId=members.find { member -> member.identityId==identityId }?.membershipId.orEmpty()) }
        val owner = members.firstOrNull { it.isOwner }
        val playback = when (snapshot.stream.state) {
            StreamState.ACTIVE -> RoomPlaybackState.PLAYING
            StreamState.ABORTED -> RoomPlaybackState.ABORTED
            StreamState.EMPTY -> if (asOwner) RoomPlaybackState.OWNER_EMPTY else RoomPlaybackState.WAITING_FOR_OWNER
        }
        log.i("snapshot received stream=${snapshot.stream.state} streamSessionId=$streamSessionId playback=$playback sameActiveStream=$sameActiveStream")
        mutableState.update { current ->
            val effectivePlayback = if (sameActiveStream) current.playback else playback
            current.copy(
                model = RoomScreenModel(
                    title = snapshot.room.title,
                    partyCode = snapshot.room.code,
                    role = if (asOwner) RoomRole.OWNER else RoomRole.GUEST,
                    ownerName = owner?.displayName.orEmpty(),
                    ownerPresent = asOwner || (owner?.present ?: false),
                    playback = effectivePlayback,
                    participants = members.map { it.toParticipant() },
                ),
                playback = effectivePlayback,
                chat = current.chat.copy(
                    identityId = identityId.orEmpty(), messages = snapshot.chat.messages.takeLast(500),
                    hasMore = snapshot.chat.hasMore, loading = false, loadingMore = false, error = null, connected = true,
                    pending = current.chat.pending.filterNot { pending -> snapshot.chat.messages.any { it.id == pending.id } },
                    unread = current.chat.unread + if (!current.chat.open || !current.chat.atLatest) snapshot.chat.messages.count { message ->
                        message.sequence > (current.chat.messages.lastOrNull()?.sequence ?: if (current.chat.loading) Long.MAX_VALUE else 0L) && message.identityId != identityId
                    } else 0,
                ),
                // A reconnect snapshot describes the same shared stream. Keep
                // viewer-local playback, position and track selections intact.
                videoUrl = if (sameActiveStream) current.videoUrl else snapshot.stream.url,
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

    /**
     * A member closed the app or explicitly left. When it is the owner, the room
     * stays open but loses its host, which the screen surfaces as owner-away.
     */
    private fun handleMemberLeft(membershipId: String) {
        val leaving = members.firstOrNull { it.membershipId == membershipId }
        removeMember(membershipId)
        if (leaving?.isOwner == true) {
            mutableState.update { it.copy(model = it.model.copy(ownerPresent = asOwner)) }
        }
    }

    private fun refreshParticipants() {
        mutableState.update { it.copy(syncMembers=members,syncSelfId=members.find { member -> member.identityId==identityId }?.membershipId.orEmpty()) }
        val owner = members.firstOrNull { it.isOwner }
        mutableState.update { current ->
            current.copy(
                model = current.model.copy(
                    participants = members.map { it.toParticipant() },
                    // No owner in the room means the owner left; only the
                    // owner's own device keeps reporting itself as present.
                    ownerPresent = asOwner || (owner?.present ?: false),
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
        voiceStatus = mutableState.value.voicePeers[membershipId],
    )

    private fun startStream() {
        val url = mutableState.value.videoUrl
        if (url.isBlank()) {
            log.w("startStream skipped: no video URL")
            return
        }
        log.i("startStream requested")
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
                log.e("startStream failed: ${(error as? PartyException)?.code ?: "UNKNOWN"} connectivity=$connectivity")
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
        mutableState.update { it.copy(leaveOpen = false, fullscreenMode = false) }
        voiceCoordinator.leave(roomId)
        mutableEffects.tryEmit(RoomEffect.Left)
    }

    private fun toggleMic() {
        if (mutableState.value.voiceState == VoiceConnectionState.IDLE) {
            mutableState.update { it.copy(micMuted = true) }
            return
        }
        voiceCoordinator.toggleMicrophone(roomId)
    }

    private fun clearSpeakingMembers() {
        if (speakingMembershipIds.isEmpty()) return
        speakingMembershipIds = emptySet()
        refreshParticipants()
    }

    private fun requestSync() {
        val session = realtime
        val streamId = streamSessionId
        val current = mutableState.value
        when {
            session == null -> {
                log.w("sync skipped: realtime not connected")
                mutableEffects.tryEmit(RoomEffect.SyncUnavailable)
            }
            streamId == null || !current.hasPlayer -> {
                log.w("sync skipped: no active stream streamSessionId=$streamId playback=${current.playback}")
                mutableEffects.tryEmit(RoomEffect.SyncUnavailable)
            }
            else -> {
                val requestId = newSyncRequestId()
                pendingSyncRequestId = requestId
                syncTimeoutJob?.cancel()
                // The server answers immediately from in-memory telemetry; a
                // missing reply means the request was lost. Surface it instead
                // of leaving the user without feedback.
                syncTimeoutJob = viewModelScope.launch {
                    delay(SYNC_TIMEOUT_MS)
                    if (pendingSyncRequestId == requestId) {
                        pendingSyncRequestId = null
                        log.w("sync timed out requestId=$requestId")
                        mutableEffects.tryEmit(RoomEffect.SyncUnavailable)
                    }
                }
                log.i("sync requested stream=$streamId position=${current.positionMs}ms requestId=$requestId")
                viewModelScope.launch {
                    runCatching { session.requestSync(streamId, current.positionMs, requestId) }
                        .onFailure { error ->
                            log.e("sync request failed: ${(error as? PartyException)?.code ?: "UNKNOWN"}")
                            if (pendingSyncRequestId == requestId) {
                                pendingSyncRequestId = null
                                syncTimeoutJob?.cancel()
                                mutableEffects.tryEmit(RoomEffect.SyncUnavailable)
                            }
                        }
                }
            }
        }
    }

    private fun newSyncRequestId(): String = "sync-" + Random.nextLong().toULong().toString(16)

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
            log.d("report> stream=$streamId pos=${current.positionMs}ms playing=${current.playback == RoomPlaybackState.PLAYING}")
            runCatching {
                session.reportPlayback(streamId, current.positionMs, current.playback == RoomPlaybackState.PLAYING)
            }.onFailure { error -> log.e("report failed: ${(error as? PartyException)?.code ?: "UNKNOWN"}") }
        }
    }

    private companion object {
        const val SKIP_STEP_MS = 15_000f
        const val REPORT_INTERVAL_MS = 3_000L
        const val SYNC_TIMEOUT_MS = 6_000L
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
