package top.roomio.app

import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import top.roomio.app.theme.AppTheme
import top.roomio.app.room.RoomScreen

@Preview
@Composable
fun App(
    onThemeChanged: @Composable (isDark: Boolean) -> Unit = {}
) = AppTheme(onThemeChanged) {
    RoomScreen(modifier = Modifier)
}
