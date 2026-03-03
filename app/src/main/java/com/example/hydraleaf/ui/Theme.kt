package com.example.hydraleaf.ui

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// Nature-teal palette
val Teal50  = Color(0xFFE0F7F1)
val Teal100 = Color(0xFFB2EBD6)
val Teal200 = Color(0xFF80DFBB)
val Teal400 = Color(0xFF26C596)
val Teal600 = Color(0xFF00A87B)
val Teal700 = Color(0xFF009A6E)
val Teal900 = Color(0xFF00634A)

val NightSurface   = Color(0xFF0A1E1A)
val NightOnSurface = Color(0xFFE0E0E0)
val NightCard      = Color(0xFF12302A)
val NightVariant   = Color(0xFF1C4038)

private val DarkColorScheme = darkColorScheme(
    primary         = Teal400,
    onPrimary       = Color.Black,
    primaryContainer = Teal900,
    secondary       = Teal200,
    background      = NightSurface,
    surface         = NightCard,
    surfaceVariant  = NightVariant,
    onBackground    = NightOnSurface,
    onSurface       = NightOnSurface,
    onSurfaceVariant = Color(0xFFB0CFC7)
)

private val LightColorScheme = lightColorScheme(
    primary         = Teal600,
    onPrimary       = Color.White,
    primaryContainer = Teal100,
    secondary       = Teal700,
    background      = Color(0xFFF4FBF8),
    surface         = Color.White,
    surfaceVariant  = Teal50,
    onBackground    = Color(0xFF1C1B1F),
    onSurface       = Color(0xFF1C1B1F),
    onSurfaceVariant = Color(0xFF4A635C)
)

private val HydraTypography = Typography(
    headlineLarge = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Black, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp),
    titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp),
    titleMedium = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal, lineHeight = 16.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp)
)

@Composable
fun HydraLeafTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Black.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = HydraTypography,
        content = content
    )
}
