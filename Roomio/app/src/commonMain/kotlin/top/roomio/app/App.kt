package top.roomio.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.createGraph
import top.roomio.app.ui.RoomioUi
import top.roomio.data.profile.ProfileDataBindings
import top.roomio.domain.profile.ProfileRepository

@DependencyGraph(bindingContainers = [ProfileDataBindings::class])
internal interface RoomioAppGraph {
    val profileRepository: ProfileRepository
}

@Composable
fun App(
    onThemeChanged: @Composable (isDark: Boolean) -> Unit = {},
) {
    val graph = remember { createGraph<RoomioAppGraph>() }
    RoomioUi(
        profileRepository = graph.profileRepository,
        onThemeChanged = onThemeChanged,
    )
}
