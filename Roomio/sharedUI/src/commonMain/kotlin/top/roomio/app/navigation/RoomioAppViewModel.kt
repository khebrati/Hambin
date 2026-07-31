package top.roomio.app.navigation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import top.roomio.app.profile.ProfileAvatar

internal data class RoomioAppState(
    val identityName: String = "Nika",
    val identityAvatar: ProfileAvatar = ProfileAvatar.COMET,
)

internal sealed interface RoomioAppAction {
    data class ProfileSaved(
        val name: String,
        val avatar: ProfileAvatar,
    ) : RoomioAppAction
}

internal class RoomioAppViewModel : ViewModel() {
    private val mutableState = MutableStateFlow(RoomioAppState())
    val state = mutableState.asStateFlow()

    fun onAction(action: RoomioAppAction) {
        when (action) {
            is RoomioAppAction.ProfileSaved -> mutableState.update {
                it.copy(
                    identityName = action.name,
                    identityAvatar = action.avatar,
                )
            }
        }
    }
}
