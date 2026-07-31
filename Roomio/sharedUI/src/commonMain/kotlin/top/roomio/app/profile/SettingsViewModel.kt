package top.roomio.app.profile

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal data class SettingsUiState(
    val draftName: String,
    val draftAvatar: ProfileAvatar,
) {
    val trimmedName: String
        get() = draftName.trim()

    val isNameError: Boolean
        get() = draftName.isNotEmpty() && trimmedName.isEmpty()

    val canSave: Boolean
        get() = trimmedName.isNotEmpty()
}

internal sealed interface SettingsAction {
    data class NameChanged(val name: String) : SettingsAction
    data class AvatarSelected(val avatar: ProfileAvatar) : SettingsAction
    data object BackClicked : SettingsAction
    data object SaveClicked : SettingsAction
}

internal class SettingsViewModel(
    identityName: String,
    identityAvatar: ProfileAvatar,
) : ViewModel() {
    private val mutableState = MutableStateFlow(
        SettingsUiState(
            draftName = identityName,
            draftAvatar = identityAvatar,
        ),
    )
    val state = mutableState.asStateFlow()

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.NameChanged -> mutableState.update {
                it.copy(draftName = action.name)
            }
            is SettingsAction.AvatarSelected -> mutableState.update {
                it.copy(draftAvatar = action.avatar)
            }
            SettingsAction.BackClicked,
            SettingsAction.SaveClicked,
            -> Unit
        }
    }
}
