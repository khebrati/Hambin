package top.roomio.app

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import top.roomio.app.theme.AppTheme

@OptIn(ExperimentalTestApi::class)
class ComposeTest {

    @Test
    fun simpleCheck() = runComposeUiTest {
        setContent {
            var txt by remember { mutableStateOf("Go") }
            Column {
                Text(
                    text = txt,
                    modifier = Modifier.testTag("t_text")
                )
                Button(
                    onClick = { txt += "." },
                    modifier = Modifier.testTag("t_button")
                ) {
                    Text("click me")
                }
            }
        }

        onNodeWithTag("t_button").apply {
            repeat(3) { performClick() }
        }
        onNodeWithTag("t_text").assertTextEquals("Go...")
    }

    @Test
    fun inviteDialogShowsBothShareOptions() = runComposeUiTest {
        setContent {
            AppTheme(onThemeChanged = {}) {
                App()
            }
        }

        onNodeWithText("Create a party").performClick()
        onNodeWithText("Create party").performClick()

        onNodeWithContentDescription("Invite friends").performClick()

        onNodeWithText("Share either option. Friends will preview the room before they join.").assertExists()
        onNodeWithText("Party code").assertExists()
        onNodeWithText("Prototype invite link").assertExists()
        onNodeWithText("Copy code").assertExists()
        onNodeWithText("Copy link").assertExists()
    }

    @Test
    fun homeShowsIdentityActionsAndReturnableRoom() = runComposeUiTest {
        setContent {
            App()
        }

        onNodeWithText("Bring the film. Keep your own pace.").assertExists()
        onNodeWithText("Nika").assertExists()
        onNodeWithText("Create a party").assertExists()
        onNodeWithText("Join with code").assertExists()
        onNodeWithText("Friday night screening").assertExists()
        onNodeWithText("2 friends · No owner").assertExists()
    }

    @Test
    fun homeSettingsCanUpdateTheDisplayName() = runComposeUiTest {
        setContent {
            App()
        }

        onNodeWithContentDescription("Settings").performClick()

        onNodeWithText("Your profile").assertExists()
        onNodeWithText("Display name").assertExists()
        onNodeWithText("Avatar").assertExists()
        onNodeWithText("Comet").assertExists()
        onNodeWithText("Sunny").assertExists()
        onNodeWithText("Save profile").assertExists()
    }

    @Test
    fun createFromHomeOpensPartyManagerAndCreatesRoom() = runComposeUiTest {
        setContent {
            App()
        }

        onNodeWithText("Create a party").performClick()

        onNodeWithText("Party manager").assertExists()
        onNodeWithText("Open a room for movie night").assertExists()
        onNodeWithText("You will be the host", ignoreCase = true).assertExists()

        onNodeWithText("Create party").performClick()

        onNodeWithText("Your cinema is empty").assertExists()
    }

    @Test
    fun joinFromHomeOpensDisabledEmptyCodeState() = runComposeUiTest {
        setContent {
            App()
        }

        onNodeWithText("Join with code").performClick()

        onNodeWithText("Step into their room").assertExists()
        onNodeWithText("Party code").assertExists()
        onNodeWithText("Preview party").assertIsNotEnabled()
    }

    @Test
    fun destinationUpActionsReturnToHome() = runComposeUiTest {
        setContent {
            App()
        }

        onNodeWithContentDescription("Settings").performClick()
        onNodeWithText("Your profile").assertExists()
        onNodeWithContentDescription("Back").performClick()
        onNodeWithText("Bring the film. Keep your own pace.").assertExists()

        onNodeWithText("Create a party").performClick()
        onNodeWithText("Party manager").assertExists()
        onNodeWithContentDescription("Back").performClick()
        onNodeWithText("Bring the film. Keep your own pace.").assertExists()
    }

    @Test
    fun confirmedLeavePartyReturnsToHome() = runComposeUiTest {
        setContent {
            App()
        }

        onNodeWithText("Create a party").performClick()
        onNodeWithText("Create party").performClick()
        onNodeWithContentDescription("Leave").performClick()
        onNodeWithText("Leave party?").assertExists()
        onNodeWithText("Leave").performClick()

        onNodeWithText("Bring the film. Keep your own pace.").assertExists()
        onNodeWithText("Party manager").assertDoesNotExist()
    }

    @Test
    fun joinCodeShowsPreviewBeforeEnteringRoom() = runComposeUiTest {
        setContent {
            App()
        }

        onNodeWithText("Join with code").performClick()
        onNodeWithText("Party code").performTextInput("MOON-42")
        onNodeWithText("Preview party").performClick()

        onNodeWithText("Room preview").assertExists()
        onNodeWithText("Mira's late show").assertExists()
        onNodeWithText("Open").assertExists()
        onNodeWithText("3 people").assertExists()
        onNodeWithText("Playing now").assertExists()

        onNodeWithText("Join party").performClick()

        onNodeWithText("Voice party").assertExists()
        onNodeWithText("MOON-42 · Guest").assertExists()
    }

    @Test
    fun notNowReturnsFromPreviewToEnteredCode() = runComposeUiTest {
        setContent {
            App()
        }

        onNodeWithText("Join with code").performClick()
        onNodeWithText("Party code").performTextInput("MOON-42")
        onNodeWithText("Preview party").performClick()
        onNodeWithText("Not now").performClick()

        onNodeWithText("Step into their room").assertExists()
        onNodeWithText("MOON-42").assertExists()
    }
}
