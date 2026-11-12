package top.roomio.app.room

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import java.io.File
import org.jetbrains.skia.Image
import top.roomio.app.locale.AppLanguageEnvironment
import top.roomio.app.theme.LocalThemeIsDark
import top.roomio.app.theme.RoomioTheme
import top.roomio.domain.party.ChatMessage
import top.roomio.domain.profile.AppLanguage
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class RoomChatUiTest {
    @Test fun compactPanelKeepsDraftRetryAndCollapseAvailableAtLargeText() = runComposeUiTest {
        val state=mutableStateOf(RoomChatState(connected=true,loading=false,pending=listOf(PendingChatMessage("failed","A failed message",ChatSendState.FAILED,"NETWORK"))))
        val actions=mutableListOf<RoomAction>()
        setContent {
            val density=LocalDensity.current
            CompositionLocalProvider(LocalThemeIsDark provides remember { mutableStateOf(false) }, LocalDensity provides Density(density.density,1.5f)) {
                RoomioTheme(darkTheme=false) {
                    Column(Modifier.width(360.dp).height(1000.dp).verticalScroll(rememberScrollState())) {
                        RoomChatPanel(state.value,{ actions+=it; if(it is RoomAction.ChatDraftChanged) state.value=state.value.copy(draft=it.text); if(it==RoomAction.ChatToggle)state.value=state.value.copy(open=!state.value.open) })
                    }
                }
            }
        }
        onNodeWithContentDescription("Send message").performScrollTo().assertIsNotEnabled()
        onNodeWithText("Message the party").performScrollTo().performTextInput("Hello 🍿")
        onNodeWithContentDescription("Send message").performScrollTo().assertIsEnabled().performClick()
        runOnIdle { assertTrue(RoomAction.ChatSend in actions) }
        onNodeWithText("Retry").performScrollTo().performClick()
        runOnIdle { assertTrue(RoomAction.ChatRetry("failed") in actions) }
        onNodeWithContentDescription("Collapse chat").performScrollTo().performClick()
        onNodeWithContentDescription("Open chat").assertIsDisplayed().performClick()
        onNodeWithText("Message the party").performScrollTo().assertTextContains("Hello 🍿")
        runOnIdle { state.value=state.value.copy(connected=false) }
        onNodeWithContentDescription("Send message").performScrollTo().assertIsNotEnabled()
    }

    @Test fun sharedRoomRendersSelectedSidePanelInCompactPersianAndExpandedEnglish() = runComposeUiTest {
        val expanded=mutableStateOf(false)
        val model=RoomScreenModel("Friday night screening","ABCD2345",RoomRole.OWNER,"Nika",true,RoomPlaybackState.PLAYING,emptyList())
        val message=ChatMessage("message","room","friend","member","Jo","berry","سلام 🍿\nMovie night","2026-11-12T06:00:00Z",5)
        setContent {
            val density=LocalDensity.current
            AppLanguageEnvironment(if(expanded.value) AppLanguage.ENGLISH else AppLanguage.PERSIAN) {
                CompositionLocalProvider(LocalThemeIsDark provides remember { mutableStateOf(true) },LocalDensity provides Density(density.density,if(expanded.value)1f else 1.5f)) {
                    RoomioTheme(darkTheme=true) {
                        Box(Modifier.width(if(expanded.value)1280.dp else 360.dp).height(1000.dp)) {
                            RoomScreen(state=RoomUiState(model,chat=RoomChatState(identityId="me",messages=listOf(message),connected=true,loading=false)),playerContent={ Box(it) })
                        }
                    }
                }
            }
        }
        onNodeWithText("گفتگوی دورهمی").performScrollTo().assertIsDisplayed()
        onNodeWithText("پیام به دورهمی").performScrollTo()
        File("/tmp/hambin-chat-native-debug.png").writeBytes(Image.makeFromBitmap(onRoot().captureToImage().asSkiaBitmap()).encodeToData()!!.bytes)
        onNodeWithText("سلام 🍿\nMovie night").performScrollTo().assertIsDisplayed()
        File("/tmp/hambin-chat-native-compact.png").writeBytes(Image.makeFromBitmap(onRoot().captureToImage().asSkiaBitmap()).encodeToData()!!.bytes)
        runOnIdle { expanded.value=true }
        onNodeWithText("Party chat").assertIsDisplayed()
        onNodeWithContentDescription("Send message").assertIsDisplayed()
        File("/tmp/hambin-chat-native-expanded.png").writeBytes(Image.makeFromBitmap(onRoot().captureToImage().asSkiaBitmap()).encodeToData()!!.bytes)
    }
}
