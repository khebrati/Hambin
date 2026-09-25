package top.roomio.data.party

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import top.roomio.domain.party.RealtimeClient
import top.roomio.domain.party.RoomRepository
import top.roomio.domain.party.SessionKeeper
import top.roomio.domain.party.SessionRepository
import top.roomio.domain.party.VoiceClient

@BindingContainer
interface PartyDataBindings {
    @Binds
    fun bindSessionRepository(impl: KtorSessionRepository): SessionRepository

    @Binds
    fun bindRoomRepository(impl: KtorRoomRepository): RoomRepository

    @Binds
    fun bindRealtimeClient(impl: KtorRealtimeClient): RealtimeClient

    companion object {
        /** The deployed backend. Override here to point at another environment. */
        const val DEFAULT_BASE_URL = "https://api.roomio.audiosense.ir"

        @Provides
        @SingleIn(AppScope::class)
        fun provideBackendConfig(): BackendConfig = BackendConfig(DEFAULT_BASE_URL)

        @Provides
        @SingleIn(AppScope::class)
        fun provideVoiceClient(): VoiceClient = createVoiceClient()

        @Provides
        @SingleIn(AppScope::class)
        fun provideSessionKeeper(roomRepository: RoomRepository): SessionKeeper = createSessionKeeper(roomRepository)

        @Provides
        @SingleIn(AppScope::class)
        fun provideJson(): Json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

        @Provides
        @SingleIn(AppScope::class)
        fun provideHttpClient(tokens: TokenStore, json: Json): HttpClient = HttpClient {
            expectSuccess = false
            install(ContentNegotiation) { json(json) }
            install(WebSockets)
            install(Logging) {
                level = LogLevel.INFO
                sanitizeHeader { header -> header == HttpHeaders.Authorization }
            }
            defaultRequest {
                accept(ContentType.Application.Json)
                tokens.current()?.let { session ->
                    header(HttpHeaders.Authorization, "Bearer ${session.accessToken}")
                }
            }
        }
    }
}
