package top.roomio.data.party

import com.russhwolf.settings.Settings
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import top.roomio.domain.party.GuestIdentity
import top.roomio.domain.party.GuestSession

/**
 * Persists the guest identity and its rotated tokens in platform settings and
 * exposes them for the HTTP client's default request.
 */
@SingleIn(AppScope::class)
@Inject
class TokenStore(
    private val settings: Settings,
) {
    private val mutableSession = MutableStateFlow(readSession())

    val session: StateFlow<GuestSession?> = mutableSession.asStateFlow()

    fun current(): GuestSession? = mutableSession.value

    fun save(session: GuestSession) {
        settings.putString(ACCESS_KEY, session.accessToken)
        settings.putString(REFRESH_KEY, session.refreshToken)
        settings.putString(ID_KEY, session.identity.id)
        settings.putString(NAME_KEY, session.identity.name)
        settings.putString(AVATAR_KEY, session.identity.avatar)
        settings.putString(LANGUAGE_KEY, session.identity.language)
        mutableSession.value = session
    }

    fun updateIdentity(identity: GuestIdentity) {
        val existing = mutableSession.value ?: return
        save(existing.copy(identity = identity))
    }

    fun clear() {
        settings.remove(ACCESS_KEY)
        settings.remove(REFRESH_KEY)
        settings.remove(ID_KEY)
        settings.remove(NAME_KEY)
        settings.remove(AVATAR_KEY)
        settings.remove(LANGUAGE_KEY)
        mutableSession.value = null
    }

    private fun readSession(): GuestSession? {
        val access = settings.getStringOrNull(ACCESS_KEY) ?: return null
        val refresh = settings.getStringOrNull(REFRESH_KEY) ?: return null
        val id = settings.getStringOrNull(ID_KEY) ?: return null
        return GuestSession(
            identity = GuestIdentity(
                id = id,
                name = settings.getStringOrNull(NAME_KEY).orEmpty(),
                avatar = settings.getStringOrNull(AVATAR_KEY).orEmpty(),
                language = settings.getStringOrNull(LANGUAGE_KEY).orEmpty(),
            ),
            accessToken = access,
            refreshToken = refresh,
        )
    }

    private companion object {
        const val ACCESS_KEY = "party.accessToken"
        const val REFRESH_KEY = "party.refreshToken"
        const val ID_KEY = "party.identityId"
        const val NAME_KEY = "party.identityName"
        const val AVATAR_KEY = "party.identityAvatar"
        const val LANGUAGE_KEY = "party.identityLanguage"
    }
}
