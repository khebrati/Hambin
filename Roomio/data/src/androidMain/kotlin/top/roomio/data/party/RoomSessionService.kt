package top.roomio.data.party

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.os.Build
import android.os.IBinder
import top.roomio.data.R
import top.roomio.domain.party.RoomSessionExtras
import top.roomio.domain.party.SessionAction

/**
 * Foreground service that keeps the room session alive while the app is in the
 * background: the realtime socket and the LiveKit voice call keep running, and a
 * persistent notification shows that the party is still active.
 *
 * It runs for as long as the user is inside a room and stops on leave. The
 * notification returns the user to the room on tap and exposes microphone and
 * leave actions handled through [RoomSessionBus].
 */
class RoomSessionService : Service() {

    private var roomId: String? = null
    private var asOwner: Boolean = false
    private var voiceActive: Boolean = false
    private var micMuted: Boolean = true
    private var foregroundStarted: Boolean = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSession()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE_MIC -> {
                toggleMicrophone()
                return START_STICKY
            }
            ACTION_LEAVE -> {
                // The room owns leaving: it disconnects voice and tells the
                // backend, then the service goes away.
                RoomSessionBus.send(SessionAction.Leave)
                stopSession()
                return START_NOT_STICKY
            }
            else -> {
                roomId = intent?.getStringExtra(EXTRA_ROOM_ID)
                asOwner = intent?.getBooleanExtra(EXTRA_ROOM_AS_OWNER, false) ?: false
                voiceActive = intent?.getBooleanExtra(EXTRA_VOICE_ACTIVE, false) ?: false
                micMuted = intent?.getBooleanExtra(EXTRA_MIC_MUTED, true) ?: true
                startForegroundCompat()
            }
        }
        // Restart if the process is killed while the user is still in the room.
        return START_STICKY
    }

    override fun onDestroy() {
        stopForegroundCompat()
        super.onDestroy()
    }

    private fun toggleMicrophone() {
        micMuted = !micMuted
        RoomSessionBus.send(if (micMuted) SessionAction.MuteVoice else SessionAction.UnmuteVoice)
        if (foregroundStarted) notifyForeground()
    }

    private fun startForegroundCompat() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(manager)
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val started = runCatching {
                startForeground(NOTIFICATION_ID, notification, foregroundServiceType())
            }.isSuccess
            // The platform can refuse a microphone service started from the
            // background; a data-sync session still keeps the room connected.
            if (!started) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        foregroundStarted = true
    }

    private fun notifyForeground() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(manager)
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun stopSession() {
        foregroundStarted = false
        stopForegroundCompat()
        stopSelf()
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun foregroundServiceType(): Int {
        val microphone = voiceActive && hasMicrophonePermission()
        return if (microphone) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        }
    }

    private fun buildNotification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        builder
            .setSmallIcon(R.drawable.ic_roomio_session)
            .setContentTitle(getString(R.string.session_notification_title))
            .setContentText(
                getString(
                    if (voiceActive) {
                        R.string.session_notification_text_voice
                    } else {
                        R.string.session_notification_text
                    },
                ),
            )
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
        launchIntent()?.let { builder.setContentIntent(it) }
        if (voiceActive) {
            builder.addAction(microphoneAction())
        }
        builder.addAction(leaveAction())
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            @Suppress("DEPRECATION")
            builder.setPriority(Notification.PRIORITY_LOW)
        }
        return builder.build()
    }

    private fun microphoneAction(): Notification.Action {
        val icon = if (micMuted) R.drawable.ic_roomio_unmute else R.drawable.ic_roomio_mute
        val title = getString(
            if (micMuted) R.string.session_action_unmute else R.string.session_action_mute,
        )
        return action(icon, title, REQUEST_TOGGLE_MIC, ACTION_TOGGLE_MIC)
    }

    private fun leaveAction(): Notification.Action =
        action(
            R.drawable.ic_roomio_leave,
            getString(R.string.session_action_leave),
            REQUEST_LEAVE,
            ACTION_LEAVE,
        )

    private fun action(iconRes: Int, title: String, requestCode: Int, action: String): Notification.Action {
        val pendingIntent = PendingIntent.getService(
            this,
            requestCode,
            Intent(this, RoomSessionService::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Action.Builder(Icon.createWithResource(this, iconRes), title, pendingIntent).build()
    }

    private fun launchIntent(): PendingIntent? {
        val launch = packageManager.getLaunchIntentForPackage(packageName) ?: return null
        launch.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        roomId?.let { id ->
            launch.putExtra(RoomSessionExtras.ROOM_ID, id)
            launch.putExtra(RoomSessionExtras.AS_OWNER, asOwner)
        }
        return PendingIntent.getActivity(
            this,
            REQUEST_OPEN,
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun ensureChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.session_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.session_channel_description)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun hasMicrophonePermission(): Boolean =
        checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    companion object {
        const val ACTION_START = "top.roomio.data.party.action.START_SESSION"
        const val ACTION_STOP = "top.roomio.data.party.action.STOP_SESSION"
        const val ACTION_TOGGLE_MIC = "top.roomio.data.party.action.TOGGLE_MIC"
        const val ACTION_LEAVE = "top.roomio.data.party.action.LEAVE"
        const val EXTRA_ROOM_ID = "roomId"
        const val EXTRA_ROOM_AS_OWNER = "roomAsOwner"
        const val EXTRA_VOICE_ACTIVE = "voiceActive"
        const val EXTRA_MIC_MUTED = "micMuted"
        private const val CHANNEL_ID = "roomio_session"
        private const val NOTIFICATION_ID = 4701
        private const val REQUEST_OPEN = 1
        private const val REQUEST_TOGGLE_MIC = 2
        private const val REQUEST_LEAVE = 3
    }
}
