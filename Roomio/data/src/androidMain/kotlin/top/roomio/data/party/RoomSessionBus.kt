package top.roomio.data.party

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import top.roomio.domain.party.SessionAction

/**
 * Process-wide channel between [RoomSessionService] and the in-process room
 * session. The service is created by the Android framework, so it cannot share
 * the application's dependency graph directly; both sides meet here instead.
 *
 * The service publishes the user's notification buttons and the room session
 * observes them.
 */
internal object RoomSessionBus {
    private val mutableActions = MutableSharedFlow<SessionAction>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val actions: SharedFlow<SessionAction> = mutableActions.asSharedFlow()

    fun send(action: SessionAction) {
        mutableActions.tryEmit(action)
    }
}
