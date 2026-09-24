package top.roomio.data.party

import android.content.Context
import android.content.Intent
import android.os.Build
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

    override fun start(voiceActive: Boolean) {
        val intent = Intent(context, RoomSessionService::class.java).apply {
            action = RoomSessionService.ACTION_START
            putExtra(RoomSessionService.EXTRA_VOICE_ACTIVE, voiceActive)
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
