package top.roomio.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.createGraphFactory
import kotlinx.coroutines.flow.flowOf
import top.roomio.app.home.HomeViewModel
import top.roomio.app.manager.PartyManagerViewModel
import top.roomio.app.navigation.RoomioNavigation
import top.roomio.app.profile.SettingsViewModel
import top.roomio.app.preview.RoomPreviewViewModel
import top.roomio.app.room.RoomViewModel
import top.roomio.app.theme.AppTheme
import top.roomio.domain.profile.ProfileAvatarId
import top.roomio.domain.profile.ProfileRepository
import top.roomio.domain.profile.UserProfile

@DependencyGraph
internal interface PresentationGraph {
    val homeViewModel: HomeViewModel
    val settingsViewModel: SettingsViewModel
    val partyManagerViewModelFactory: PartyManagerViewModel.Factory
    val roomPreviewViewModelFactory: RoomPreviewViewModel.Factory
    val roomViewModelFactory: RoomViewModel.Factory

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides profileRepository: ProfileRepository,
        ): PresentationGraph
    }
}

@Preview
@Composable
private fun AppPreview() = RoomioUi(
    profileRepository = PreviewProfileRepository,
)

@Composable
fun RoomioUi(
    profileRepository: ProfileRepository,
    onThemeChanged: @Composable (isDark: Boolean) -> Unit = {},
) {
    val presentationGraph = remember(profileRepository) {
        createGraphFactory<PresentationGraph.Factory>().create(profileRepository)
    }

    AppTheme(onThemeChanged) {
        RoomioNavigation(
            presentationGraph = presentationGraph,
            modifier = Modifier,
        )
    }
}

private object PreviewProfileRepository : ProfileRepository {
    private val profile = UserProfile("Nika", ProfileAvatarId.COMET)

    override fun observeProfile() = flowOf(profile)

    override fun currentProfile() = profile

    override fun saveProfile(profile: UserProfile) = Result.success(Unit)
}
