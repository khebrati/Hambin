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

/** Firebase credentials and Hambin profile are committed as one settings value. */
@SingleIn(AppScope::class)
@Inject
class TokenStore(
    private val settings: Settings,
    private val json: Json,
) {
    private val mutableRecord = MutableStateFlow(readRecord())
    private val mutableSession = MutableStateFlow(mutableRecord.value?.toGuestSession())

    /** Compatibility view for repositories and room UI; null until bootstrap succeeds. */
    val session: StateFlow<GuestSession?> = mutableSession.asStateFlow()

    fun current(): GuestSession? = mutableRecord.value?.toGuestSession()
    fun credentials(): FirebaseCredentials? = mutableRecord.value?.credentials()

    fun saveCredentials(credentials: FirebaseCredentials, name: String, avatar: String, language: String) {
        commit(
            PersistedGuestRecord(
                uid = credentials.uid,
                idToken = credentials.idToken,
                refreshToken = credentials.refreshToken,
                expiresAtEpochMs = credentials.expiresAtEpochMs,
                projectId = credentials.projectId,
                profileName = name,
                profileAvatar = avatar,
                profileLanguage = language,
                identity = null,
            ),
        )
    }

    fun saveCredentials(credentials: FirebaseCredentials) {
        val current = mutableRecord.value ?: return
        commit(current.copy(
            idToken = credentials.idToken,
            refreshToken = credentials.refreshToken,
            expiresAtEpochMs = credentials.expiresAtEpochMs,
            projectId = credentials.projectId,
        ))
    }

    fun saveIdentity(identity: GuestIdentity) {
        val current = mutableRecord.value ?: return
        commit(current.copy(
            profileName = identity.name,
            profileAvatar = identity.avatar,
            profileLanguage = identity.language,
            identity = StoredIdentity(identity.id, identity.name, identity.avatar, identity.language),
        ))
    }

    fun updateProfile(name: String, avatar: String, language: String) {
        val current = mutableRecord.value ?: return
        commit(current.copy(profileName = name, profileAvatar = avatar, profileLanguage = language))
    }

    /** Never clear credentials on provider/network errors; only explicit reset may do so. */
    fun clear() {
        settings.remove(RECORD_KEY)
        mutableRecord.value = null
        mutableSession.value = null
    }

    private fun commit(record: PersistedGuestRecord) {
        settings.putString(RECORD_KEY, json.encodeToString(record))
        mutableRecord.value = record
        mutableSession.value = record.toGuestSession()
    }

    private fun readRecord(): PersistedGuestRecord? = settings.getStringOrNull(RECORD_KEY)
        ?.let { value -> runCatching { json.decodeFromString<PersistedGuestRecord>(value) }.getOrNull() }

    private companion object {
        const val RECORD_KEY = "party.firebaseSession.v1"
    }

    @Serializable
    private data class PersistedGuestRecord(
        val uid: String,
        val idToken: String,
        val refreshToken: String,
        val expiresAtEpochMs: Long,
        val projectId: String,
        val profileName: String,
        val profileAvatar: String,
        val profileLanguage: String,
        val identity: StoredIdentity? = null,
    ) {
        fun credentials() = FirebaseCredentials(uid, idToken, refreshToken, expiresAtEpochMs, projectId)
        fun toGuestSession(): GuestSession? = identity?.let {
            GuestSession(GuestIdentity(it.id, it.name, it.avatar, it.language), idToken, refreshToken)
        }
    }

    @Serializable
    private data class StoredIdentity(val id: String, val name: String, val avatar: String, val language: String)
}

/** Data-layer Firebase credential record; never crosses into domain or presentation. */
data class FirebaseCredentials(
    val uid: String,
    val idToken: String,
    val refreshToken: String,
    val expiresAtEpochMs: Long,
    val projectId: String,
)
