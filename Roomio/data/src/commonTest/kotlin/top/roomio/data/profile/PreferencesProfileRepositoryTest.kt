package top.roomio.data.profile

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import top.roomio.domain.profile.ProfileAvatarId
import top.roomio.domain.profile.UserProfile

class PreferencesProfileRepositoryTest {
    @Test
    fun profilePersistsAcrossRepositoryInstances() {
        val settings = MapSettings()
        val repository = PreferencesProfileRepository(settings)

        repository.saveProfile(
            UserProfile(
                name = "Mira",
                avatar = ProfileAvatarId.MINT,
            ),
        )

        val restoredProfile = PreferencesProfileRepository(settings).currentProfile()
        assertEquals("Mira", restoredProfile.name)
        assertEquals(ProfileAvatarId.MINT, restoredProfile.avatar)
    }
}
