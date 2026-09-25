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

    @Volatile
    private var leaveHandler: (() -> Unit)? = null

    val actions: SharedFlow<SessionAction> = mutableActions.asSharedFlow()

    fun send(action: SessionAction) {
        mutableActions.tryEmit(action)
    }

    /**
     * Registers the in-process owner of the room session so [RoomSessionService]
     * can end it when the app is dismissed. Pass null when the session stops.
     */
    fun registerLeaveHandler(handler: (() -> Unit)?) {
        leaveHandler = handler
    }

    /**
     * Asks the in-process room session to leave the room because the app is
     * closing. Returns false when no session is registered, so the caller can
     * stop the service itself.
     */
    fun requestLeaveFromTaskRemoval(): Boolean {
        val handler = leaveHandler ?: return false
        handler()
        return true
    }
}
