package top.roomio.data.party

import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.encodeToJsonElement
import kotlin.random.Random
import top.roomio.data.party.dto.PlaybackReportMessage
import top.roomio.data.party.dto.RealtimeTicketDto
import top.roomio.data.party.dto.SyncRequestMessage
import top.roomio.domain.party.PARTY_CODE_NETWORK
import top.roomio.domain.party.PartyException
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
    private val tokens: TokenStore,
) : RealtimeClient {

    override suspend fun connect(roomId: String): RealtimeSession {
        val initial = openRoomNamespace(roomId)
        return KtorRealtimeSession(
            initialNamespace = initial,
            reconnect = { openRoomNamespace(roomId) },
            sessions = sessions,
            tokens = tokens,
            json = json,
        )
    }

    private suspend fun openRoomNamespace(roomId: String): SocketIoNamespace {
        val ticket = issueTicket(roomId)
        val expected = "/hambin/rooms/$roomId"
        if (ticket.namespace != expected) throw PartyException("INVALID_TICKET", "Backend returned an unexpected room namespace")
        val socket = http.webSocketSession(socketIoUrl(config.baseUrl))
        val connection = SocketIoConnection(socket, json)
        return try {
            connection.awaitOpen()
            connection.connectNamespace(ticket.namespace, ticket.ticket)
        } catch (error: Throwable) {
            connection.close()
            throw error
        }
    }

    private suspend fun issueTicket(roomId: String): RealtimeTicketDto = try {
        fetchTicket(roomId)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Throwable) {
        val partyError = error.asPartyException()
        if (!partyError.isAuthFailure()) throw partyError
        sessions.refresh() ?: throw partyError
        fetchTicket(roomId)
    }

    private suspend fun fetchTicket(roomId: String): RealtimeTicketDto = http.post(
        "${config.baseUrl}/v1/rooms/$roomId/realtime-ticket",
    ).bodyOrThrow()

}

private class KtorRealtimeSession(
    initialNamespace: SocketIoNamespace,
    private val reconnect: suspend () -> SocketIoNamespace,
    private val sessions: SessionRepository,
    private val tokens: TokenStore,
    private val json: Json,
) : RealtimeSession {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val output = Channel<RealtimeEvent>(capacity = 64)
    private val namespaceMutex = Mutex()
    private var namespace = initialNamespace
    private val eventGate = SnapshotEventGate(json)
    private var closed = false
    override val events: Flow<RealtimeEvent> = output.receiveAsFlow()

    init {
        scope.launch { pump() }
        scope.launch { refreshRealtimeAuth() }
    }

    override suspend fun reportPlayback(streamSessionId: String, positionMs: Long, playing: Boolean) {
        emitCommand("playback.report", json.encodeToJsonElement(PlaybackReportMessage.serializer(),
            PlaybackReportMessage(streamSessionId = streamSessionId, positionMs = positionMs, playing = playing),
        ))
    }

    override suspend fun requestSync(streamSessionId: String, positionMs: Long, requestId: String) {
        emitCommand("sync.request", json.encodeToJsonElement(SyncRequestMessage.serializer(),
            SyncRequestMessage(streamSessionId = streamSessionId, positionMs = positionMs, requestId = requestId),
        ))
    }

    override suspend fun close() {
        closed = true
        scope.cancel()
        namespaceMutex.withLock { runCatching { namespace.close() } }
        output.close()
    }

    private suspend fun emitCommand(name: String, payload: JsonElement) {
        val ack = namespaceMutex.withLock { namespace.emit(name, payload) }
        val obj = ack as? JsonObject ?: return
        val code = obj["code"]?.jsonPrimitive?.contentOrNull ?: return
        val message = obj["message"]?.jsonPrimitive?.contentOrNull ?: "Realtime command failed"
        throw PartyException(code, message)
    }

    private suspend fun pump() {
        var attempts = 0
        while (scope.isActive && !closed) {
            try {
                val current = namespaceMutex.withLock { namespace }
                for (incoming in current.events) {
                    if (incoming.name == "snapshot") attempts = 0
                    for (event in eventGate.accept(incoming)) output.send(event)
                }
                if (!closed) throw IllegalStateException("Socket.IO namespace ended")
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                if (closed) break
                val failure = error.asPartyException()
                output.trySend(RealtimeEvent.Failure(failure.code.ifBlank { PARTY_CODE_NETWORK }, failure.message.orEmpty()))
                runCatching { namespaceMutex.withLock { namespace.close() } }
                attempts = (attempts + 1).coerceAtMost(MAX_RECONNECT_ATTEMPTS)
                val base = (RECONNECT_BASE_MS shl (attempts - 1)).coerceAtMost(RECONNECT_MAX_MS)
                delay(base + Random.nextLong(0, (base / 4).coerceAtLeast(1)))
                try {
                    val replacement = reconnect()
                    namespaceMutex.withLock { namespace = replacement }
                } catch (reconnectError: CancellationException) {
                    throw reconnectError
                } catch (reconnectError: Throwable) {
                    output.trySend(RealtimeEvent.Failure(reconnectError.asPartyException().code, reconnectError.message.orEmpty()))
                }
            }
        }
        output.close()
    }

    private suspend fun refreshRealtimeAuth() {
        while (scope.isActive && !closed) {
            val credentials = tokens.credentials() ?: return
            val waitMs = (credentials.expiresAtEpochMs - nowMs() - AUTH_REFRESH_EARLY_MS).coerceAtLeast(1_000L)
            delay(waitMs)
            try {
                sessions.refresh()
                val fresh = tokens.credentials() ?: return
                val ack = namespaceMutex.withLock {
                    namespace.emit("auth.refresh", buildJsonObject { put("idToken", JsonPrimitive(fresh.idToken)) })
                }
                val obj = ack as? JsonObject
                val code = obj?.get("code")?.jsonPrimitive?.contentOrNull
                if (code != null) throw PartyException(code, "Realtime authentication refresh was rejected")
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                val partyError = error.asPartyException()
                if (partyError.code != PARTY_CODE_NETWORK) {
                    output.trySend(RealtimeEvent.Failure(partyError.code, partyError.message.orEmpty()))
                }
                delay(AUTH_RETRY_MS)
            }
        }
    }

    private companion object {
        const val AUTH_REFRESH_EARLY_MS = 5 * 60 * 1000L
        const val AUTH_RETRY_MS = 30_000L
        const val RECONNECT_BASE_MS = 500L
        const val RECONNECT_MAX_MS = 30_000L
        const val MAX_RECONNECT_ATTEMPTS = 7
    }
}

private fun socketIoUrl(baseUrl: String): String = when {
    baseUrl.startsWith("https://") -> "wss://${baseUrl.removePrefix("https://").trimEnd('/')}/socket.io/?EIO=4&transport=websocket"
    baseUrl.startsWith("http://") -> "ws://${baseUrl.removePrefix("http://").trimEnd('/')}/socket.io/?EIO=4&transport=websocket"
    else -> throw IllegalArgumentException("Backend URL must use HTTP or HTTPS")
}

private fun nowMs(): Long = kotlin.time.Clock.System.now().toEpochMilliseconds()
