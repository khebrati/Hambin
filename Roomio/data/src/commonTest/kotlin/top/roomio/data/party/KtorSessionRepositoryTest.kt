package top.roomio.data.party

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import top.roomio.domain.party.GuestIdentity
import top.roomio.domain.party.GuestSession

class KtorSessionRepositoryTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    @Test
    fun updateProfileRefreshesExpiredAccessTokenAndRetries() = runTest {
        val tokens = tokenStore()
        var refreshes = 0
        val engine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/v1/guest-sessions/refresh" -> {
                    refreshes++
                    respond(
                        content = sessionJson(access = "fresh", refresh = "refresh-2", avatar = "ECHO"),
                        status = HttpStatusCode.OK,
                        headers = jsonHeaders,
                    )
                }
                "/v1/guest-sessions/profile" -> {
                    if (request.headers[HttpHeaders.Authorization] == "Bearer fresh") {
                        respond(
                            content = """{"identity":{"id":"id-1","name":"erfan","avatar":"NOVA","language":"en"}}""",
                            status = HttpStatusCode.OK,
                            headers = jsonHeaders,
                        )
                    } else {
                        respond(
                            content = """{"code":"INVALID_TOKEN","message":"invalid or expired token"}""",
                            status = HttpStatusCode.Unauthorized,
                            headers = jsonHeaders,
                        )
                    }
                }
                else -> respond("{}", HttpStatusCode.NotFound, jsonHeaders)
            }
        }
        val repository = repository(tokens, engine)

        val identity = repository.updateProfile("erfan", "NOVA", "en")

        assertEquals("NOVA", identity.avatar)
        assertEquals(1, refreshes)
        assertEquals("fresh", tokens.current()?.accessToken)
        assertEquals("refresh-2", tokens.current()?.refreshToken)
    }

    @Test
    fun ensureSessionRegistersNewIdentityWhenSessionIsUnrecoverable() = runTest {
        val tokens = tokenStore()
        var profileAttempts = 0
        val engine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/v1/guest-sessions/profile" -> {
                    profileAttempts++
                    respond(
                        content = """{"code":"INVALID_TOKEN","message":"invalid or expired token"}""",
                        status = HttpStatusCode.Unauthorized,
                        headers = jsonHeaders,
                    )
                }
                "/v1/guest-sessions/refresh" -> respond(
                    content = """{"code":"SESSION_REVOKED","message":"refresh session revoked"}""",
                    status = HttpStatusCode.Unauthorized,
                    headers = jsonHeaders,
                )
                "/v1/guest-sessions" -> respond(
                    content = sessionJson(access = "new-access", refresh = "new-refresh", id = "id-2", avatar = "NOVA"),
                    status = HttpStatusCode.Created,
                    headers = jsonHeaders,
                )
                else -> respond("{}", HttpStatusCode.NotFound, jsonHeaders)
            }
        }
        val repository = repository(tokens, engine)

        val session = repository.ensureSession("erfan", "NOVA", "en")

        assertEquals("id-2", session.identity.id)
        assertEquals("NOVA", session.identity.avatar)
        assertEquals("new-access", tokens.current()?.accessToken)
        assertEquals(1, profileAttempts)
    }

    private fun tokenStore(): TokenStore = TokenStore(MapSettings()).apply {
        save(
            GuestSession(
                identity = GuestIdentity("id-1", "erfan", "ECHO", "en"),
                accessToken = "expired",
                refreshToken = "refresh-1",
            ),
        )
    }

    private fun repository(tokens: TokenStore, engine: MockEngine): KtorSessionRepository {
        val http = HttpClient(engine) {
            install(ContentNegotiation) { json(json) }
            defaultRequest {
                tokens.current()?.let { header(HttpHeaders.Authorization, "Bearer ${it.accessToken}") }
            }
        }
        return KtorSessionRepository(http, BackendConfig("https://example.test"), tokens)
    }

    private fun sessionJson(
        access: String,
        refresh: String,
        id: String = "id-1",
        avatar: String = "ECHO",
    ): String = """
        {"identity":{"id":"$id","name":"erfan","avatar":"$avatar","language":"en"},
         "accessToken":"$access","refreshToken":"$refresh"}
    """.trimIndent()
}
