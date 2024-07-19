package ir.khebrati.hambin.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import org.koin.core.component.KoinComponent

class HomeScreen : Screen {
    @Composable
    override fun Content() {
        Scaffold(
            bottomBar = { BottomAppBar { Text("Home screen") } }
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val navigator = LocalNavigator.current
                Button(onClick = { navigator!!.push(WatchPartyScreen()) }) {
                    Text("Go to Room Screen")
                }
            }
        }
    }
}