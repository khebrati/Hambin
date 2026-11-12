package top.roomio.data.party

import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import top.roomio.data.party.dto.CreateRoomRequest
import top.roomio.data.party.dto.LeaveResponseDto
import top.roomio.data.party.dto.RoomAndMembershipDto
import top.roomio.data.party.dto.RoomPreviewDto
import top.roomio.data.party.dto.SnapshotDto
import top.roomio.data.party.dto.StartStreamRequest
import top.roomio.data.party.dto.StreamResponseDto
import top.roomio.data.party.dto.VoiceConfigurationDto
import top.roomio.data.party.dto.VoiceConfigurationRequest
import top.roomio.data.party.dto.toDomain
import top.roomio.domain.party.PARTY_CODE_NETWORK
import top.roomio.domain.party.PartyException
import top.roomio.domain.party.Room
import top.roomio.domain.party.RoomPreview
import top.roomio.domain.party.RoomRepository
import top.roomio.domain.party.RoomSnapshot
import top.roomio.domain.party.SessionRepository
import top.roomio.domain.party.StreamInfo
import top.roomio.domain.party.VoiceConfiguration

@SingleIn(AppScope::class)
@Inject
class KtorRoomRepository(
    private val http: HttpClient,
    private val config: BackendConfig,
    private val sessions: SessionRepository,
    private val lastRooms: LastRoomStore,
) : RoomRepository {
    @kotlinx.serialization.Serializable
    private data class BugReportRequest(val reportId: String, val description: String, val clientInfo: Map<String, String>, val sessionStartedAt: String, val log: String)
    private var pendingReport: Pair<String, BugReportRequest>? = null

    override suspend fun reportBug(roomId: String, description: String) {
        val text = description.trim()
        val previous = pendingReport?.takeIf { it.first == roomId && it.second.description == text }?.second
        log.i("report.send retry=${previous != null}")
        val payload = previous ?: BugReportRequest(newReportId(), text, reportDeviceInfo(), AppSessionLog.startedAt, AppSessionLog.snapshot())
        pendingReport = roomId to payload
        val sent = kotlinx.coroutines.withTimeoutOrNull(30_000) {
            authed {
                http.post("${config.baseUrl}/v1/rooms/$roomId/bug-reports") {
                    contentType(ContentType.Application.Json)
                    setBody(payload)
                }.bodyOrThrow<kotlinx.serialization.json.JsonObject>()
            }
            true
        } ?: false
        if (!sent) throw PartyException(PARTY_CODE_NETWORK, "Report request timed out")
        log.i("report.accepted")
        pendingReport = null
    }

    override suspend fun lastRoom(): Room? = sessions.currentSession()?.identity?.id?.let {
        lastRooms.read(config.baseUrl, it)
    }

    private suspend fun remember(room: Room): Room {
        sessions.currentSession()?.identity?.id?.let { lastRooms.remember(config.baseUrl, it, room) }
        return room
    }

    private suspend fun forget(codeOrId: String) {
        sessions.currentSession()?.identity?.id?.let { lastRooms.forget(config.baseUrl, it, codeOrId) }
    }

    override suspend fun createRoom(title: String): Room = authed {
        http.post("${config.baseUrl}/v1/rooms") {
            contentType(ContentType.Application.Json)
            setBody(CreateRoomRequest(title = title))
        }.bodyOrThrow<RoomAndMembershipDto>().room.toDomain().let { remember(it) }
    }

    override suspend fun preview(code: String): RoomPreview = authed {
        try {
            http.get("${config.baseUrl}/v1/rooms/$code/preview").bodyOrThrow<RoomPreviewDto>().toDomain().also {
                if (it.availability == top.roomio.domain.party.RoomAvailability.ENDED) forget(code)
            }
        } catch (error: PartyException) {
            if (error.code in setOf("ROOM_ENDED", "ROOM_NOT_FOUND")) forget(code)
            throw error
        }
    }

    override suspend fun join(code: String): Room = authed {
        try {
            http.post("${config.baseUrl}/v1/rooms/$code/join").bodyOrThrow<RoomAndMembershipDto>().room.toDomain().let { remember(it) }
        } catch (error: PartyException) {
            if (error.code in setOf("ROOM_ENDED", "ROOM_NOT_FOUND")) forget(code)
            throw error
        }
    }

    override suspend fun snapshot(roomId: String): RoomSnapshot = authed {
        http.get("${config.baseUrl}/v1/rooms/$roomId/snapshot").bodyOrThrow<SnapshotDto>().toDomain()
    }

    override suspend fun leave(roomId: String): Boolean = authed {
        http.post("${config.baseUrl}/v1/rooms/$roomId/leave").bodyOrThrow<LeaveResponseDto>().roomClosed.also { if (it) forget(roomId) }
    }

    override suspend fun startStream(roomId: String, url: String): StreamInfo = authed {
        http.post("${config.baseUrl}/v1/rooms/$roomId/stream/start") {
            contentType(ContentType.Application.Json)
            setBody(StartStreamRequest(url = url))
        }.bodyOrThrow<StreamResponseDto>().stream.toDomain()
    }

    override suspend fun abortStream(roomId: String): StreamInfo = authed {
        http.post("${config.baseUrl}/v1/rooms/$roomId/stream/abort")
            .bodyOrThrow<StreamResponseDto>().stream.toDomain()
    }

    override suspend fun voiceConfiguration(roomId: String, voiceSessionId: String?): VoiceConfiguration = authed {
        val dto = http.post("${config.baseUrl}/v1/rooms/$roomId/voice/config") {
            voiceSessionId?.let { contentType(ContentType.Application.Json); setBody(VoiceConfigurationRequest(it, "resume")) }
        }.bodyOrThrow<VoiceConfigurationDto>()
        dto.toDomain()
    }

    private suspend fun <T> authed(block: suspend () -> T): T = try {
        block()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Throwable) {
        val partyError = error.asPartyException()
        if (partyError.isAuthFailure()) {
            // A token refresh can itself fail while offline; surface the original
            // auth error instead of crashing.
            log.e("request auth failure code=${partyError.code}; refreshing session")
            if (runCatching { sessions.refresh() }.isSuccess) {
                block()
            } else {
                throw partyError
            }
        } else if (partyError.code == PARTY_CODE_NETWORK) {
            log.e("request failed before backend code=NETWORK")
            throw partyError
        } else {
            throw partyError
        }
    }

    private companion object {
        val log = Logger.withTag("RoomRepo")
    }
}
