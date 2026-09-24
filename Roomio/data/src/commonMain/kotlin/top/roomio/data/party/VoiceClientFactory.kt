package top.roomio.data.party

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import top.roomio.domain.party.VoiceClient
import top.roomio.domain.party.VoiceEvent
import top.roomio.domain.party.VoiceSession
import top.roomio.domain.party.VoiceToken

/** Builds the platform voice client. Android uses LiveKit; other targets are disabled. */
internal expect fun createVoiceClient(): VoiceClient

/**
 * A voice client for platforms without a voice implementation. Joining fails
 * immediately so the UI can show that voice is unavailable.
 */
internal class DisabledVoiceClient : VoiceClient {
    override suspend fun connect(token: VoiceToken): VoiceSession = DisabledVoiceSession
}

private object DisabledVoiceSession : VoiceSession {
    override val events: Flow<VoiceEvent> = flowOf(VoiceEvent.Failed("Voice is unavailable on this device"))
    override fun setMicrophoneEnabled(enabled: Boolean) = Unit
    override suspend fun disconnect() = Unit
}
