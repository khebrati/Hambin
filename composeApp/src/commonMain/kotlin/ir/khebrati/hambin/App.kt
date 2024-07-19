package ir.khebrati.hambin

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.Navigator
import hambin.composeapp.generated.resources.*
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import ir.khebrati.hambin.Presentation.UserSettingScreen
import ir.khebrati.hambin.theme.AppTheme
import ir.khebrati.hambin.theme.LocalThemeIsDark
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.core.Koin
import org.koin.core.context.KoinContext

@Composable
fun App(koin: Koin) = AppTheme {
    Napier.base(DebugAntilog())
    Navigator(UserSettingScreen()) {
        Scaffold(
            content = { CurrentScreen() },
            bottomBar = {
                BottomAppBar {
                    Text("BottomAppBar")
                }
            }
        )
    }
}