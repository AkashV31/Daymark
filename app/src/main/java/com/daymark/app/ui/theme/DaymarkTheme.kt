package com.daymark.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsControllerCompat

private val Ink = Color(0xFF23253A)
private val MutedInk = Color(0xFF777A91)
private val CanvasLight = Color(0xFFF6F6FB)
private val CardLight = Color(0xFFFFFFFF)
private val CanvasDark = Color(0xFF15151F)
private val CardDark = Color(0xFF20202C)
private val TextDark = Color(0xFFF1F0F8)
private val MutedDark = Color(0xFFB4B2C2)

data class ThemeAccentInfo(val id: String, val name: String, val previewColor: Color)

data class Accent(
    val light: Color,
    val dark: Color,
    val secondary: Color,
    val canvasLight: Color = CanvasLight,
    val surfaceLight: Color = CardLight,
    val surfaceVariantLight: Color = Color(0xFFF0EFF7),
    val outlineLight: Color = Color(0xFFE1E0EA),
    val canvasDark: Color = CanvasDark,
    val surfaceDark: Color = CardDark,
    val surfaceVariantDark: Color = Color(0xFF292938),
    val outlineDark: Color = Color(0xFF3D3D4E)
)

val accents: Map<String, Accent> = mapOf(
    "LIGHT" to Accent(
        light = Color(0xFF7467D8), dark = Color(0xFFA99BFF), secondary = Color(0xFF6D879A)
    ),
    "LAVENDER" to Accent(
        light = Color(0xFF7A5CE1), dark = Color(0xFFB4A0FF), secondary = Color(0xFF8C78C5),
        canvasLight = Color(0xFFF7F5FC), surfaceVariantLight = Color(0xFFEFEBF9),
        canvasDark = Color(0xFF161421), surfaceDark = Color(0xFF221F32), surfaceVariantDark = Color(0xFF2C2740)
    ),
    "OCEAN" to Accent(
        light = Color(0xFF2B78A0), dark = Color(0xFF76C4EB), secondary = Color(0xFF5B92A7),
        canvasLight = Color(0xFFF3F7FA), surfaceVariantLight = Color(0xFFE5EEF4), outlineLight = Color(0xFFD6E3EC),
        canvasDark = Color(0xFF11171E), surfaceDark = Color(0xFF1A242E), surfaceVariantDark = Color(0xFF22303D), outlineDark = Color(0xFF2F4254)
    ),
    "SAGE" to Accent(
        light = Color(0xFF3D7C6B), dark = Color(0xFF7EC5B0), secondary = Color(0xFF618E80),
        canvasLight = Color(0xFFF3F7F5), surfaceVariantLight = Color(0xFFE6EFEA), outlineLight = Color(0xFFD7E5DC),
        canvasDark = Color(0xFF121815), surfaceDark = Color(0xFF1B2521), surfaceVariantDark = Color(0xFF23322C), outlineDark = Color(0xFF31453D)
    ),
    "ROSE" to Accent(
        light = Color(0xFFB3577D), dark = Color(0xFFF09BBF), secondary = Color(0xFFA67187),
        canvasLight = Color(0xFFFAF4F6), surfaceVariantLight = Color(0xFFF6E8EE), outlineLight = Color(0xFFECDAE2),
        canvasDark = Color(0xFF1B1216), surfaceDark = Color(0xFF291B21), surfaceVariantDark = Color(0xFF38232C), outlineDark = Color(0xFF4C303C)
    ),
    "MONOCHROME" to Accent(
        light = Color(0xFF4A5568), dark = Color(0xFFB0BDD4), secondary = Color(0xFF718096),
        canvasLight = Color(0xFFF7FAFC), surfaceVariantLight = Color(0xFFEDF2F7), outlineLight = Color(0xFFE2E8F0),
        canvasDark = Color(0xFF111317), surfaceDark = Color(0xFF1A1D23), surfaceVariantDark = Color(0xFF242831), outlineDark = Color(0xFF323844)
    ),
    "EMBER" to Accent(
        light = Color(0xFFC75D2C), dark = Color(0xFFFF9D70), secondary = Color(0xFFD3835B),
        canvasLight = Color(0xFFFAF5F2), surfaceVariantLight = Color(0xFFF6EAE3), outlineLight = Color(0xFFEBD8CE),
        canvasDark = Color(0xFF1C1410), surfaceDark = Color(0xFF2B1E18), surfaceVariantDark = Color(0xFF3B2A22), outlineDark = Color(0xFF50392E)
    ),
    "MIDNIGHT" to Accent(
        light = Color(0xFF3949AB), dark = Color(0xFF8C9EFF), secondary = Color(0xFF5C6BC0),
        canvasLight = Color(0xFFF3F5FA), surfaceVariantLight = Color(0xFFE6EBF5), outlineLight = Color(0xFFD7DFEE),
        canvasDark = Color(0xFF0C101A), surfaceDark = Color(0xFF141A29), surfaceVariantDark = Color(0xFF1C2438), outlineDark = Color(0xFF29354F)
    ),
    "MOCHA" to Accent(
        light = Color(0xFF795548), dark = Color(0xFFBCAAA4), secondary = Color(0xFF8D6E63),
        canvasLight = Color(0xFFF7F5F4), surfaceVariantLight = Color(0xFFEFEBE9), outlineLight = Color(0xFFDED8D5),
        canvasDark = Color(0xFF161312), surfaceDark = Color(0xFF231E1C), surfaceVariantDark = Color(0xFF312B28), outlineDark = Color(0xFF433B37)
    ),
    "AURORA" to Accent(
        light = Color(0xFF197669), dark = Color(0xFF54D2BE), secondary = Color(0xFF7B52C9),
        canvasLight = Color(0xFFF2F7F6), surfaceVariantLight = Color(0xFFE4F0EE), outlineLight = Color(0xFFD3E5E2),
        canvasDark = Color(0xFF0E1716), surfaceDark = Color(0xFF152422), surfaceVariantDark = Color(0xFF1E322F), outlineDark = Color(0xFF294541)
    )
)

val allAccents: List<ThemeAccentInfo> by lazy {
    buildList {
        add(ThemeAccentInfo("LIGHT", "Indigo", Color(0xFF7467D8)))
        add(ThemeAccentInfo("LAVENDER", "Lavender", Color(0xFF7A5CE1)))
        add(ThemeAccentInfo("OCEAN", "Ocean", Color(0xFF2B78A0)))
        add(ThemeAccentInfo("SAGE", "Sage", Color(0xFF3D7C6B)))
        add(ThemeAccentInfo("ROSE", "Rose", Color(0xFFB3577D)))
        add(ThemeAccentInfo("MONOCHROME", "Monochrome", Color(0xFF4A5568)))
        add(ThemeAccentInfo("EMBER", "Ember", Color(0xFFC75D2C)))
        add(ThemeAccentInfo("MIDNIGHT", "Midnight", Color(0xFF3949AB)))
        add(ThemeAccentInfo("MOCHA", "Mocha", Color(0xFF795548)))
        add(ThemeAccentInfo("AURORA", "Aurora", Color(0xFF197669)))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(ThemeAccentInfo("DYNAMIC", "Dynamic (System)", Color(0xFF6750A4)))
        }
    }
}

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
fun DaymarkTheme(
    themeKey: String = "SYSTEM",
    accentKey: String = "LIGHT",
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val rawMode = themeKey.uppercase()
    val isLegacyAccent = rawMode in accents.keys && rawMode !in setOf("LIGHT", "DARK", "SYSTEM")
    val mode = if (isLegacyAccent) "SYSTEM" else rawMode
    val activeAccentKey = if (isLegacyAccent) rawMode else accentKey.uppercase()

    val systemDark = isSystemInDarkTheme()
    val dark = mode == "DARK" || (mode == "SYSTEM" && systemDark)

    val targetColors: ColorScheme = if (activeAccentKey == "DYNAMIC" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        val accent = accents[activeAccentKey] ?: accents.getValue("LIGHT")
        val primary = if (dark) accent.dark else accent.light
        if (dark) {
            darkColorScheme(
                primary = primary,
                onPrimary = Color(0xFF141221),
                secondary = accent.secondary.lightnessScale(1.18f),
                onSecondary = Color(0xFF111820),
                tertiary = Color(0xFF84C7B6),
                background = accent.canvasDark,
                onBackground = TextDark,
                surface = accent.surfaceDark,
                onSurface = TextDark,
                surfaceVariant = accent.surfaceVariantDark,
                onSurfaceVariant = MutedDark,
                outline = accent.outlineDark,
                outlineVariant = accent.outlineDark.copy(alpha = 0.6f),
                error = Color(0xFFFF929C)
            )
        } else {
            lightColorScheme(
                primary = primary,
                onPrimary = Color.White,
                secondary = accent.secondary,
                onSecondary = Color.White,
                tertiary = Color(0xFF72A99C),
                background = accent.canvasLight,
                onBackground = Ink,
                surface = accent.surfaceLight,
                onSurface = Ink,
                surfaceVariant = accent.surfaceVariantLight,
                onSurfaceVariant = MutedInk,
                outline = accent.outlineLight,
                outlineVariant = accent.outlineLight.copy(alpha = 0.7f),
                error = Color(0xFFB84F64)
            )
        }
    }

    // Animate theme transitions smoothly across colorScheme tokens
    val animSpec = tween<Color>(durationMillis = 220)
    val animatedPrimary = animateColorAsState(targetColors.primary, animSpec, label = "themePrimary").value
    val animatedBackground = animateColorAsState(targetColors.background, animSpec, label = "themeBackground").value
    val animatedSurface = animateColorAsState(targetColors.surface, animSpec, label = "themeSurface").value
    val animatedSurfaceVariant = animateColorAsState(targetColors.surfaceVariant, animSpec, label = "themeSurfaceVariant").value
    val animatedOutline = animateColorAsState(targetColors.outline, animSpec, label = "themeOutline").value

    val animatedColors = targetColors.copy(
        primary = animatedPrimary,
        background = animatedBackground,
        surface = animatedSurface,
        surfaceVariant = animatedSurfaceVariant,
        outline = animatedOutline
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowInsetsControllerCompat(window, view)
                insetsController.isAppearanceLightStatusBars = !dark
                insetsController.isAppearanceLightNavigationBars = !dark
            }
        }
    }

    MaterialTheme(colorScheme = animatedColors, typography = daymarkTypography, content = content)
}

private fun Color.lightnessScale(factor: Float): Color = Color(
    red = (red * factor).coerceAtMost(1f),
    green = (green * factor).coerceAtMost(1f),
    blue = (blue * factor).coerceAtMost(1f),
    alpha = alpha
)
