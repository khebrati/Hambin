package top.roomio.data.party

import top.roomio.domain.party.VoiceClient

internal actual fun createVoiceClient(): VoiceClient = DisabledVoiceClient()
