package top.roomio.app.theme

import com.materialkolor.rememberDynamicColorScheme
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import top.roomio.domain.profile.ThemeColor
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
    themeColor: ThemeColor = ThemeColor.ROOMIO,
    content: @Composable () -> Unit,
) {
    val systemIsDark = isSystemInDarkTheme()
    val isDarkState = remember(systemIsDark) { mutableStateOf(systemIsDark) }

    CompositionLocalProvider(LocalThemeIsDark provides isDarkState) {
        RoomioTheme(darkTheme = isDarkState.value, themeColor = themeColor) {
            onThemeChanged(!isDarkState.value)
            content()
        }
    }
}

/**
 * Cross-platform Roomio theme based on the approved Immersive Cinema direction.
 * The default retains the cinema palette; named seeds generate Material Tonal Spot schemes.
 */
@Composable
internal fun RoomioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeColor: ThemeColor = ThemeColor.ROOMIO,
    content: @Composable () -> Unit,
) {
    val seed = themeColor.seed
    val colorScheme = if (seed == null) {
        if (darkTheme) RoomioDarkColorScheme else RoomioLightColorScheme
    } else {
        rememberDynamicColorScheme(
            seedColor = seed, isDark = darkTheme, style = PaletteStyle.TonalSpot,
            contrastLevel = 0.0, specVersion = ColorSpec.SpecVersion.SPEC_2021,
        )
    }
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
            colorScheme = colorScheme,
            typography = RoomioTypography(),
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
