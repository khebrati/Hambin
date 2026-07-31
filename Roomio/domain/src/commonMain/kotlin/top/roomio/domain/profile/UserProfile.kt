package top.roomio.domain.profile

import kotlinx.coroutines.flow.Flow

data class UserProfile(
    val name: String,
    val avatar: ProfileAvatarId,
)

enum class ProfileAvatarId {
    COMET,
    MINT,
    SUNNY,
    BERRY,
    CLOUD,
    EMBER,
    NOVA,
    ORBIT,
    PRISM,
    ECHO,
    SPARK,
    BLOOM,
}

interface ProfileRepository {
    fun observeProfile(): Flow<UserProfile>

    fun currentProfile(): UserProfile

    fun saveProfile(profile: UserProfile): Result<Unit>
}
