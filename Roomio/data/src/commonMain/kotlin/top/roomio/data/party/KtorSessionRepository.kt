package top.roomio.data.party

import co.touchlab.kermit.Logger
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import top.roomio.data.party.dto.GuestSessionRequest
import top.roomio.data.party.dto.ProfileResponseDto
import top.roomio.data.party.dto.toDomain
import top.roomio.domain.party.GuestIdentity
import top.roomio.domain.party.GuestSession
import top.roomio.domain.party.PARTY_CODE_NETWORK
import top.roomio.domain.party.PartyException
import top.roomio.domain.party.SessionRepository

@SingleIn(AppScope::class)
@Inject
class KtorSessionRepository(
    private val http: HttpClient,
    private val config: BackendConfig,
    private val auth: FirebaseAuthClient,
    private val tokens: TokenStore,
) : SessionRepository {
    private val sessionMutex = Mutex()
    private val log = Logger.withTag("BackendIdentity")

    override suspend fun ensureSession(name: String, avatar: String, language: String): GuestSession = normalized {
        sessionMutex.withLock {
        var credentials = tokens.credentials()
            if (credentials == null) {
                credentials = auth.signUpAnonymously()
                // Save first: an outage during bootstrap must retry this same Firebase UID.
                tokens.saveCredentials(credentials, name, avatar, language)
            }
            credentials = refreshIfNeeded(credentials)
            val current = tokens.current()
            val identity = when {
                current == null -> bootstrap(name, avatar, language)
                current.identity.name != name || current.identity.avatar != avatar || current.identity.language != language ->
                    updateProfileLocked(name, avatar, language)
                else -> current.identity
            }
            tokens.saveIdentity(identity)
            tokens.current() ?: throw PartyException("INVALID_TOKEN", "Guest profile was not saved")
        }
    }

    override suspend fun currentSession(): GuestSession? = tokens.current()

    override suspend fun refresh(): GuestSession? = sessionMutex.withLock {
        val credentials = tokens.credentials() ?: return@withLock null
        refreshLocked(credentials)
        tokens.current()
    }

    override suspend fun updateProfile(name: String, avatar: String, language: String): GuestIdentity = normalized {
        sessionMutex.withLock {
            updateProfileLocked(name, avatar, language)
        }
    }

    private suspend fun updateProfileLocked(name: String, avatar: String, language: String): GuestIdentity {
        var credentials = tokens.credentials()
            ?: throw PartyException("INVALID_TOKEN", "Guest identity is not initialized")
        credentials = refreshIfNeeded(credentials)
        val request = GuestSessionRequest(name = name, avatar = avatar, language = language)
        val identity = try {
            patchProfile(request)
        } catch (error: PartyException) {
            if (!error.isAuthFailure()) throw error
            credentials = refreshLocked(credentials)
            patchProfile(request)
        }
        tokens.saveIdentity(identity)
        tokens.updateProfile(name, avatar, language)
        return identity
    }

    private suspend fun bootstrap(name: String, avatar: String, language: String): GuestIdentity {
        val response = http.post("${config.baseUrl}/v1/identities/bootstrap") {
            contentType(ContentType.Application.Json)
            setBody(GuestSessionRequest(name, avatar, language))
        }
        val identity = try {
            response.bodyOrThrow<ProfileResponseDto>().identity.toDomain()
        } catch (error: PartyException) {
            log.w("endpoint=identities/bootstrap status=${response.status.value} code=${error.code}")
            throw error
        }
        log.i("endpoint=identities/bootstrap status=${response.status.value} result=success")
        tokens.saveIdentity(identity)
        return identity
    }

    private suspend fun patchProfile(request: GuestSessionRequest): GuestIdentity = http.patch(
        "${config.baseUrl}/v1/guest-sessions/profile",
    ) {
        contentType(ContentType.Application.Json)
        setBody(request)
    }.bodyOrThrow<ProfileResponseDto>().identity.toDomain()

    private suspend fun refreshIfNeeded(current: FirebaseCredentials): FirebaseCredentials =
        if (current.expiresAtEpochMs - Clock.System.now().toEpochMilliseconds() <= REFRESH_EARLY_MS) {
            refreshLocked(current)
        } else {
            current
        }

    private suspend fun refreshLocked(current: FirebaseCredentials): FirebaseCredentials {
        val fresh = auth.refresh(current)
        if (fresh.uid != current.uid || fresh.projectId != config.firebaseProjectId) {
            throw PartyException("INVALID_TOKEN", "Firebase refresh changed project identity")
        }
        tokens.saveCredentials(fresh)
        return fresh
    }

    private suspend fun <T> normalized(block: suspend () -> T): T = try {
        block()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Throwable) {
        when (error) {
            is PartyException -> throw error
            else -> throw error.asPartyException()
        }
    }

    private companion object {
        const val REFRESH_EARLY_MS = 5 * 60 * 1000L
    }
}
