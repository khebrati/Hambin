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
import top.roomio.data.party.dto.VoiceTokenDto
import top.roomio.data.party.dto.toDomain
import top.roomio.domain.party.PARTY_CODE_NETWORK
import top.roomio.domain.party.PartyException
import top.roomio.domain.party.Room
import top.roomio.domain.party.RoomPreview
import top.roomio.domain.party.RoomRepository
import top.roomio.domain.party.RoomSnapshot
import top.roomio.domain.party.SessionRepository
import top.roomio.domain.party.StreamInfo
import top.roomio.domain.party.VoiceToken

@SingleIn(AppScope::class)
@Inject
class KtorRoomRepository(
    private val http: HttpClient,
    private val config: BackendConfig,
    private val sessions: SessionRepository,
) : RoomRepository {

    override suspend fun createRoom(title: String): Room = authed {
        http.post("${config.baseUrl}/v1/rooms") {
            contentType(ContentType.Application.Json)
            setBody(CreateRoomRequest(title = title))
        }.bodyOrThrow<RoomAndMembershipDto>().room.toDomain()
    }

    override suspend fun preview(code: String): RoomPreview = authed {
        http.get("${config.baseUrl}/v1/rooms/$code/preview").bodyOrThrow<RoomPreviewDto>().toDomain()
    }

    override suspend fun join(code: String): Room = authed {
        http.post("${config.baseUrl}/v1/rooms/$code/join").bodyOrThrow<RoomAndMembershipDto>().room.toDomain()
    }

    override suspend fun snapshot(roomId: String): RoomSnapshot = authed {
        http.get("${config.baseUrl}/v1/rooms/$roomId/snapshot").bodyOrThrow<SnapshotDto>().toDomain()
    }

    override suspend fun leave(roomId: String): Boolean = authed {
        http.post("${config.baseUrl}/v1/rooms/$roomId/leave").bodyOrThrow<LeaveResponseDto>().roomClosed
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

    override suspend fun voiceToken(roomId: String): VoiceToken = authed {
        val dto = http.post("${config.baseUrl}/v1/rooms/$roomId/voice/session-token")
            .bodyOrThrow<VoiceTokenDto>()
        VoiceToken(token = dto.token, url = dto.url)
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
            log.e("request failed before backend: ${error.message ?: error::class.simpleName}")
            throw partyError
        } else {
            throw partyError
        }
    }

    private companion object {
        val log = Logger.withTag("RoomRepo")
    }
}
