package top.roomio.data.party

import android.content.Context
import co.touchlab.kermit.Logger
import io.livekit.android.LiveKit
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import top.roomio.domain.party.VoiceClient
import top.roomio.domain.party.VoiceEvent
import top.roomio.domain.party.VoiceSession
import top.roomio.domain.party.VoiceToken

private val log = Logger.withTag("LiveKitVoice")

/** Holds the application context for LiveKit, initialized by the Android launcher. */
internal object AppContextHolder {
    lateinit var appContext: Context
}

internal actual fun createVoiceClient(): VoiceClient = LiveKitVoiceClient(AppContextHolder.appContext)

/** A LiveKit-backed [VoiceClient] for Android. */
internal class LiveKitVoiceClient(
    private val context: Context,
) : VoiceClient {
    override suspend fun connect(token: VoiceToken): VoiceSession {
        val room = LiveKit.create(context.applicationContext)
        room.connect(token.url, token.token)
        log.i("voice connected url=${token.url}")
        return LiveKitVoiceSession(room)
    }
}

/** A LiveKit-backed [VoiceSession]. */
internal class LiveKitVoiceSession(
    private val room: Room,
) : VoiceSession {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Voice events for this session. The flow completes once the room is gone
     * (disconnected or failed to connect) so callers can reconnect with a fresh
     * token instead of waiting on a dead session forever.
     */
    override val events: Flow<VoiceEvent> = callbackFlow {
        val job = scope.launch {
            room.events.collect { event ->
                when (event) {
                    is RoomEvent.ActiveSpeakersChanged -> {
                        val speaking = event.speakers.mapNotNull { it.identity?.value }.toSet()
                        log.d("active speakers count=${speaking.size}")
                        trySend(VoiceEvent.SpeakersChanged(speaking))
                    }
                    is RoomEvent.Connected -> {
                        log.i("room connected")
                        trySend(VoiceEvent.Connected)
                    }
                    is RoomEvent.Disconnected -> {
                        log.w("room disconnected reason=${event.reason} error=${event.error?.message}")
                        trySend(VoiceEvent.Disconnected)
                        close()
                    }
                    is RoomEvent.FailedToConnect -> {
                        log.e("room failed to connect: ${event.error.message ?: event.error::class.simpleName}")
                        trySend(VoiceEvent.Failed(event.error.message))
                        close()
                    }
                    is RoomEvent.Reconnecting -> log.w("room reconnecting")
                    is RoomEvent.Reconnected -> log.i("room reconnected")
                    is RoomEvent.ParticipantConnected -> log.i("participant connected id=${event.participant.identity?.value}")
                    is RoomEvent.ParticipantDisconnected -> log.i("participant disconnected id=${event.participant.identity?.value}")
                    is RoomEvent.TrackSubscribed -> log.i("track subscribed kind=${event.track.kind} from=${event.participant.identity?.value}")
                    is RoomEvent.TrackUnsubscribed -> log.i("track unsubscribed kind=${event.track.kind} from=${event.participant.identity?.value}")
                    is RoomEvent.TrackMuted -> log.i("track muted sid=${event.publication.sid}")
                    is RoomEvent.TrackUnmuted -> log.i("track unmuted sid=${event.publication.sid}")
                    else -> Unit
                }
            }
        }
        awaitClose { job.cancel() }
    }

    override fun setMicrophoneEnabled(enabled: Boolean) {
        scope.launch {
            runCatching { room.localParticipant.setMicrophoneEnabled(enabled) }
                .onSuccess { log.i("microphone enabled=$enabled") }
                .onFailure { error -> log.e("setMicrophoneEnabled failed: ${error.message ?: error::class.simpleName}") }
        }
    }

    override suspend fun disconnect() {
        room.disconnect()
        scope.cancel()
    }
}
