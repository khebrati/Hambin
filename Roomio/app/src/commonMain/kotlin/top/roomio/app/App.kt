package top.roomio.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.createGraph
import top.roomio.app.ui.RoomioUi
import top.roomio.data.party.PartyDataBindings
import top.roomio.data.profile.ProfileDataBindings
import top.roomio.domain.party.RealtimeClient
import top.roomio.domain.party.RoomRepository
import top.roomio.domain.party.SessionKeeper
import top.roomio.domain.party.SessionRepository
import top.roomio.domain.party.VoiceClient
import top.roomio.domain.profile.ProfileRepository

@DependencyGraph(
    AppScope::class,
    bindingContainers = [ProfileDataBindings::class, PartyDataBindings::class],
)
internal interface RoomioAppGraph {
    val profileRepository: ProfileRepository
    val sessionRepository: SessionRepository
    val roomRepository: RoomRepository
    val realtimeClient: RealtimeClient
    val voiceClient: VoiceClient
    val sessionKeeper: SessionKeeper
}

@Composable
fun App(
    onThemeChanged: @Composable (isDark: Boolean) -> Unit = {},
) {
    val graph = remember { createGraph<RoomioAppGraph>() }
    RoomioUi(
        profileRepository = graph.profileRepository,
        sessionRepository = graph.sessionRepository,
        roomRepository = graph.roomRepository,
        realtimeClient = graph.realtimeClient,
        voiceClient = graph.voiceClient,
        sessionKeeper = graph.sessionKeeper,
        onThemeChanged = onThemeChanged,
    )
}
