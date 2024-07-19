package ir.khebrati.hambin.Presentation

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import ir.khebrati.hambin.TestClass
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class UserSettingScreen : Screen,KoinComponent {
    private val testClass : TestClass by inject()
    @Composable
    override fun Content() {
        testClass.test()
    }
}