package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SaqiDarkColorScheme = darkColorScheme(
    primary = SaqiCyan,
    onPrimary = Color.Black,
    primaryContainer = SaqiPurple,
    onPrimaryContainer = Color.White,
    secondary = SaqiMagenta,
    onSecondary = Color.Black,
    secondaryContainer = SaqiSurfaceVariant,
    onSecondaryContainer = SaqiCyan,
    tertiary = SaqiPinkGlow,
    onTertiary = Color.White,
    background = SaqiBackground,
    onBackground = SaqiTextPrimary,
    surface = SaqiSurface,
    onSurface = SaqiTextPrimary,
    surfaceVariant = SaqiSurfaceVariant,
    onSurfaceVariant = SaqiTextSecondary
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SaqiDarkColorScheme,
        typography = Typography,
        content = content
    )
}
