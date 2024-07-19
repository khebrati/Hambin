package ir.khebrati.hambin.presentation.homeScreen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabNavigator
import ir.khebrati.hambin.presentation.homeScreen.tabs.UserSettingTab
import ir.khebrati.hambin.presentation.homeScreen.tabs.WatchPartyManagerTab
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.tab.Tab

class HomeScreen : Screen {
    @Composable
    override fun Content() {
        TabNavigator(WatchPartyManagerTab) {
            Scaffold(
                content = { CurrentTab() },
                bottomBar = {
                    BottomAppBar {
                        bottomAppBarItem(WatchPartyManagerTab, modifier = Modifier.weight(1f))
                        bottomAppBarItem(UserSettingTab, modifier = Modifier.weight(1f))
                    }
                }
            )
        }
    }
}

@Composable
fun RowScope.bottomAppBarItem(tab: Tab, modifier: Modifier = Modifier) {
    //todo add selected color
    val currentTab = LocalTabNavigator.current
    Box(modifier = modifier.clickable {
        currentTab.current = tab
    }.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement =Arrangement.spacedBy(space = 3.dp, alignment = Alignment.CenterVertically),
            modifier = Modifier.fillMaxHeight()
        ) {
            Icon(tab.options.icon!!, tab.options.title)
            Text(tab.options.title, style = MaterialTheme.typography.labelMedium)
        }
    }
}