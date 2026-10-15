package top.roomio.app.androidApp

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat
import top.roomio.app.App

class AppActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEBUG) {
            // Local emulator defaults; explicit extras also let a physical device use
            // adb reverse or a host-LAN address without changing release configuration.
            System.setProperty("roomio.backendUrl", intent.getStringExtra("roomio.backendUrl") ?: "http://10.0.2.2:8081")
            System.setProperty("roomio.firebaseProjectId", intent.getStringExtra("roomio.firebaseProjectId") ?: "demo-hambin")
            System.setProperty("roomio.authEmulatorHost", intent.getStringExtra("roomio.authEmulatorHost") ?: "10.0.2.2:9099")
        }
        enableEdgeToEdge()
        setContent { 
            App(onThemeChanged = { ThemeChanged(it) }) 
        }
    }
}

@Composable
private fun ThemeChanged(isDark: Boolean) {
    val view = LocalView.current
    LaunchedEffect(isDark) {
        val window = (view.context as Activity).window
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = isDark
            isAppearanceLightNavigationBars = isDark
        }
    }
}
