package top.roomio.app.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import top.roomio.app.navigation.RoomioAppState

internal data class HomeUiState(
    val identityName: String = "Nika",
    val identityAvatar: top.roomio.app.profile.ProfileAvatar =
        top.roomio.app.profile.ProfileAvatar.COMET,
    val roomPreview: HomeRoomPreview? = returnableHomeRoom,
)

internal sealed interface HomeAction {
    data object SettingsClicked : HomeAction
    data object CreatePartyClicked : HomeAction
    data object JoinPartyClicked : HomeAction
    data object OpenRoomClicked : HomeAction
}

internal class HomeViewModel(
    appState: StateFlow<RoomioAppState>,
) : ViewModel() {
    val state: StateFlow<HomeUiState> = appState
        .map { profile ->
            HomeUiState(
                identityName = profile.identityName,
                identityAvatar = profile.identityAvatar,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(
                identityName = appState.value.identityName,
                identityAvatar = appState.value.identityAvatar,
            ),
        )
}
