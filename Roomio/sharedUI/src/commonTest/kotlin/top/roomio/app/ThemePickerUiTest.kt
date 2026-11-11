package top.roomio.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertNotEquals
import top.roomio.app.profile.SettingsScreen
import top.roomio.app.profile.SettingsViewModel
import top.roomio.app.theme.RoomioTheme
import top.roomio.app.theme.LocalThemeIsDark

@OptIn(ExperimentalTestApi::class)
class ThemePickerUiTest {
    @Test
    fun colorChoiceAppliesAcrossCompactAndExpandedDarkRtlLargeText() = runComposeUiTest {
        val preferences = TestThemePreferencesRepository()
        val viewModel = SettingsViewModel(TestProfileRepository(), preferences)
        val expanded = mutableStateOf(false)
        var primary = Color.Unspecified
        setContent {
            val density = LocalDensity.current
            val selectedColor by preferences.color.collectAsState()
            val state by viewModel.state.collectAsState()
            val dark = remember(expanded.value) { mutableStateOf(expanded.value) }
            CompositionLocalProvider(
                LocalThemeIsDark provides dark,
                LocalDensity provides Density(density.density, if (expanded.value) 1.8f else 1f),
                LocalLayoutDirection provides if (expanded.value) LayoutDirection.Rtl else LayoutDirection.Ltr,
            ) {
                RoomioTheme(darkTheme = dark.value, themeColor = selectedColor) {
                    val scheme = MaterialTheme.colorScheme
                    SideEffect { primary = scheme.primary }
                    Box(Modifier.width(if (expanded.value) 1000.dp else 360.dp).height(900.dp)) {
                        SettingsScreen(state, viewModel::onAction)
                    }
                }
            }
        }
        val initial = primary
        onNodeWithText("Sage").performScrollTo().assertIsDisplayed().performClick()
        onNodeWithText("Sage").assertIsSelected()
        runOnIdle { assertNotEquals(initial, primary); expanded.value = true }
        onNodeWithText("Cocoa").performScrollTo().assertIsDisplayed().performClick()
        onNodeWithText("Cocoa").assertIsSelected()
    }
}
