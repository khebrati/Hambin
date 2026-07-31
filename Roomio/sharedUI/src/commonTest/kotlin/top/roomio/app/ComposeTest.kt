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
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import top.roomio.app.room.RoomScreen
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
                RoomScreen()
            }
        }

        onNodeWithContentDescription("Invite friends").performClick()

        onNodeWithText("Share either option. Friends will preview the room before they join.").assertExists()
        onNodeWithText("Party code").assertExists()
        onNodeWithText("Prototype invite link").assertExists()
        onNodeWithText("Copy code").assertExists()
        onNodeWithText("Copy link").assertExists()
    }
}
