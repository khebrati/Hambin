package top.roomio.app.home

import top.roomio.app.diagnostics.logHandledException

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import top.roomio.domain.party.RoomRepository
import top.roomio.domain.party.RoomAvailability
import top.roomio.domain.party.PartyException
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import top.roomio.app.profile.ProfileAvatar
import top.roomio.app.profile.toPresentationAvatar
import top.roomio.domain.party.SessionRepository
import top.roomio.domain.profile.ProfileRepository

internal data class HomeUiState(
    val identityName: String = "Nika",
    val identityAvatar: ProfileAvatar = ProfileAvatar.COMET,
    val roomPreview: HomeRoomPreview? = null,
)

internal sealed interface HomeAction {
    data object SettingsClicked : HomeAction
    data object CreatePartyClicked : HomeAction
    data object JoinPartyClicked : HomeAction
    data object OpenRoomClicked : HomeAction
}

@Inject
internal class HomeViewModel(
    private val profileRepository: ProfileRepository,
    private val sessionRepository: SessionRepository,
    private val roomRepository: RoomRepository,
) : ViewModel() {
    private val lastRoom = MutableStateFlow<HomeRoomPreview?>(null)
    private var refreshJob: Job? = null

    fun refreshLastRoom() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            try {
                fun top.roomio.domain.party.Room.toCard() = HomeRoomPreview(title, 0, true, code)
                lastRoom.value = roomRepository.lastRoom()?.toCard()
                val profile = profileRepository.currentProfile()
                sessionRepository.ensureSession(profile.name, profile.avatar.name, profile.language.tag)
                val saved = roomRepository.lastRoom() ?: return@launch
                lastRoom.value = saved.toCard()
                val preview = roomRepository.preview(saved.code)
                lastRoom.value = if (preview.availability == RoomAvailability.ENDED) null else saved.toCard().copy(title = preview.title)
            } catch (cancelled: CancellationException) {
                logHandledException("HomeViewModel.catch1", cancelled)
                throw cancelled
            } catch (error: Exception) {
                logHandledException("HomeViewModel.catch2", error)
                if (error is PartyException && error.code in setOf("ROOM_ENDED", "ROOM_NOT_FOUND")) lastRoom.value = null
                // Keep the cached card offline; opening it offers preview retry.
            }
        }
    }

    val state: StateFlow<HomeUiState> = combine(profileRepository.observeProfile(), lastRoom) { profile, room ->
            HomeUiState(
                identityName = profile.name,
                identityAvatar = profile.avatar.toPresentationAvatar(),
                roomPreview = room,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = profileRepository.currentProfile().let { profile ->
                HomeUiState(
                    identityName = profile.name,
                    identityAvatar = profile.avatar.toPresentationAvatar(),
                )
            },
        )
}
