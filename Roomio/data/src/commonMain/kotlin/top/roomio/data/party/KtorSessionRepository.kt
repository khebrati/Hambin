package top.roomio.data.party

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import top.roomio.data.party.dto.GuestSessionRequest
import top.roomio.data.party.dto.ProfileResponseDto
import top.roomio.data.party.dto.RefreshRequest
import top.roomio.data.party.dto.SessionDto
import top.roomio.data.party.dto.toDomain
import top.roomio.domain.party.GuestIdentity
import top.roomio.domain.party.GuestSession
import top.roomio.domain.party.PartyException
import top.roomio.domain.party.SessionRepository

@SingleIn(AppScope::class)
@Inject
class KtorSessionRepository(
    private val http: HttpClient,
    private val config: BackendConfig,
    private val tokens: TokenStore,
) : SessionRepository {

    override suspend fun ensureSession(name: String, avatar: String, language: String): GuestSession =
        normalized {
            val current = tokens.current()
            if (current != null) {
                val identity = current.identity
                if (identity.name == name && identity.avatar == avatar && identity.language == language) {
                    return@normalized current
                }
                // The saved profile changed after the identity was created; push
                // the new name/avatar to the backend so room membership stays
                // current. An expired access token is refreshed transparently.
                // If the whole session is unrecoverable the store is cleared, so
                // register a fresh identity instead of staying stuck offline.
                val updated = try {
                    updateProfile(name, avatar, language)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (error: Throwable) {
                    if (tokens.current() != null) throw error
                    null
                }
                if (updated != null) {
                    return@normalized current.copy(identity = updated)
                }
            }
            val session = http.post("${config.baseUrl}/v1/guest-sessions") {
                contentType(ContentType.Application.Json)
                setBody(GuestSessionRequest(name = name, avatar = avatar, language = language))
            }.bodyOrThrow<SessionDto>().toDomain()
            tokens.save(session)
            session
        }

    override suspend fun currentSession(): GuestSession? = tokens.current()

    override suspend fun refresh(): GuestSession? {
        val current = tokens.current() ?: return null
        return try {
            normalized {
                val session = http.post("${config.baseUrl}/v1/guest-sessions/refresh") {
                    contentType(ContentType.Application.Json)
                    setBody(RefreshRequest(current.refreshToken))
                }.bodyOrThrow<SessionDto>().toDomain()
                tokens.save(session)
                session
            }
        } catch (error: PartyException) {
            if (error.code == "SESSION_EXPIRED" || error.code == "SESSION_REVOKED" || error.code == "INVALID_TOKEN") {
                tokens.clear()
            }
            throw error
        }
    }

    override suspend fun updateProfile(name: String, avatar: String, language: String): GuestIdentity =
        authed {
            val identity = http.patch("${config.baseUrl}/v1/guest-sessions/profile") {
                contentType(ContentType.Application.Json)
                setBody(GuestSessionRequest(name = name, avatar = avatar, language = language))
            }.bodyOrThrow<ProfileResponseDto>().identity.toDomain()
            tokens.updateIdentity(identity)
            identity
        }

    /**
     * Runs an authenticated request, refreshing the session once when the
     * access token is stale. Without this, a profile update after the access
     * token's short TTL fails forever and blocks room creation.
     */
    private suspend fun <T> authed(block: suspend () -> T): T = try {
        block()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Throwable) {
        val partyError = error.asPartyException()
        if (partyError.isAuthFailure() && runCatching { refresh() }.isSuccess) {
            block()
        } else {
            throw partyError
        }
    }

    /** Normalizes transport failures to [PartyException] so callers handle one error type. */
    private suspend fun <T> normalized(block: suspend () -> T): T = try {
        block()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Throwable) {
        throw error.asPartyException()
    }
}
