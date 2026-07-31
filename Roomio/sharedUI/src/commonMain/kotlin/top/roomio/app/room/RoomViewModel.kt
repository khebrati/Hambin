package top.roomio.app.room

import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal enum class InviteCopyTarget { CODE, LINK }

internal enum class InviteCopyState { IDLE, CODE, LINK, ERROR }

internal data class RoomUiState(
    val model: RoomScreenModel,
    val playback: RoomPlaybackState = model.playback,
    val videoUrl: String = "",
    val playbackPosition: Float = 0.13f,
    val volume: Float = 0.72f,
    val micMuted: Boolean = false,
    val theaterMode: Boolean = false,
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
    data class PlaybackPositionChanged(val position: Float) : RoomAction
    data class PlaybackSkipped(val direction: Float) : RoomAction
    data class VolumeChanged(val volume: Float) : RoomAction
    data class InviteCopySucceeded(val target: InviteCopyTarget) : RoomAction
    data object InviteCopyFailed : RoomAction
    data object StartPlaybackClicked : RoomAction
    data object RetryPlaybackClicked : RoomAction
    data object TogglePlaybackClicked : RoomAction
    data object ToggleVolumeClicked : RoomAction
    data object ToggleTheaterClicked : RoomAction
    data object ToggleMicClicked : RoomAction
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

internal enum class RoomEffect {
    SYNC_COMPLETED,
    PARTY_CODE_COPIED,
    INVITE_LINK_COPIED,
    COPY_UNAVAILABLE,
}

@AssistedInject
internal class RoomViewModel(
    @Assisted model: RoomScreenModel,
) : ViewModel() {
    private val mutableState = MutableStateFlow(RoomUiState(model))
    val state = mutableState.asStateFlow()

    private val mutableEffects = MutableSharedFlow<RoomEffect>(extraBufferCapacity = 1)
    val effects = mutableEffects.asSharedFlow()

    @AssistedFactory
    fun interface Factory {
        fun create(model: RoomScreenModel): RoomViewModel
    }

    fun onAction(action: RoomAction) {
        when (action) {
            is RoomAction.VideoUrlChanged -> mutableState.update {
                it.copy(
                    videoUrl = action.url,
                    playback = if (it.playback == RoomPlaybackState.ERROR) {
                        RoomPlaybackState.OWNER_EMPTY
                    } else {
                        it.playback
                    },
                )
            }
            is RoomAction.VideoUrlPasted -> mutableState.update { it.copy(videoUrl = action.url) }
            is RoomAction.PlaybackPositionChanged -> mutableState.update {
                it.copy(playbackPosition = action.position.coerceIn(0f, 1f))
            }
            is RoomAction.PlaybackSkipped -> mutableState.update {
                it.copy(
                    playbackPosition = (it.playbackPosition + action.direction * 0.035f)
                        .coerceIn(0f, 1f),
                )
            }
            is RoomAction.VolumeChanged -> mutableState.update {
                it.copy(volume = action.volume.coerceIn(0f, 1f))
            }
            is RoomAction.InviteCopySucceeded -> {
                mutableState.update {
                    it.copy(
                        inviteCopyState = when (action.target) {
                            InviteCopyTarget.CODE -> InviteCopyState.CODE
                            InviteCopyTarget.LINK -> InviteCopyState.LINK
                        },
                    )
                }
                emitEffect(
                    when (action.target) {
                        InviteCopyTarget.CODE -> RoomEffect.PARTY_CODE_COPIED
                        InviteCopyTarget.LINK -> RoomEffect.INVITE_LINK_COPIED
                    },
                )
            }
            RoomAction.InviteCopyFailed -> {
                mutableState.update { it.copy(inviteCopyState = InviteCopyState.ERROR) }
                emitEffect(RoomEffect.COPY_UNAVAILABLE)
            }
            RoomAction.StartPlaybackClicked -> startPlayback()
            RoomAction.RetryPlaybackClicked -> mutableState.update {
                it.copy(
                    playback = if (it.isOwner) {
                        RoomPlaybackState.OWNER_EMPTY
                    } else {
                        RoomPlaybackState.WAITING_FOR_OWNER
                    },
                )
            }
            RoomAction.TogglePlaybackClicked -> mutableState.update {
                it.copy(
                    playback = if (it.playback == RoomPlaybackState.PLAYING) {
                        RoomPlaybackState.PAUSED
                    } else {
                        RoomPlaybackState.PLAYING
                    },
                )
            }
            RoomAction.ToggleVolumeClicked -> mutableState.update {
                it.copy(volume = if (it.volume == 0f) 0.72f else 0f)
            }
            RoomAction.ToggleTheaterClicked -> mutableState.update {
                it.copy(theaterMode = !it.theaterMode)
            }
            RoomAction.ToggleMicClicked -> mutableState.update { it.copy(micMuted = !it.micMuted) }
            RoomAction.SyncClicked -> {
                mutableState.update { it.copy(playbackPosition = 0.21f) }
                emitEffect(RoomEffect.SYNC_COMPLETED)
            }
            RoomAction.InviteClicked -> mutableState.update {
                it.copy(inviteOpen = true, inviteCopyState = InviteCopyState.IDLE)
            }
            RoomAction.InviteDismissed -> mutableState.update { it.copy(inviteOpen = false) }
            RoomAction.LinkClicked -> mutableState.update { it.copy(linkOpen = true) }
            RoomAction.LinkDismissed -> mutableState.update { it.copy(linkOpen = false) }
            RoomAction.LeaveClicked -> mutableState.update { it.copy(leaveOpen = true) }
            RoomAction.LeaveDismissed -> mutableState.update { it.copy(leaveOpen = false) }
            RoomAction.LeaveConfirmed -> mutableState.update { it.copy(leaveOpen = false) }
            RoomAction.EndStreamClicked -> mutableState.update { it.copy(abortOpen = true) }
            RoomAction.EndStreamDismissed -> mutableState.update { it.copy(abortOpen = false) }
            RoomAction.EndStreamConfirmed -> mutableState.update {
                it.copy(
                    playback = RoomPlaybackState.ABORTED,
                    abortOpen = false,
                )
            }
        }
    }

    private fun startPlayback() {
        if (!state.value.videoUrl.startsWith("http")) {
            mutableState.update { it.copy(playback = RoomPlaybackState.ERROR) }
            return
        }

        mutableState.update { it.copy(playback = RoomPlaybackState.LOADING) }
        viewModelScope.launch {
            delay(650)
            mutableState.update {
                if (it.playback == RoomPlaybackState.LOADING) {
                    it.copy(playback = RoomPlaybackState.PLAYING)
                } else {
                    it
                }
            }
        }
    }

    private fun emitEffect(effect: RoomEffect) {
        mutableEffects.tryEmit(effect)
    }
}
