package top.roomio.data.profile

import com.russhwolf.settings.Settings
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import top.roomio.domain.profile.ProfileRepository

@BindingContainer
interface ProfileDataBindings {
    @Binds
    fun bindProfileRepository(
        repository: PreferencesProfileRepository,
    ): ProfileRepository

    companion object {
        @Provides
        fun provideSettings(): Settings = Settings()
    }
}
