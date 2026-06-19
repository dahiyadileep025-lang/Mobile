package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = TubeRed,
    secondary = AccentCrimson,
    tertiary = GoogleBlue,
    background = DarkCharcoal,
    surface = SurfaceGray,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF2E2E2E),
    onSurfaceVariant = Color(0xFFCCCCCC),
    outline = BorderGray
)

private val LightColorScheme = lightColorScheme(
    primary = Red40,
    secondary = DarkAccent40,
    tertiary = GoogleBlue,
    background = Color(0xFFF9F9F9),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF0F0F0F),
    onSurface = Color(0xFF1F1F1F),
    surfaceVariant = Color(0xFFECECEC),
    onSurfaceVariant = Color(0xFF6F6F6F),
    outline = Color(0xFFE1E1E1)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color support
    dynamicColor: Boolean = false, // Disable dynamic colors by default to enforce our awesome branding!
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> DarkColorScheme // Let's enforce beautiful Dark mode by default for video streaming apps!
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
