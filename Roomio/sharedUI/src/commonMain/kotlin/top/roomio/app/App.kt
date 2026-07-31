package top.roomio.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import top.roomio.app.home.HomeScreen
import top.roomio.app.manager.ManagerMode
import top.roomio.app.manager.PartyManagerScreen
import top.roomio.app.profile.ProfileAvatar
import top.roomio.app.profile.SettingsScreen
import top.roomio.app.room.RoomScreen
import top.roomio.app.theme.AppTheme

private enum class AppDestination { HOME, SETTINGS, MANAGER, ROOM }

@Preview
@Composable
fun App(
    onThemeChanged: @Composable (isDark: Boolean) -> Unit = {}
) = AppTheme(onThemeChanged) {
    var destination by remember { mutableStateOf(AppDestination.HOME) }
    var identityName by remember { mutableStateOf("Nika") }
    var identityAvatar by remember { mutableStateOf(ProfileAvatar.COMET) }
    var managerMode by remember { mutableStateOf(ManagerMode.CREATE) }

    when (destination) {
        AppDestination.HOME -> HomeScreen(
            identityName = identityName,
            identityAvatar = identityAvatar,
            onSettings = { destination = AppDestination.SETTINGS },
            onCreate = {
                managerMode = ManagerMode.CREATE
                destination = AppDestination.MANAGER
            },
            onJoin = {
                managerMode = ManagerMode.JOIN
                destination = AppDestination.MANAGER
            },
            onOpenRoom = { destination = AppDestination.ROOM },
            modifier = Modifier,
        )
        AppDestination.SETTINGS -> SettingsScreen(
            identityName = identityName,
            identityAvatar = identityAvatar,
            onBack = { destination = AppDestination.HOME },
            onSave = { name, avatar ->
                identityName = name
                identityAvatar = avatar
                destination = AppDestination.HOME
            },
        )
        AppDestination.MANAGER -> PartyManagerScreen(
            initialMode = managerMode,
            onBack = { destination = AppDestination.HOME },
            onCreate = { destination = AppDestination.ROOM },
            onPreview = { destination = AppDestination.ROOM },
        )
        AppDestination.ROOM -> RoomScreen(modifier = Modifier)
    }
}
