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

    override suspend fun ensureSession(name: String, avatar: String, language: String): GuestSession {
        tokens.current()?.let { return it }
        val session = http.post("${config.baseUrl}/v1/guest-sessions") {
            contentType(ContentType.Application.Json)
            setBody(GuestSessionRequest(name = name, avatar = avatar, language = language))
        }.bodyOrThrow<SessionDto>().toDomain()
        tokens.save(session)
        return session
    }

    override suspend fun currentSession(): GuestSession? = tokens.current()

    override suspend fun refresh(): GuestSession? {
        val current = tokens.current() ?: return null
        return try {
            val session = http.post("${config.baseUrl}/v1/guest-sessions/refresh") {
                contentType(ContentType.Application.Json)
                setBody(RefreshRequest(current.refreshToken))
            }.bodyOrThrow<SessionDto>().toDomain()
            tokens.save(session)
            session
        } catch (error: PartyException) {
            if (error.code == "SESSION_EXPIRED" || error.code == "SESSION_REVOKED" || error.code == "INVALID_TOKEN") {
                tokens.clear()
            }
            throw error
        }
    }

    override suspend fun updateProfile(name: String, avatar: String, language: String): GuestIdentity {
        val identity = http.patch("${config.baseUrl}/v1/guest-sessions/profile") {
            contentType(ContentType.Application.Json)
            setBody(GuestSessionRequest(name = name, avatar = avatar, language = language))
        }.bodyOrThrow<ProfileResponseDto>().identity.toDomain()
        tokens.updateIdentity(identity)
        return identity
    }
}
