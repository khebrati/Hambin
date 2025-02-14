import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.awt.Dimension
import ir.khebrati.hambin.App
import ir.khebrati.hambin.di.initKoin

fun main() = application {
    Window(
        title = "Hambin",
        state = rememberWindowState(width = 800.dp, height = 600.dp),
        onCloseRequest = ::exitApplication,
    ) {
        window.minimumSize = Dimension(350, 600)
        val koinApplication = initKoin(emptyList())
        App(koinApplication.koin)
    }
}