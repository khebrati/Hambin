package top.roomio.domain.profile

import kotlinx.coroutines.flow.Flow

data class UserProfile(
    val name: String,
    val avatar: ProfileAvatarId,
    val language: AppLanguage = AppLanguage.ENGLISH,
)

enum class ProfileAvatarId(val displayName: String) {
    COMET("Ice Queen"),
    MINT("Frog Wizard"),
    SUNNY("Skeleton Detective"),
    BERRY("Captain Octopus"),
    CLOUD("Gamer TV"),
    EMBER("Alien Scientist"),
    NOVA("Robot Poet"),
    ORBIT("Coffee Wolf"),
    PRISM("Professor Fox"),
    ECHO("Bookworm Dragon"),
    SPARK("Frankie"),
    BLOOM("Mummy Barista"),
}

interface ProfileRepository {
    fun observeProfile(): Flow<UserProfile>

    fun currentProfile(): UserProfile

    fun saveProfile(profile: UserProfile): Result<Unit>
}

/** Supported interface languages; stable tags are also used by the backend profile. */
enum class AppLanguage(val tag: String) {
    ENGLISH("en"), PERSIAN("fa");
    companion object {
        fun fromTag(tag: String): AppLanguage = entries.firstOrNull { it.tag == tag } ?: ENGLISH
    }
}
