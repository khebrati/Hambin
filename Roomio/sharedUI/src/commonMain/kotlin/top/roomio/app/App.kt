package top.roomio.app

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.stringResource
import roomio.sharedui.generated.resources.Res
import roomio.sharedui.generated.resources.cancel
import roomio.sharedui.generated.resources.display_name
import roomio.sharedui.generated.resources.save
import roomio.sharedui.generated.resources.settings
import top.roomio.app.home.HomeScreen
import top.roomio.app.manager.ManagerMode
import top.roomio.app.manager.PartyManagerScreen
import top.roomio.app.room.RoomScreen
import top.roomio.app.theme.AppTheme

private enum class AppDestination { HOME, MANAGER, ROOM }

@Preview
@Composable
fun App(
    onThemeChanged: @Composable (isDark: Boolean) -> Unit = {}
) = AppTheme(onThemeChanged) {
    var destination by remember { mutableStateOf(AppDestination.HOME) }
    var identityName by remember { mutableStateOf("Nika") }
    var settingsOpen by remember { mutableStateOf(false) }
    var managerMode by remember { mutableStateOf(ManagerMode.CREATE) }
    var nameDraft by remember(identityName, settingsOpen) { mutableStateOf(identityName) }

    when (destination) {
        AppDestination.HOME -> HomeScreen(
            identityName = identityName,
            onSettings = { settingsOpen = true },
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
        AppDestination.MANAGER -> PartyManagerScreen(
            initialMode = managerMode,
            onBack = { destination = AppDestination.HOME },
            onCreate = { destination = AppDestination.ROOM },
            onPreview = { destination = AppDestination.ROOM },
        )
        AppDestination.ROOM -> RoomScreen(modifier = Modifier)
    }

    if (settingsOpen) {
        AlertDialog(
            onDismissRequest = { settingsOpen = false },
            title = { Text(stringResource(Res.string.settings)) },
            text = {
                OutlinedTextField(
                    value = nameDraft,
                    onValueChange = { nameDraft = it },
                    singleLine = true,
                    label = { Text(stringResource(Res.string.display_name)) },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        identityName = nameDraft.trim()
                        settingsOpen = false
                    },
                    enabled = nameDraft.isNotBlank(),
                ) {
                    Text(stringResource(Res.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { settingsOpen = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }
}
