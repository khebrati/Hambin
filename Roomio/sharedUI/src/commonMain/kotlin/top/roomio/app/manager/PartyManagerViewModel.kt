package top.roomio.app.manager

import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal data class PartyManagerUiState(
    val mode: ManagerMode,
    val code: String = "",
    val isLoading: Boolean = false,
    val hasCodeError: Boolean = false,
) {
    val normalizedCode: String
        get() = code.trim().uppercase()

    val canPreview: Boolean
        get() = normalizedCode.isNotEmpty() && !isLoading
}

internal sealed interface PartyManagerAction {
    data class ModeSelected(val mode: ManagerMode) : PartyManagerAction
    data class CodeChanged(val code: String) : PartyManagerAction
    data object BackClicked : PartyManagerAction
    data object CreateClicked : PartyManagerAction
    data object PreviewClicked : PartyManagerAction
    data object PreviewRejected : PartyManagerAction
}

@AssistedInject
internal class PartyManagerViewModel(
    @Assisted initialMode: ManagerMode,
    @Assisted loading: Boolean = false,
    @Assisted codeError: Boolean = false,
) : ViewModel() {
    private val mutableState = MutableStateFlow(
        PartyManagerUiState(
            mode = initialMode,
            code = if (codeError) "NOVA-99" else "",
            isLoading = loading,
            hasCodeError = codeError,
        ),
    )
    val state = mutableState.asStateFlow()

    @AssistedFactory
    fun interface Factory {
        fun create(
            initialMode: ManagerMode,
            loading: Boolean,
            codeError: Boolean,
        ): PartyManagerViewModel
    }

    fun onAction(action: PartyManagerAction) {
        when (action) {
            is PartyManagerAction.ModeSelected -> mutableState.update {
                it.copy(mode = action.mode)
            }
            is PartyManagerAction.CodeChanged -> mutableState.update {
                it.copy(
                    code = action.code.uppercase(),
                    hasCodeError = false,
                )
            }
            PartyManagerAction.PreviewRejected -> mutableState.update {
                it.copy(hasCodeError = true)
            }
            PartyManagerAction.BackClicked,
            PartyManagerAction.CreateClicked,
            PartyManagerAction.PreviewClicked,
            -> Unit
        }
    }
}
