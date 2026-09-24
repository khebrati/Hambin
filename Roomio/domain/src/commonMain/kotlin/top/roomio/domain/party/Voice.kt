package top.roomio.domain.party

import kotlinx.coroutines.flow.Flow

/**
 * Opens room-scoped voice sessions using tokens issued by the backend. Voice
 * is opt-in, starts muted, and is independent of room and playback state.
 */
interface VoiceClient {
    /** Connects to the voice room with a room-scoped [VoiceToken]. */
    suspend fun connect(token: VoiceToken): VoiceSession
}

/** A live voice connection. Disconnect it when leaving the room. */
interface VoiceSession {
    /** Voice events such as active-speaker changes and connection state. */
    val events: Flow<VoiceEvent>

    /** Enables or disables the local microphone. */
    fun setMicrophoneEnabled(enabled: Boolean)

    /** Leaves the voice room. */
    suspend fun disconnect()
}

sealed interface VoiceEvent {
    /** The room connection is established. */
    data object Connected : VoiceEvent

    /** The room connection was closed. */
    data object Disconnected : VoiceEvent

    /** The connection could not be established. */
    data class Failed(val reason: String?) : VoiceEvent

    /** The membership IDs that are currently speaking, loudest first. */
    data class SpeakersChanged(val speakingMembershipIds: Set<String>) : VoiceEvent
}
