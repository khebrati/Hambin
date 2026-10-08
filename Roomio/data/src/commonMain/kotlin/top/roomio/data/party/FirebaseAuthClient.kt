package top.roomio.data.party

import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.http.formUrlEncode
import io.ktor.http.parameters
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlin.time.Clock
import top.roomio.domain.party.PARTY_CODE_NETWORK
import top.roomio.domain.party.PartyException

/** Firebase Auth REST adapter shared by Android, iOS, and desktop. */
@SingleIn(AppScope::class)
@Inject
class FirebaseAuthClient(
    private val http: HttpClient,
    private val config: FirebaseAuthConfig,
    private val json: Json,
) {
    private val log = Logger.withTag("FirebaseAuth")

    init {
        log.i("Auth REST configured projectId=${config.projectId} emulator=${config.emulatorHost ?: "disabled"}")
    }

    suspend fun signUpAnonymously(): FirebaseCredentials {
        val response = http.post(config.signUpUrl) {
            contentType(ContentType.Application.Json)
            setBody(SignUpRequest(returnSecureToken = true))
        }
        val payload = response.bodyAsText()
        if (!response.status.isSuccess()) {
            val error = firebaseError(payload, response.status.value)
            log.w("Auth REST endpoint=accounts:signUp status=${response.status.value} code=${error.code}")
            throw error
        }
        val result = json.decodeFromString<SignUpResponse>(payload)
        if (result.localId.isBlank() || result.idToken.isBlank() || result.refreshToken.isBlank()) {
            log.w("Auth REST endpoint=accounts:signUp status=${response.status.value} code=INVALID_TOKEN")
            throw PartyException("INVALID_TOKEN", "Firebase returned an incomplete anonymous session")
        }
        log.i("Auth REST endpoint=accounts:signUp status=${response.status.value} result=success projectId=${config.projectId}")
        return FirebaseCredentials(
            uid = result.localId,
            idToken = result.idToken,
            refreshToken = result.refreshToken,
            expiresAtEpochMs = Clock.System.now().toEpochMilliseconds() + result.expiresIn.toLongOrNull().orZero() * 1000L,
            projectId = config.projectId,
        )
    }

    suspend fun refresh(current: FirebaseCredentials): FirebaseCredentials {
        val response = http.post(config.refreshUrl) {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(
                parameters {
                    append("grant_type", "refresh_token")
                    append("refresh_token", current.refreshToken)
                }.formUrlEncode(),
            )
        }
        val payload = response.bodyAsText()
        if (!response.status.isSuccess()) {
            val error = firebaseError(payload, response.status.value)
            log.w("Auth REST endpoint=token:refresh status=${response.status.value} code=${error.code}")
            throw error
        }
        val result = json.decodeFromString<RefreshResponse>(payload)
        if (result.userId != current.uid || (result.projectId != null && result.projectId != config.projectId)) {
            throw PartyException("INVALID_TOKEN", "Firebase refresh returned a different identity")
        }
        if (result.idToken.isBlank() || result.refreshToken.isBlank()) {
            log.w("Auth REST endpoint=token:refresh status=${response.status.value} code=INVALID_TOKEN")
            throw PartyException("INVALID_TOKEN", "Firebase returned an incomplete refreshed session")
        }
        log.i("Auth REST endpoint=token:refresh status=${response.status.value} result=success projectId=${config.projectId}")
        return FirebaseCredentials(
            uid = current.uid,
            idToken = result.idToken,
            refreshToken = result.refreshToken,
            expiresAtEpochMs = Clock.System.now().toEpochMilliseconds() + result.expiresIn.toLongOrNull().orZero() * 1000L,
            projectId = config.projectId,
        )
    }

    private fun firebaseError(body: String, status: Int): PartyException {
        val message = runCatching {
            json.parseToJsonElement(body).jsonObject["error"]!!.jsonObject["message"]!!.jsonPrimitive.content
        }.getOrNull().orEmpty()
        val code = when {
            status >= 500 || message.contains("TOO_MANY_ATTEMPTS", ignoreCase = true) -> PARTY_CODE_NETWORK
            message.contains("INVALID_REFRESH_TOKEN", ignoreCase = true) || message.contains("USER_DISABLED", ignoreCase = true) -> "SESSION_REVOKED"
            message.contains("TOKEN_EXPIRED", ignoreCase = true) -> "SESSION_EXPIRED"
            else -> "INVALID_TOKEN"
        }
        return PartyException(code, if (code == PARTY_CODE_NETWORK) "Firebase authentication is temporarily unavailable" else "Firebase authentication failed")
    }
}

@Serializable
private data class SignUpRequest(val returnSecureToken: Boolean)

@Serializable
private data class SignUpResponse(
    val localId: String,
    val idToken: String,
    val refreshToken: String,
    val expiresIn: String,
)

@Serializable
private data class RefreshResponse(
    val id_token: String,
    val refresh_token: String,
    val expires_in: String,
    val user_id: String,
    val project_id: String? = null,
) {
    val idToken get() = id_token
    val refreshToken get() = refresh_token
    val expiresIn get() = expires_in
    val userId get() = user_id
    val projectId get() = project_id
}

private fun Long?.orZero(): Long = this ?: 0L
