package com.daymark.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.isSystemInDarkTheme

private val Ink = Color(0xFF23253A)
private val MutedInk = Color(0xFF777A91)
private val CanvasLight = Color(0xFFF6F6FB)
private val CardLight = Color(0xFFFFFFFF)
private val CanvasDark = Color(0xFF15151F)
private val CardDark = Color(0xFF20202C)
private val TextDark = Color(0xFFF1F0F8)
private val MutedDark = Color(0xFFB4B2C2)

private data class Accent(val light: Color, val dark: Color, val secondary: Color)
private val accents = mapOf(
    "LIGHT" to Accent(Color(0xFF7467D8), Color(0xFFA99BFF), Color(0xFF6D879A)),
    "LAVENDER" to Accent(Color(0xFF7A5CE1), Color(0xFFB4A0FF), Color(0xFF8C78C5)),
    "OCEAN" to Accent(Color(0xFF397EA5), Color(0xFF83C5E5), Color(0xFF6D9CAF)),
    "SAGE" to Accent(Color(0xFF4C8977), Color(0xFF8BCBB4), Color(0xFF719A88)),
    "ROSE" to Accent(Color(0xFFB9688C), Color(0xFFE8A3BF), Color(0xFFAD8295)),
    "MONOCHROME" to Accent(Color(0xFF525B70), Color(0xFFBCC4D6), Color(0xFF838B9E))
)

private val daymarkTypography = Typography(
    headlineLarge = androidx.compose.ui.text.TextStyle(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
    headlineMedium = androidx.compose.ui.text.TextStyle(fontSize = 25.sp, lineHeight = 31.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.35).sp),
    headlineSmall = androidx.compose.ui.text.TextStyle(fontSize = 21.sp, lineHeight = 27.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
    titleLarge = androidx.compose.ui.text.TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    bodyLarge = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, lineHeight = 23.sp, fontWeight = FontWeight.Normal),
    bodyMedium = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal),
    bodySmall = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, lineHeight = 17.sp, fontWeight = FontWeight.Normal),
    labelLarge = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = androidx.compose.ui.text.TextStyle(fontSize = 10.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
)

@Composable
fun DaymarkTheme(themeKey: String, content: @Composable () -> Unit) {
    val key = themeKey.uppercase()
    val systemDark = isSystemInDarkTheme()
    val dark = key == "DARK" || (key == "SYSTEM" && systemDark)
    val accent = accents[key] ?: accents.getValue("LIGHT")
    val primary = if (dark) accent.dark else accent.light
    val colors = if (dark) {
        darkColorScheme(
            primary = primary,
            onPrimary = Color(0xFF211E33),
            secondary = accent.secondary.lightnessScale(1.18f),
            onSecondary = Color(0xFF111820),
            tertiary = Color(0xFF84C7B6),
            background = CanvasDark,
            onBackground = TextDark,
            surface = CardDark,
            onSurface = TextDark,
            surfaceVariant = Color(0xFF292938),
            onSurfaceVariant = MutedDark,
            outline = Color(0xFF3D3D4E),
            outlineVariant = Color(0xFF30303E),
            error = Color(0xFFFF929C)
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            secondary = accent.secondary,
            onSecondary = Color.White,
            tertiary = Color(0xFF72A99C),
            background = CanvasLight,
            onBackground = Ink,
            surface = CardLight,
            onSurface = Ink,
            surfaceVariant = Color(0xFFF0EFF7),
            onSurfaceVariant = MutedInk,
            outline = Color(0xFFE1E0EA),
            outlineVariant = Color(0xFFEAE9F0),
            error = Color(0xFFB84F64)
        )
    }
    MaterialTheme(colorScheme = colors, typography = daymarkTypography, content = content)
}

private fun Color.lightnessScale(factor: Float): Color = Color(
    red = (red * factor).coerceAtMost(1f),
    green = (green * factor).coerceAtMost(1f),
    blue = (blue * factor).coerceAtMost(1f),
    alpha = alpha
)
