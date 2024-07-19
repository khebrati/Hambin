import androidx.compose.ui.window.ComposeUIViewController
import ir.khebrati.hambin.App
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController { App() }
