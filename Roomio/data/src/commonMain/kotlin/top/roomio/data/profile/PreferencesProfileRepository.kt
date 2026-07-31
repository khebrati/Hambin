package top.roomio.data.profile

import com.russhwolf.settings.Settings
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import top.roomio.domain.profile.ProfileAvatarId
import top.roomio.domain.profile.ProfileRepository
import top.roomio.domain.profile.UserProfile

@Inject
class PreferencesProfileRepository(
    private val settings: Settings,
) : ProfileRepository {
    private val mutableProfile = MutableStateFlow(readProfile())

    override fun observeProfile() = mutableProfile.asStateFlow()

    override fun currentProfile(): UserProfile = mutableProfile.value

    override fun saveProfile(profile: UserProfile): Result<Unit> = runCatching {
        settings.putString(NAME_KEY, profile.name)
        settings.putString(AVATAR_KEY, profile.avatar.name)
        mutableProfile.value = profile
    }

    private fun readProfile(): UserProfile {
        val storedName = settings
            .getString(NAME_KEY, DEFAULT_PROFILE.name)
            .trim()
            .ifEmpty { DEFAULT_PROFILE.name }
        val storedAvatar = settings.getString(AVATAR_KEY, DEFAULT_PROFILE.avatar.name)
        val avatar = ProfileAvatarId.entries
            .firstOrNull { it.name == storedAvatar }
            ?: DEFAULT_PROFILE.avatar

        return UserProfile(
            name = storedName,
            avatar = avatar,
        )
    }

    private companion object {
        const val NAME_KEY = "profile.name"
        const val AVATAR_KEY = "profile.avatar"

        val DEFAULT_PROFILE = UserProfile(
            name = "Nika",
            avatar = ProfileAvatarId.COMET,
        )
    }
}
