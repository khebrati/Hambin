package top.roomio.data.party

import android.content.Context
import android.content.Intent
import android.os.Build
import kotlinx.coroutines.flow.Flow
import top.roomio.domain.party.RoomSession
import top.roomio.domain.party.SessionAction
import top.roomio.domain.party.SessionKeeper

internal actual fun createSessionKeeper(): SessionKeeper =
    AndroidSessionKeeper(AppContextHolder.appContext)

/**
 * Starts and stops [RoomSessionService] so the room session survives while the
 * app is backgrounded. Starting is best-effort: the platform may reject a
 * background start, but in this app it always follows a foreground user action.
 */
internal class AndroidSessionKeeper(
    private val context: Context,
) : SessionKeeper {

    override val actions: Flow<SessionAction> = RoomSessionBus.actions

    override fun start(room: RoomSession) {
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
        val intent = Intent(context, RoomSessionService::class.java)
        runCatching { context.stopService(intent) }
    }
}
