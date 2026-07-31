package top.roomio.app.profile

import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import top.roomio.domain.profile.ProfileRepository
import top.roomio.domain.profile.UserProfile

internal data class SettingsUiState(
    val draftName: String,
    val draftAvatar: ProfileAvatar,
    val saveFailed: Boolean = false,
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

internal enum class SettingsEffect {
    PROFILE_SAVED,
}

@Inject
internal class SettingsViewModel(
    private val profileRepository: ProfileRepository,
) : ViewModel() {
    private val initialProfile = profileRepository.currentProfile()
    private val mutableState = MutableStateFlow(
        SettingsUiState(
            draftName = initialProfile.name,
            draftAvatar = initialProfile.avatar.toPresentationAvatar(),
        ),
    )
    val state = mutableState.asStateFlow()

    private val mutableEffects = MutableSharedFlow<SettingsEffect>(extraBufferCapacity = 1)
    val effects = mutableEffects.asSharedFlow()

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.NameChanged -> mutableState.update {
                it.copy(
                    draftName = action.name,
                    saveFailed = false,
                )
            }
            is SettingsAction.AvatarSelected -> mutableState.update {
                it.copy(
                    draftAvatar = action.avatar,
                    saveFailed = false,
                )
            }
            SettingsAction.SaveClicked -> saveProfile()
            SettingsAction.BackClicked -> Unit
        }
    }

    private fun saveProfile() {
        val currentState = state.value
        if (!currentState.canSave) return

        profileRepository.saveProfile(
            UserProfile(
                name = currentState.trimmedName,
                avatar = currentState.draftAvatar.toDomainAvatar(),
            ),
        ).fold(
            onSuccess = {
                mutableState.update { it.copy(saveFailed = false) }
                mutableEffects.tryEmit(SettingsEffect.PROFILE_SAVED)
            },
            onFailure = {
                mutableState.update { it.copy(saveFailed = true) }
            },
        )
    }
}
