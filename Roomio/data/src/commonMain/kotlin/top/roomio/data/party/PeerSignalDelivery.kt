package top.roomio.data.party

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Rejected trickle ICE is a peer failure, never an uncaught session coroutine. */
internal suspend fun deliverPeerSignal(send: suspend () -> Unit, rejected: (Throwable) -> Unit) {
    try {
        send()
    } catch (error: Exception) {
        currentCoroutineContext().ensureActive()
        rejected(error)
    }
}
