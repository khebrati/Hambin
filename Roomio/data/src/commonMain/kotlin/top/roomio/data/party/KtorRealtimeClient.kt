package top.roomio.data.party

import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.post
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import top.roomio.data.party.dto.PlaybackReportMessage
import top.roomio.data.party.dto.RealtimeTicketDto
import top.roomio.data.party.dto.SyncRequestMessage
import top.roomio.domain.party.PARTY_CODE_NETWORK
import top.roomio.domain.party.RealtimeClient
import top.roomio.domain.party.RealtimeEvent
import top.roomio.domain.party.RealtimeSession
import top.roomio.domain.party.SessionRepository

private val log = Logger.withTag("Realtime")

@SingleIn(AppScope::class)
@Inject
class KtorRealtimeClient(
    private val http: HttpClient,
    private val config: BackendConfig,
    private val json: Json,
    private val sessions: SessionRepository,
) : RealtimeClient {

    override suspend fun connect(roomId: String): RealtimeSession {
        log.i("connect room=$roomId base=${config.baseUrl}")
        val ticket = runCatching { issueTicket(roomId) }.getOrElse { error ->
            log.e("connect failed: ticket not issued room=$roomId error=${error.message ?: error::class.simpleName}")
            throw error
        }
        val wsUrl = config.baseUrl
            .replace("https://", "wss://")
            .replace("http://", "ws://")
            .trimEnd('/') + "/v1/rooms/$roomId/realtime?ticket=$ticket"
        log.i("connect opening ws=${maskTicket(wsUrl)}")
        return KtorRealtimeSession(http.webSocketSession(wsUrl), json)
    }

    private suspend fun issueTicket(roomId: String): String = try {
        val ticket = fetchTicket(roomId)
        log.i("ticket issued room=$roomId len=${ticket.length}")
        ticket
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Throwable) {
        val partyError = error.asPartyException()
        if (partyError.isAuthFailure()) {
            log.w("ticket auth failure room=$roomId code=${partyError.code}; refreshing session and retrying")
            if (runCatching { sessions.refresh() }.isSuccess) {
                val ticket = fetchTicket(roomId)
                log.i("ticket issued after refresh room=$roomId len=${ticket.length}")
                ticket
            } else {
                throw partyError
            }
        } else {
            if (partyError.code == PARTY_CODE_NETWORK) {
                log.e("ticket fetch failed before backend room=$roomId: ${error.message ?: error::class.simpleName}")
            }
            throw partyError
        }
    }

    private suspend fun fetchTicket(roomId: String): String {
        log.i("ticket fetch ${config.baseUrl}/v1/rooms/$roomId/realtime-ticket")
        return http.post("${config.baseUrl}/v1/rooms/$roomId/realtime-ticket")
            .bodyOrThrow<RealtimeTicketDto>()
            .ticket
    }
}

internal class KtorRealtimeSession(
    private val socket: DefaultClientWebSocketSession,
    private val json: Json,
) : RealtimeSession {

    override val events: Flow<RealtimeEvent> = flow {
        coroutineScope {
            // Keep the transport alive and detect half-open sockets. A dead
            // server (or a broken proxy path) never answers a ping, so if no
            // frame arrives within the timeout we close the socket and let the
            // caller reconnect instead of silently holding a stale connection.
            var lastReceive = TimeSource.Monotonic.markNow()
            val heartbeat = launch {
                while (isActive) {
                    delay(HEARTBEAT_INTERVAL_MS)
                    runCatching { socket.send(Frame.Text(PING_FRAME)) }
                        .onFailure { error ->
                            log.w("heartbeat ping failed: ${error.message ?: error::class.simpleName}")
                        }
                    if (lastReceive.elapsedNow() > HEARTBEAT_TIMEOUT) {
                        log.w("heartbeat timeout; closing transport")
                        runCatching {
                            socket.close(CloseReason(CloseReason.Codes.NORMAL, "heartbeat timeout"))
                        }
                        break
                    }
                }
            }
            try {
                for (frame in socket.incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.data.decodeToString()
                        log.d("ws< ${text.take(MAX_FRAME_LOG_CHARS)}")
                        lastReceive = TimeSource.Monotonic.markNow()
                        emit(parseRealtimeFrame(json, text))
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                // A dropped transport is an expected event, not a crash. Surface it
                // so the caller can reconnect and still show a stable room.
                log.e("ws transport failed: ${error.message ?: error::class.simpleName}")
                emit(RealtimeEvent.Failure("REALTIME_DISCONNECTED", error.message ?: "disconnected"))
            } finally {
                heartbeat.cancel()
            }
        }
        log.w("ws closed")
    }

    override suspend fun reportPlayback(streamSessionId: String, positionMs: Long, playing: Boolean) {
        val frame = json.encodeToString(
            PlaybackReportMessage(streamSessionId = streamSessionId, positionMs = positionMs, playing = playing),
        )
        log.d("report> $frame")
        socket.send(Frame.Text(frame))
    }

    override suspend fun requestSync(streamSessionId: String, positionMs: Long, requestId: String) {
        val frame = json.encodeToString(
            SyncRequestMessage(streamSessionId = streamSessionId, positionMs = positionMs, requestId = requestId),
        )
        log.i("sync.request> $frame")
        socket.send(Frame.Text(frame))
    }

    override suspend fun close() {
        log.i("close called")
        socket.close(CloseReason(CloseReason.Codes.NORMAL, "client closing"))
    }
}

/** Logs the WebSocket URL without revealing the single-use ticket credential. */
private fun maskTicket(url: String): String {
    val separator = "?ticket="
    val index = url.indexOf(separator)
    return if (index >= 0) url.substring(0, index) + separator + "<redacted>" else url
}

private const val MAX_FRAME_LOG_CHARS = 200

/** How often the client solicits a server pong to prove the transport is alive. */
private const val HEARTBEAT_INTERVAL_MS = 10_000L

/** Close the socket when no server frame (for example a pong) arrives within this window. */
private val HEARTBEAT_TIMEOUT = 30.seconds

/** Minimal application-level ping; the server replies with `{"type":"pong"}`. */
private const val PING_FRAME = """{"type":"ping"}"""
