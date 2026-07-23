package top.roomio.app.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Roboto Flex is used by the browser prototype. SansSerif is the portable KMP
 * fallback until the approved variable font is added to shared resources.
 */
private val RoomioFontFamily = FontFamily.SansSerif

private fun roomioTextStyle(
    size: Int,
    lineHeight: Int,
    weight: FontWeight = FontWeight.Normal,
) = TextStyle(
    fontFamily = RoomioFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = 0.sp,
)

internal val RoomioTypography = Typography(
    displayLarge = roomioTextStyle(size = 57, lineHeight = 64, weight = FontWeight.SemiBold),
    displayMedium = roomioTextStyle(size = 45, lineHeight = 52, weight = FontWeight.SemiBold),
    displaySmall = roomioTextStyle(size = 36, lineHeight = 44, weight = FontWeight.SemiBold),
    headlineLarge = roomioTextStyle(size = 32, lineHeight = 40, weight = FontWeight.SemiBold),
    headlineMedium = roomioTextStyle(size = 28, lineHeight = 36, weight = FontWeight.SemiBold),
    headlineSmall = roomioTextStyle(size = 24, lineHeight = 32, weight = FontWeight.SemiBold),
    titleLarge = roomioTextStyle(size = 22, lineHeight = 28, weight = FontWeight.SemiBold),
    titleMedium = roomioTextStyle(size = 16, lineHeight = 24, weight = FontWeight.SemiBold),
    titleSmall = roomioTextStyle(size = 14, lineHeight = 20, weight = FontWeight.SemiBold),
    bodyLarge = roomioTextStyle(size = 16, lineHeight = 24),
    bodyMedium = roomioTextStyle(size = 14, lineHeight = 20),
    bodySmall = roomioTextStyle(size = 12, lineHeight = 16),
    labelLarge = roomioTextStyle(size = 14, lineHeight = 20, weight = FontWeight.SemiBold),
    labelMedium = roomioTextStyle(size = 12, lineHeight = 16, weight = FontWeight.SemiBold),
    labelSmall = roomioTextStyle(size = 11, lineHeight = 16, weight = FontWeight.SemiBold),
)
