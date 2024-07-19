package ir.khebrati.hambin.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import hambin.composeapp.generated.resources.Res
import androidx.compose.ui.text.font.FontWeight
import hambin.composeapp.generated.resources.Roboto_Black
import hambin.composeapp.generated.resources.Roboto_BlackItalic
import hambin.composeapp.generated.resources.Roboto_Bold
import hambin.composeapp.generated.resources.Roboto_BoldItalic
import hambin.composeapp.generated.resources.Roboto_Italic
import hambin.composeapp.generated.resources.Roboto_Light
import hambin.composeapp.generated.resources.Roboto_LightItalic
import hambin.composeapp.generated.resources.Roboto_Medium
import hambin.composeapp.generated.resources.Roboto_MediumItalic
import hambin.composeapp.generated.resources.Roboto_Regular
import hambin.composeapp.generated.resources.Roboto_Thin
import hambin.composeapp.generated.resources.Roboto_ThinItalic
//import hambin.composeapp.generated.resources.NotoSerifGeorgian_Black
//import hambin.composeapp.generated.resources.NotoSerifGeorgian_Bold
//import hambin.composeapp.generated.resources.NotoSerifGeorgian_Condensed_Black
//import hambin.composeapp.generated.resources.NotoSerifGeorgian_Condensed_Bold
//import hambin.composeapp.generated.resources.NotoSerifGeorgian_Condensed_ExtraBold
//import hambin.composeapp.generated.resources.NotoSerifGeorgian_Condensed_ExtraLight
//import hambin.composeapp.generated.resources.NotoSerifGeorgian_Condensed_Light
//import hambin.composeapp.generated.resources.NotoSerifGeorgian_Condensed_Medium
//import hambin.composeapp.generated.resources.NotoSerifGeorgian_Condensed_Regular
//import hambin.composeapp.generated.resources.NotoSerifGeorgian_Condensed_SemiBold
//import hambin.composeapp.generated.resources.NotoSerifGeorgian_Condensed_Thin
//import hambin.composeapp.generated.resources.NotoSerifGeorgian_ExtraBold
//import hambin.composeapp.generated.resources.NotoSerifGeorgian_ExtraCondensed_Black
//import hambin.composeapp.generated.resources.NotoSerifGeorgian_ExtraCondensed_Bold
//import hambin.composeapp.generated.resources.NotoSerifGeorgian_ExtraCondensed_ExtraBold
//import hambin.composeapp.generated.resources.NotoSerifGeorgian_ExtraCondensed_ExtraLight
//import hambin.composeapp.generated.resources.NotoSerifGeorgian_ExtraCondensed_Light
//import hambin.composeapp.generated.resources.NotoSerifGeorgian_ExtraCondensed_Medium
//import hambin.composeapp.generated.resources.NotoSerifGeorgian_ExtraCondensed_Regular
//import hambin.composeapp.generated.resources.NotoSerifGeorgian_ExtraCondensed_SemiBold
//import hambin.composeapp.generated.resources.NotoSerifGeorgian_ExtraCondensed_Thin
//import hambin.composeapp.generated.resources.NotoSerifGeorgian_ExtraLight
//import hambin.composeapp.generated.resources.NotoSerifGeorgian_Light
//import hambin.composeapp.generated.resources.NotoSerifGeorgian_Medium
//import hambin.composeapp.generated.resources.NotoSerifGeorgian_Regular
//import hambin.composeapp.generated.resources.NotoSerifGeorgian_SemiBold
//import hambin.composeapp.generated.resources.NotoSerifGeorgian_SemiCondensed_Black
//import hambin.composeapp.generated.resources.NotoSerifGeorgian_SemiCondensed_Bold
//import hambin.composeapp.generated.resources.NotoSerifGeorgian_SemiCondensed_ExtraBold
//import hambin.composeapp.generated.resources.NotoSerifGeorgian_SemiCondensed_ExtraLight
//import hambin.composeapp.generated.resources.NotoSerifGeorgian_SemiCondensed_Light
//import hambin.composeapp.generated.resources.NotoSerifGeorgian_SemiCondensed_Medium
//import hambin.composeapp.generated.resources.NotoSerifGeorgian_SemiCondensed_Regular
//import hambin.composeapp.generated.resources.NotoSerifGeorgian_SemiCondensed_SemiBold
//import hambin.composeapp.generated.resources.NotoSerifGeorgian_SemiCondensed_Thin
//import hambin.composeapp.generated.resources.NotoSerifGeorgian_Thin
import org.jetbrains.compose.resources.Font

@Composable
fun fontFamily() = FontFamily(
//    Font(Res.font.NotoSerifGeorgian_Bold),
//    Font(Res.font.NotoSerifGeorgian_Black),
//    Font(Res.font.NotoSerifGeorgian_ExtraLight),
//    Font(Res.font.NotoSerifGeorgian_SemiBold),
//    Font(Res.font.NotoSerifGeorgian_Thin),
//    Font(Res.font.NotoSerifGeorgian_Light),
//    Font(Res.font.NotoSerifGeorgian_ExtraBold),
//    Font(Res.font.NotoSerifGeorgian_Medium),
//    Font(Res.font.NotoSerifGeorgian_Regular),
//    Font(Res.font.NotoSerifGeorgian_Condensed_Black),
//    Font(Res.font.NotoSerifGeorgian_Condensed_Bold),
//    Font(Res.font.NotoSerifGeorgian_Condensed_ExtraLight),
//    Font(Res.font.NotoSerifGeorgian_Condensed_ExtraBold),
//    Font(Res.font.NotoSerifGeorgian_Condensed_Light),
//    Font(Res.font.NotoSerifGeorgian_Condensed_Medium),
//    Font(Res.font.NotoSerifGeorgian_Condensed_Regular),
//    Font(Res.font.NotoSerifGeorgian_Condensed_SemiBold),
//    Font(Res.font.NotoSerifGeorgian_Condensed_Thin),
//    Font(Res.font.NotoSerifGeorgian_ExtraCondensed_Bold),
//    Font(Res.font.NotoSerifGeorgian_ExtraCondensed_Black),
//    Font(Res.font.NotoSerifGeorgian_ExtraCondensed_ExtraBold),
//    Font(Res.font.NotoSerifGeorgian_ExtraCondensed_ExtraLight),
//    Font(Res.font.NotoSerifGeorgian_ExtraCondensed_Light),
//    Font(Res.font.NotoSerifGeorgian_ExtraCondensed_Medium),
//    Font(Res.font.NotoSerifGeorgian_ExtraCondensed_Regular),
//    Font(Res.font.NotoSerifGeorgian_ExtraCondensed_SemiBold),
//    Font(Res.font.NotoSerifGeorgian_ExtraCondensed_Thin),
//    Font(Res.font.NotoSerifGeorgian_SemiCondensed_Black),
//    Font(Res.font.NotoSerifGeorgian_SemiCondensed_Bold),
//    Font(Res.font.NotoSerifGeorgian_SemiCondensed_ExtraBold),
//    Font(Res.font.NotoSerifGeorgian_SemiCondensed_ExtraLight),
//    Font(Res.font.NotoSerifGeorgian_SemiCondensed_Light),
//    Font(Res.font.NotoSerifGeorgian_SemiCondensed_Medium),
//    Font(Res.font.NotoSerifGeorgian_SemiCondensed_Regular),
//    Font(Res.font.NotoSerifGeorgian_SemiCondensed_SemiBold),
//    Font(Res.font.NotoSerifGeorgian_SemiCondensed_Thin)
    Font(Res.font.Roboto_Bold),
    Font(Res.font.Roboto_Black),
    Font(Res.font.Roboto_BlackItalic),
    Font(Res.font.Roboto_BoldItalic),
    Font(Res.font.Roboto_Italic),
    Font(Res.font.Roboto_Light),
    Font(Res.font.Roboto_LightItalic),
    Font(Res.font.Roboto_Medium),
    Font(Res.font.Roboto_MediumItalic),
    Font(Res.font.Roboto_Regular),
    Font(Res.font.Roboto_Thin),
    Font(Res.font.Roboto_ThinItalic)
)

// Default Material 3 typography values
val baseline = Typography()

@Composable
fun AppTypography() = Typography().run {
    val fontFamily = fontFamily()
    copy(
        displayLarge = baseline.displayLarge.copy(fontFamily = fontFamily),
        displayMedium = baseline.displayMedium.copy(fontFamily = fontFamily),
        displaySmall = baseline.displaySmall.copy(fontFamily = fontFamily),
        headlineLarge = baseline.headlineLarge.copy(fontFamily = fontFamily),
        headlineMedium = baseline.headlineMedium.copy(fontFamily = fontFamily),
        headlineSmall = baseline.headlineSmall.copy(fontFamily = fontFamily),
        titleLarge = baseline.titleLarge.copy(fontFamily = fontFamily),
        titleMedium = baseline.titleMedium.copy(fontFamily = fontFamily),
        titleSmall = baseline.titleSmall.copy(fontFamily = fontFamily),
        bodyLarge = baseline.bodyLarge.copy(fontFamily = fontFamily),
        bodyMedium = baseline.bodyMedium.copy(fontFamily = fontFamily),
        bodySmall = baseline.bodySmall.copy(fontFamily = fontFamily),
        labelLarge = baseline.labelLarge.copy(fontFamily = fontFamily),
        labelMedium = baseline.labelMedium.copy(fontFamily = fontFamily),
        labelSmall = baseline.labelSmall.copy(fontFamily = fontFamily),
    )
}

