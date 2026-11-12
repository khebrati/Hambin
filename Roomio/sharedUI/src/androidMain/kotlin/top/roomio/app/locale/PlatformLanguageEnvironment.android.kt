package top.roomio.app.locale

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Locale

@Composable
internal actual fun PlatformLanguageEnvironment(tag: String, content: @Composable () -> Unit) {
    // Compose resource locale and background notification strings follow the saved choice.
    Locale.setDefault(Locale.forLanguageTag(tag))
    val configuration = LocalConfiguration.current
    val localized = remember(configuration, tag) {
        Configuration(configuration).apply { setLocale(Locale.forLanguageTag(tag)) }
    }
    // Preserve the Activity in LocalContext for fullscreen, sharing and activity results.
    CompositionLocalProvider(LocalConfiguration provides localized, content = content)
}
