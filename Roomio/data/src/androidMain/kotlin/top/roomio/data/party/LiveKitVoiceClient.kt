package top.roomio.data.party

import android.content.Context
import co.touchlab.kermit.Logger
import io.livekit.android.LiveKit
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.room.Room
import io.livekit.android.room.track.RemoteAudioTrack
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
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
    /**
     * The client is application-scoped. A reconnect creates a new room, so the
     * previously active room is disconnected and released first; otherwise every
     * reconnect would leak a room and its peer connection.
     */
    private var activeRoom: Room? = null

    override suspend fun connect(token: VoiceToken): VoiceSession {
        activeRoom?.let { previous ->
            runCatching { previous.disconnect() }
            runCatching { previous.release() }
        }
        val room = LiveKit.create(context.applicationContext)
        activeRoom = room
        room.connect(token.url, token.token)
        room.setSpeakerMute(false)
        log.i("voice connected url=${token.url}")
        return LiveKitVoiceSession(room) {
            if (activeRoom === room) activeRoom = null
        }
    }
}

/** A LiveKit-backed [VoiceSession]. */
internal class LiveKitVoiceSession(
    private val room: Room,
    private val onReleased: () -> Unit = {},
) : VoiceSession {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Voice events for this session. The flow always completes once the room is
     * gone (disconnected, failed, or stalled) so callers can reconnect with a
     * fresh token instead of waiting on a dead session forever.
     */
    override val events: Flow<VoiceEvent> = callbackFlow {
        var speakers: Set<String> = emptySet()
        fun publishSpeakers() {
            trySend(VoiceEvent.SpeakersChanged(speakers))
        }

        val collector = scope.launch {
            try {
                room.events.collect { event ->
                    when (event) {
                        is RoomEvent.ActiveSpeakersChanged -> {
                            speakers = event.speakers.mapNotNull { it.identity?.value }.toSet()
                            log.d("active speakers count=${speakers.size}")
                            publishSpeakers()
                        }
                        is RoomEvent.Connected -> {
                            log.i("room connected")
                            room.setSpeakerMute(false)
                            trySend(VoiceEvent.Connected)
                        }
                        is RoomEvent.Disconnected -> {
                            log.w("room disconnected reason=${event.reason} error=${event.error?.message}")
                            speakers = emptySet()
                            publishSpeakers()
                            trySend(VoiceEvent.Disconnected)
                            close()
                        }
                        is RoomEvent.FailedToConnect -> {
                            log.e("room failed to connect: ${event.error.message ?: event.error::class.simpleName}")
                            trySend(VoiceEvent.Failed(event.error.message))
                            close()
                        }
                        is RoomEvent.Reconnecting -> log.w("room reconnecting")
                        is RoomEvent.Reconnected -> {
                            log.i("room reconnected")
                            room.setSpeakerMute(false)
                        }
                        is RoomEvent.ParticipantConnected -> log.i("participant connected id=${event.participant.identity?.value}")
                        is RoomEvent.ParticipantDisconnected -> {
                            val id = event.participant.identity?.value
                            log.i("participant disconnected id=$id")
                            if (id != null && speakers.contains(id)) {
                                speakers = speakers - id
                                publishSpeakers()
                            }
                        }
                        is RoomEvent.TrackSubscribed -> {
                            log.i("track subscribed kind=${event.track.kind} from=${event.participant.identity?.value}")
                            restoreAudioOutput(event.track as? RemoteAudioTrack, event.publication.sid)
                        }
                        is RoomEvent.TrackUnsubscribed -> log.i("track unsubscribed kind=${event.track.kind} from=${event.participant.identity?.value}")
                        is RoomEvent.TrackMuted -> {
                            val id = event.participant.identity?.value
                            log.i("track muted sid=${event.publication.sid} from=$id")
                            // A muted track is no longer speaking. Do not wait for
                            // ActiveSpeakersChanged, which can be delayed or lost.
                            if (id != null && speakers.contains(id)) {
                                speakers = speakers - id
                                publishSpeakers()
                            }
                        }
                        is RoomEvent.TrackUnmuted -> {
                            log.i("track unmuted sid=${event.publication.sid}")
                            restoreAudioOutput(event.publication.track as? RemoteAudioTrack, event.publication.sid)
                        }
                        else -> Unit
                    }
                }
                // A hot event flow should never complete; if it does the session is dead.
                log.w("room event stream ended")
                trySend(VoiceEvent.Disconnected)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                // Without this the detached collector dies silently and the events
                // flow never completes, stranding the reconnect loop forever.
                log.e("room event stream failed: ${error.message ?: error::class.simpleName}")
                trySend(VoiceEvent.Failed(error.message ?: "voice event stream failed"))
            } finally {
                close()
            }
        }

        // LiveKit can lose the peer connection without delivering a terminal room
        // event. Poll the room state so a dead session is still detected and the
        // caller reconnects instead of staying parked indefinitely.
        val watchdog = scope.launch {
            while (isActive) {
                delay(VOICE_STATE_POLL_MS)
                if (room.state == Room.State.DISCONNECTED) {
                    log.w("room state DISCONNECTED; ending voice session")
                    trySend(VoiceEvent.Disconnected)
                    close()
                    break
                }
            }
        }

        awaitClose {
            collector.cancel()
            watchdog.cancel()
        }
    }

    /** Reasserts the remote-speaker route after reconnects and track renegotiation. */
    private fun restoreAudioOutput(track: RemoteAudioTrack?, trackSid: String) {
        room.setSpeakerMute(false)
        if (track != null) {
            track.setVolume(1.0)
            log.i("remote audio output enabled sid=$trackSid")
        }
    }

    override fun setMicrophoneEnabled(enabled: Boolean) {
        scope.launch {
            // Muting/unmuting capture must not leave the shared WebRTC audio
            // device module with its remote speaker path muted.
            room.setSpeakerMute(false)
            runCatching { room.localParticipant.setMicrophoneEnabled(enabled) }
                .onSuccess { log.i("microphone enabled=$enabled") }
                .onFailure { error -> log.e("setMicrophoneEnabled failed: ${error.message ?: error::class.simpleName}") }
        }
    }

    override suspend fun disconnect() {
        room.disconnect()
        runCatching { room.release() }
        scope.cancel()
        onReleased()
    }
}

/** How often a live session checks the room state for a missed disconnect. */
private const val VOICE_STATE_POLL_MS = 5_000L
