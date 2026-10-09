package top.roomio.data.party

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.accept
import io.ktor.http.URLBuilder
import io.ktor.http.Url
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
        fun provideBackendConfig(): BackendConfig {
            val overrides = platformBackendOverrides()
            return BackendConfig(
                baseUrl = overrides.backendUrl ?: DEFAULT_BASE_URL,
                firebaseProjectId = overrides.firebaseProjectId ?: DEFAULT_FIREBASE_PROJECT_ID,
                firebaseAuthEmulatorHost = overrides.authEmulatorHost,
            )
        }

        @Provides
        @SingleIn(AppScope::class)
        fun provideFirebaseAuthConfig(config: BackendConfig): FirebaseAuthConfig = FirebaseAuthConfig(
            projectId = config.firebaseProjectId,
            apiKey = config.firebaseApiKey,
            emulatorHost = config.firebaseAuthEmulatorHost,
        )

        @Provides
        @SingleIn(AppScope::class)
        fun provideVoiceClient(
            http: HttpClient,
            config: BackendConfig,
            json: Json,
            sessions: SessionRepository,
            tokens: TokenStore,
        ): VoiceClient = createVoiceClient(http, config, json, sessions, tokens)

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
        fun provideHttpClient(tokens: TokenStore, json: Json, config: BackendConfig): HttpClient = HttpClient {
            expectSuccess = false
            install(ContentNegotiation) { json(json) }
            install(WebSockets)
            install(Logging) {
                level = LogLevel.INFO
                sanitizeHeader { header -> header == HttpHeaders.Authorization }
                filter { request -> shouldLogHttpRequest(request.url.build()) }
            }
            install(backendAuthorizationPlugin(tokens, config))
            defaultRequest {
                accept(ContentType.Application.Json)
            }
        }

    }
}

/** Applies credentials after Ktor has resolved each request's final URL. */
internal fun backendAuthorizationPlugin(tokens: TokenStore, config: BackendConfig) =
    createClientPlugin("BackendAuthorization") {
        val log = Logger.withTag("BackendAuth")
        onRequest { request, _ ->
            val requestUrl = request.url.build()
            val path = requestUrl.encodedPath
            val isRealtimeHandshake = path.endsWith("/socket.io/") || path.endsWith("/socket.io")
            val isBackendRequest = !isRealtimeHandshake && hasBackendOrigin(requestUrl, config.baseUrl)
            if (isBackendRequest) {
                tokens.credentials()?.let { credentials ->
                    request.headers[HttpHeaders.Authorization] = "Bearer ${credentials.idToken}"
                }
            } else {
                request.headers.remove(HttpHeaders.Authorization)
            }
            if (path == "/v1/identities/bootstrap") {
                log.i("bootstrap auth scope host=${requestUrl.host}:${requestUrl.port} backendMatch=$isBackendRequest firebaseCredentialsPresent=${tokens.credentials() != null}")
            }
        }
    }

internal fun hasBackendOrigin(request: Url, baseUrl: String): Boolean {
    val backend = URLBuilder(baseUrl).build()
    return request.host.equals(backend.host, ignoreCase = true) &&
        request.port == backend.port && request.protocol == backend.protocol
}

/** Firebase REST URLs carry the public API key in the query string; exclude their full exchange. */
internal fun shouldLogHttpRequest(request: Url): Boolean {
    val host = request.host.lowercase()
    val path = request.encodedPath.lowercase()
    val firebaseAuthHost = host == "identitytoolkit.googleapis.com" ||
        host == "securetoken.googleapis.com"
    val firebaseAuthEmulatorPath = path.contains("/identitytoolkit.googleapis.com/v1/accounts:") ||
        path.contains("/securetoken.googleapis.com/v1/token")
    return !firebaseAuthHost && !firebaseAuthEmulatorPath
}
