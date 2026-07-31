package top.roomio.app.preview

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class RoomPreviewUiState(
    val model: RoomPreviewModel,
) {
    val canJoin: Boolean
        get() = model.availability == RoomAvailability.AVAILABLE
}

internal sealed interface RoomPreviewAction {
    data object BackClicked : RoomPreviewAction
    data object JoinClicked : RoomPreviewAction
}

internal class RoomPreviewViewModel(
    model: RoomPreviewModel,
) : ViewModel() {
    private val mutableState = MutableStateFlow(RoomPreviewUiState(model))
    val state = mutableState.asStateFlow()
}
