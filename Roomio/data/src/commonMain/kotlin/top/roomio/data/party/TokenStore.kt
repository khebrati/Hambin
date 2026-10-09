package top.roomio.data.party

import com.russhwolf.settings.Settings
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import top.roomio.domain.party.GuestIdentity
import top.roomio.domain.party.GuestSession

/** Firebase credentials live in platform secure storage; only non-secret profile data uses Settings. */
@SingleIn(AppScope::class)
@Inject
class TokenStore(
    private val settings: Settings,
    private val json: Json,
    private val secureStorage: SecureCredentialStorage,
) {
    private var credentialsRecord: FirebaseCredentials? = readCredentials()
    private var profile: StoredProfile = readProfile()
    private val mutableSession = MutableStateFlow(toSession())

    /** Compatibility view for repositories and room UI; null until bootstrap succeeds. */
    val session: StateFlow<GuestSession?> = mutableSession.asStateFlow()

    init {
        // Deliberately discard credentials written by the old plaintext format. A clean
        // install is expected for this migration, and retaining them would defeat the
        // secure-storage boundary.
        settings.remove(LEGACY_RECORD_KEY)
    }

    fun current(): GuestSession? = toSession()
    fun credentials(): FirebaseCredentials? = credentialsRecord

    fun saveCredentials(credentials: FirebaseCredentials, name: String, avatar: String, language: String) {
        persistCredentials(credentials)
        profile = StoredProfile(credentials.uid, name, avatar, language, null)
        persistProfile()
        publish()
    }

    fun saveCredentials(credentials: FirebaseCredentials) {
        if (credentialsRecord == null) return
        persistCredentials(credentials)
        publish()
    }

    fun saveIdentity(identity: GuestIdentity) {
        if (credentialsRecord == null) return
        profile = profile.copy(
            identity = StoredIdentity(identity.id, identity.name, identity.avatar, identity.language),
        )
        persistProfile()
        publish()
    }

    fun updateProfile(name: String, avatar: String, language: String) {
        if (credentialsRecord == null) return
        profile = profile.copy(name = name, avatar = avatar, language = language)
        persistProfile()
        publish()
    }

    /** Never clear credentials on provider/network errors; only explicit reset may do so. */
    fun clear() {
        secureStorage.clear()
        settings.remove(PROFILE_KEY)
        settings.remove(LEGACY_RECORD_KEY)
        credentialsRecord = null
        profile = StoredProfile()
        publish()
    }

    private fun persistCredentials(credentials: FirebaseCredentials) {
        // The secure write happens before updating memory so a failed keystore/keychain
        // operation cannot make the session appear successfully persisted.
        secureStorage.write(json.encodeToString(credentials))
        credentialsRecord = credentials
    }

    private fun persistProfile() {
        settings.putString(PROFILE_KEY, json.encodeToString(profile))
    }

    private fun readCredentials(): FirebaseCredentials? {
        // A platform-store failure is different from "no saved credentials". Let
        // it surface so a transient Keychain/Keystore error cannot silently create
        // a new Firebase UID and strand room ownership.
        val serialized = secureStorage.read() ?: return null
        return runCatching { json.decodeFromString<FirebaseCredentials>(serialized) }
            .getOrElse {
                secureStorage.clear()
                null
            }
    }

    private fun readProfile(): StoredProfile = runCatching {
        settings.getStringOrNull(PROFILE_KEY)?.let { json.decodeFromString<StoredProfile>(it) }
    }.getOrNull() ?: StoredProfile()

    private fun toSession(): GuestSession? {
        val credentials = credentialsRecord ?: return null
        val identity = profile.identity?.takeIf { profile.uid == credentials.uid } ?: return null
        return GuestSession(
            GuestIdentity(identity.id, identity.name, identity.avatar, identity.language),
            credentials.idToken,
            credentials.refreshToken,
        )
    }

    private fun publish() {
        mutableSession.value = toSession()
    }

    private companion object {
        const val PROFILE_KEY = "party.profile.v2"
        const val LEGACY_RECORD_KEY = "party.firebaseSession.v1"
    }

    @Serializable
    private data class StoredProfile(
        val uid: String = "",
        val name: String = "",
        val avatar: String = "",
        val language: String = "en",
        val identity: StoredIdentity? = null,
    )

    @Serializable
    private data class StoredIdentity(val id: String, val name: String, val avatar: String, val language: String)
}

/** Data-layer Firebase credential record; never crosses into domain or presentation. */
@Serializable
data class FirebaseCredentials(
    val uid: String,
    val idToken: String,
    val refreshToken: String,
    val expiresAtEpochMs: Long,
    val projectId: String,
)
