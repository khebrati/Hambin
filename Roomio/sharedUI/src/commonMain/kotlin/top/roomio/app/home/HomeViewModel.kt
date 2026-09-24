package top.roomio.app.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import top.roomio.app.profile.ProfileAvatar
import top.roomio.app.profile.toPresentationAvatar
import top.roomio.domain.party.SessionRepository
import top.roomio.domain.profile.ProfileRepository

internal data class HomeUiState(
    val identityName: String = "Nika",
    val identityAvatar: ProfileAvatar = ProfileAvatar.COMET,
    val roomPreview: HomeRoomPreview? = returnableHomeRoom,
)

internal sealed interface HomeAction {
    data object SettingsClicked : HomeAction
    data object CreatePartyClicked : HomeAction
    data object JoinPartyClicked : HomeAction
    data object OpenRoomClicked : HomeAction
}

@Inject
internal class HomeViewModel(
    profileRepository: ProfileRepository,
    sessionRepository: SessionRepository,
) : ViewModel() {
    init {
        // Register the guest identity as soon as the app opens so rooms can be
        // created or joined without a separate sign-in step.
        viewModelScope.launch {
            val profile = profileRepository.currentProfile()
            runCatching {
                sessionRepository.ensureSession(
                    name = profile.name,
                    avatar = profile.avatar.name,
                )
            }
        }
    }

    val state: StateFlow<HomeUiState> = profileRepository
        .observeProfile()
        .map { profile ->
            HomeUiState(
                identityName = profile.name,
                identityAvatar = profile.avatar.toPresentationAvatar(),
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
