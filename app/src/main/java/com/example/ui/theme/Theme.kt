package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = OnCyberCyan,
    primaryContainer = CyberCyanContainer,
    onPrimaryContainer = CyberCyan,
    secondary = EnterpriseBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF152A4A),
    onSecondaryContainer = Color(0xFF90CAF9),
    tertiary = ComplianceGreen,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF00381B),
    onTertiaryContainer = ComplianceGreen,
    background = EnterpriseObsidian,
    onBackground = TextPrimaryDark,
    surface = EnterpriseSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = EnterpriseSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = EnterpriseCardBorder,
    error = CriticalRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = CyberCyanDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD0F8FD),
    onPrimaryContainer = Color(0xFF002025),
    secondary = EnterpriseBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6E4FF),
    onSecondaryContainer = Color(0xFF001B3E),
    tertiary = Color(0xFF008947),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFB4F2CB),
    onTertiaryContainer = Color(0xFF00210E),
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    error = CriticalRed,
    onError = Color.White
)

@Composable
fun DroidCommandTheme(
    darkTheme: Boolean = true, // Enterprise consoles look optimal in dark mode by default
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
