package top.roomio.data.party

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.URLBuilder
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import top.roomio.domain.party.PartyException

class KtorSessionRepositoryTest {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
    private val responseHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    private val config = FirebaseAuthConfig("demo-hambin", "local-test-key", "127.0.0.1:9099")

    @Test
    fun authEmulatorUsesLocalKeyAndOfficialEndpointPaths() {
        assertEquals(
            "http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1/accounts:signUp?key=localkey",
            config.signUpUrl,
        )
        assertEquals(
            "http://127.0.0.1:9099/securetoken.googleapis.com/v1/token?key=localkey",
            config.refreshUrl,
        )
        val production = FirebaseAuthConfig("hambin-dab14", "public-key")
        assertTrue(production.signUpUrl.startsWith("https://identitytoolkit.googleapis.com/v1/accounts:signUp"))
        assertTrue(production.refreshUrl.startsWith("https://securetoken.googleapis.com/v1/token"))
    }

    @Test
    fun backendBearerIsAttachedOnlyAfterResolvingTheRequestHost() = runTest {
        val tokens = tokenStore(expired = false)
        val authorizationByPath = mutableMapOf<String, String?>()
        val backendConfig = BackendConfig("http://10.0.2.2:8081", "demo-hambin", "local-test-key")
        val client = HttpClient(MockEngine { request ->
            authorizationByPath[request.url.encodedPath] = request.headers[HttpHeaders.Authorization]
            respond("{}", HttpStatusCode.OK, responseHeaders)
        }) {
            expectSuccess = false
            install(backendAuthorizationPlugin(tokens, backendConfig))
        }

        client.get("http://10.0.2.2:8081/v1/probe")
        client.get("http://10.0.2.2:9099/identitytoolkit.googleapis.com/v1/accounts:signUp") {
            header(HttpHeaders.Authorization, "Bearer should-be-removed")
        }
        client.get("http://10.0.2.2:8081/socket.io/?EIO=4&transport=websocket") {
            header(HttpHeaders.Authorization, "Bearer should-be-removed")
        }

        assertEquals("Bearer old-id", authorizationByPath["/v1/probe"])
        assertNull(authorizationByPath["/identitytoolkit.googleapis.com/v1/accounts:signUp"])
        assertNull(authorizationByPath["/socket.io/"])
        client.close()
    }

    @Test
    fun firebaseCredentialsAreSavedBeforeBootstrapAndReusedAfterBackendOutage() = runTest {
        val settings = MapSettings()
        val tokens = TokenStore(settings, json)
        var signUps = 0
        var bootstraps = 0
        val http = http(MockEngine { request ->
            when {
                request.url.host == "127.0.0.1" && request.url.encodedPath.endsWith("/accounts:signUp") -> {
                    signUps++
                    respond(
                        """{"localId":"firebase-uid-1","idToken":"id-token-1","refreshToken":"refresh-1","expiresIn":"3600"}""",
                        HttpStatusCode.OK,
                        responseHeaders,
                    )
                }
                request.url.encodedPath == "/v1/identities/bootstrap" -> {
                    bootstraps++
                    if (bootstraps == 1) {
                        assertNotNull(tokens.credentials(), "Firebase UID must be durable before bootstrap")
                        respond("{}", HttpStatusCode.ServiceUnavailable, responseHeaders)
                    } else {
                        respond(identityJson("identity-1"), HttpStatusCode.OK, responseHeaders)
                    }
                }
                else -> respond("{}", HttpStatusCode.NotFound, responseHeaders)
            }
        })
        val repository = repository(http, tokens)

        val failure = runCatching { repository.ensureSession("Nika", "COMET", "en") }.exceptionOrNull()
        assertEquals("NETWORK", (failure as PartyException).code)
        assertEquals("firebase-uid-1", tokens.credentials()?.uid)
        assertNull(tokens.current(), "profile remains pending until bootstrap succeeds")

        val session = repository.ensureSession("Nika", "COMET", "en")
        assertEquals("identity-1", session.identity.id)
        assertEquals("firebase-uid-1", tokens.credentials()?.uid)
        assertEquals(1, signUps, "a bootstrap outage must never create a replacement Firebase identity")
        assertEquals(2, bootstraps)
        assertEquals(1, settings.keys.count { it == "party.firebaseSession.v1" })
        http.close()
    }

    @Test
    fun expiredCredentialRefreshIsSerializedIntoTheSameIdentity() = runTest {
        val tokens = tokenStore(expired = true)
        var refreshBody = ""
        val http = http(MockEngine { request ->
            when {
                request.url.encodedPath == "/securetoken.googleapis.com/v1/token" -> {
                    refreshBody = (request.body as io.ktor.http.content.TextContent).text
                    respond(
                        """{"id_token":"fresh-id","refresh_token":"refresh-2","expires_in":"3600","user_id":"firebase-uid-1","project_id":"demo-hambin"}""",
                        HttpStatusCode.OK,
                        responseHeaders,
                    )
                }
                request.url.encodedPath == "/v1/identities/bootstrap" ->
                    respond(identityJson("identity-1"), HttpStatusCode.OK, responseHeaders)
                else -> respond("{}", HttpStatusCode.NotFound, responseHeaders)
            }
        })

        val session = repository(http, tokens).ensureSession("Nika", "COMET", "en")

        assertEquals("identity-1", session.identity.id)
        assertEquals("fresh-id", tokens.credentials()?.idToken)
        assertEquals("refresh-2", tokens.credentials()?.refreshToken)
        assertEquals("grant_type=refresh_token&refresh_token=refresh-old", refreshBody)
        http.close()
    }

    @Test
    fun invalidRefreshKeepsStoredUidAndTokensForExplicitRecovery() = runTest {
        val tokens = tokenStore(expired = true)
        val http = http(MockEngine { request ->
            respond(
                """{"error":{"message":"INVALID_REFRESH_TOKEN"}}""",
                HttpStatusCode.BadRequest,
                responseHeaders,
            )
        })

        val failure = runCatching { repository(http, tokens).refresh() }.exceptionOrNull()

        assertEquals("SESSION_REVOKED", (failure as PartyException).code)
        assertEquals("firebase-uid-1", tokens.credentials()?.uid)
        assertEquals("refresh-old", tokens.credentials()?.refreshToken)
        http.close()
    }

    private fun repository(http: HttpClient, tokens: TokenStore) = KtorSessionRepository(
        http = http,
        config = BackendConfig("http://backend.test:8081", "demo-hambin", "local-test-key", "127.0.0.1:9099"),
        auth = FirebaseAuthClient(http, config, json),
        tokens = tokens,
    )

    private fun tokenStore(expired: Boolean): TokenStore = TokenStore(MapSettings(), json).apply {
        saveCredentials(
            FirebaseCredentials(
                uid = "firebase-uid-1",
                idToken = "old-id",
                refreshToken = "refresh-old",
                expiresAtEpochMs = if (expired) 0 else Long.MAX_VALUE,
                projectId = "demo-hambin",
            ),
            "Nika",
            "COMET",
            "en",
        )
    }

    private fun http(engine: MockEngine) = HttpClient(engine) {
        install(ContentNegotiation) { json(json) }
        expectSuccess = false
    }

    private fun identityJson(id: String) =
        """{"identity":{"id":"$id","name":"Nika","avatar":"COMET","language":"en"}}"""
}
