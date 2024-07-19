package ir.khebrati.hambin.presentation.homeScreen.tabs

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions

object WatchPartyManagerTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val icon = rememberVectorPainter(Icons.Outlined.Home)
            val title = "Home"
            return remember {
                TabOptions(
                    index = 1u,
                    title = title,
                    icon = icon
                )
            }
        }

    @Composable
    override fun Content() {
        Text("watch party manager tab content")
    }
}