package ir.khebrati.hambin

import androidx.compose.runtime.*
import cafe.adriel.voyager.navigator.Navigator
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import ir.khebrati.hambin.presentation.HomeScreen
import ir.khebrati.hambin.theme.AppTheme
import org.koin.core.Koin

@Composable
fun App(koin: Koin) = AppTheme {
    Napier.base(DebugAntilog())
    Navigator(HomeScreen())
}