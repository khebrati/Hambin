package top.roomio.app.locale

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import platform.Foundation.NSUserDefaults

@Composable
internal actual fun PlatformLanguageEnvironment(tag: String, content: @Composable () -> Unit) {
    // Compose resources use Apple's preferred languages on iOS.
    NSUserDefaults.standardUserDefaults.setObject(listOf(tag), forKey = "AppleLanguages")
    key(tag) { content() }
}
