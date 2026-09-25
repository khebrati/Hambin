package top.roomio.data.party

import android.content.Context
import android.content.Intent
import android.os.Build
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import top.roomio.domain.party.RoomRepository
import top.roomio.domain.party.RoomSession
import top.roomio.domain.party.SessionAction
import top.roomio.domain.party.SessionKeeper

internal actual fun createSessionKeeper(roomRepository: RoomRepository): SessionKeeper =
    AndroidSessionKeeper(AppContextHolder.appContext, roomRepository)

/**
 * Starts and stops [RoomSessionService] so the room session survives while the
 * app is backgrounded. Starting is best-effort: the platform may reject a
 * background start, but in this app it always follows a foreground user action.
 *
 * Closing the app is different from backgrounding it. When the last screen goes
 * away the session is ended with [stopAndLeave]: the membership is released on
 * the backend and only then is the service stopped, so other participants stop
 * seeing this device while the process is still alive to deliver the request.
 */
internal class AndroidSessionKeeper(
    private val context: Context,
    private val roomRepository: RoomRepository,
) : SessionKeeper {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var current: RoomSession? = null

    override val actions: Flow<SessionAction> = RoomSessionBus.actions

    override fun start(room: RoomSession) {
        current = room
        // Let the system-driven service end the session when the task is removed.
        RoomSessionBus.registerLeaveHandler(::leave)
        val intent = Intent(context, RoomSessionService::class.java).apply {
            action = RoomSessionService.ACTION_START
            putExtra(RoomSessionService.EXTRA_ROOM_ID, room.roomId)
            putExtra(RoomSessionService.EXTRA_ROOM_AS_OWNER, room.asOwner)
            putExtra(RoomSessionService.EXTRA_VOICE_ACTIVE, room.voiceActive)
            putExtra(RoomSessionService.EXTRA_MIC_MUTED, room.micMuted)
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun stop() {
        current = null
        RoomSessionBus.registerLeaveHandler(null)
        val intent = Intent(context, RoomSessionService::class.java)
        runCatching { context.stopService(intent) }
    }

    override fun stopAndLeave() {
        val room = current
        if (room == null) {
            stop()
            return
        }
        current = null
        // Keep the foreground service alive until the backend has released the
        // membership, then let the process stop the session normally.
        scope.launch {
            runCatching { roomRepository.leave(room.roomId) }
                .onFailure { log.w("leave on close failed room=${room.roomId}: ${it.message ?: it::class.simpleName}") }
            stop()
        }
    }

    /**
     * Entry point used by [RoomSessionService.onTaskRemoved] when the user
     * dismisses the app from recents and the room UI may already be gone.
     */
    private fun leave() {
        stopAndLeave()
    }

    private companion object {
        val log = Logger.withTag("SessionKeeper")
    }
}
