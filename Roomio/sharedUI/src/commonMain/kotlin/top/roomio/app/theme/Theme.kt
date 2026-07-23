package top.roomio.app.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

/**
 * Mutable theme state retained for the current sample-level theme toggle.
 * New UI should read colors from MaterialTheme or RoomioDesignSystem.
 */
internal val LocalThemeIsDark = compositionLocalOf<MutableState<Boolean>> {
    error("LocalThemeIsDark must be provided by AppTheme")
}

/** Access to Roomio-specific tokens that do not exist on MaterialTheme. */
internal object RoomioDesignSystem {
    val colors: RoomioExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalRoomioExtendedColors.current

    val spacing: RoomioSpacingTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalRoomioSpacing.current

    val shapes: RoomioExpressiveShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalRoomioExpressiveShapes.current

    val motion: RoomioMotionTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalRoomioMotion.current
}

/**
 * Compatibility wrapper used by App.kt.
 *
 * The callback continues to report whether system bars need dark foreground
 * icons, matching the existing Android and iOS host implementations.
 */
@Composable
internal fun AppTheme(
    onThemeChanged: @Composable (useDarkSystemBarIcons: Boolean) -> Unit,
    content: @Composable () -> Unit,
) {
    val systemIsDark = isSystemInDarkTheme()
    val isDarkState = remember(systemIsDark) { mutableStateOf(systemIsDark) }

    CompositionLocalProvider(LocalThemeIsDark provides isDarkState) {
        RoomioTheme(darkTheme = isDarkState.value) {
            onThemeChanged(!isDarkState.value)
            content()
        }
    }
}

/**
 * Cross-platform Roomio theme based on the approved Immersive Cinema direction.
 * A static scheme is intentional so the product identity is consistent across
 * Android, iOS, and desktop; dynamic color can be added later as an explicit
 * product option rather than silently replacing the approved palette.
 */
@Composable
internal fun RoomioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val extendedColors = if (darkTheme) {
        RoomioDarkExtendedColors
    } else {
        RoomioLightExtendedColors
    }

    CompositionLocalProvider(
        LocalRoomioExtendedColors provides extendedColors,
        LocalRoomioSpacing provides DefaultRoomioSpacing,
        LocalRoomioExpressiveShapes provides DefaultRoomioExpressiveShapes,
        LocalRoomioMotion provides DefaultRoomioMotion,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) RoomioDarkColorScheme else RoomioLightColorScheme,
            typography = RoomioTypography,
            shapes = RoomioMaterialShapes,
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                content = content,
            )
        }
    }
}
