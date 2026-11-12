package top.roomio.app

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.test.*
import top.roomio.app.locale.AppLanguageEnvironment
import top.roomio.app.profile.SettingsAction
import top.roomio.app.profile.SettingsScreen
import top.roomio.app.profile.SettingsViewModel
import top.roomio.app.theme.LocalThemeIsDark
import top.roomio.app.theme.RoomioTheme
import top.roomio.domain.profile.AppLanguage

@OptIn(ExperimentalTestApi::class)
class PersianLanguageTest {
    @Test
    fun savedLanguageChangesResourcesAndDirectionAndCanReturnToEnglish() = runComposeUiTest {
        val repository = TestProfileRepository()
        val viewModel = SettingsViewModel(repository, TestThemePreferencesRepository())
        val language = mutableStateOf(AppLanguage.ENGLISH)
        var direction = LayoutDirection.Ltr
        setContent {
            AppLanguageEnvironment(language.value) {
                val state by viewModel.state.collectAsState()
                direction = LocalLayoutDirection.current
                CompositionLocalProvider(LocalThemeIsDark provides remember { mutableStateOf(false) }) {
                    RoomioTheme(darkTheme = false) {
                        Box(Modifier.width(360.dp).height(900.dp)) { SettingsScreen(state, viewModel::onAction) }
                    }
                }
            }
        }
        onNodeWithText("فارسی").performScrollTo().performClick()
        runOnIdle {
            assertEquals(AppLanguage.PERSIAN, viewModel.state.value.draftLanguage)
            assertEquals(AppLanguage.ENGLISH, repository.currentProfile().language)
            viewModel.onAction(SettingsAction.SaveClicked)
            language.value = repository.currentProfile().language
        }
        onNodeWithText("نمایه شما").assertExists()
        runOnIdle { assertEquals(LayoutDirection.Rtl, direction) }
        onNodeWithText("English").performScrollTo().performClick()
        runOnIdle {
            viewModel.onAction(SettingsAction.SaveClicked)
            language.value = repository.currentProfile().language
        }
        onNodeWithText("Your profile").assertExists()
        runOnIdle { assertEquals(LayoutDirection.Ltr, direction) }
    }
}
