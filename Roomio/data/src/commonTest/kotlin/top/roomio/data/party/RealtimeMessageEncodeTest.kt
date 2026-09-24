package top.roomio.data.party

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertTrue
import top.roomio.data.party.dto.PlaybackReportMessage
import top.roomio.data.party.dto.SyncRequestMessage

class RealtimeMessageEncodeTest {

    // Mirrors the production Json config (PartyDataBindings.provideJson), which
    // does not enable encodeDefaults.
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun playbackReportAlwaysEncodesItsTypeField() {
        val encoded = json.encodeToString(
            PlaybackReportMessage.serializer(),
            PlaybackReportMessage(streamSessionId = "s1", positionMs = 1234, playing = true),
        )
        assertTrue(
            encoded.contains("\"type\":\"playback.report\""),
            "playback.report frame must carry its type so the server can dispatch: $encoded",
        )
    }

    @Test
    fun syncRequestAlwaysEncodesItsTypeField() {
        val encoded = json.encodeToString(
            SyncRequestMessage.serializer(),
            SyncRequestMessage(streamSessionId = "s1", positionMs = 2345),
        )
        assertTrue(
            encoded.contains("\"type\":\"sync.request\""),
            "sync.request frame must carry its type so the server can dispatch: $encoded",
        )
    }
}