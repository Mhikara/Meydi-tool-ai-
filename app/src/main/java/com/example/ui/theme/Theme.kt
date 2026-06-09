package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CyberColorScheme = darkColorScheme(
    primary = NeonTeal,
    secondary = NeonPurple,
    tertiary = NeonMagenta,
    background = ObsidianBg,
    surface = MidnightSurface,
    onBackground = Color.White,
    onSurface = Color.White,
    onPrimary = Color(0xFF070714),
    onSecondary = Color.White,
    onTertiary = Color.White,
    outline = DarkStroke
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force modern tech dark-theme for video coding studio UI
    dynamicColor: Boolean = false, // Disable dynamic colors to keep branding consistent
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = CyberColorScheme,
        typography = Typography,
        content = content
    )
}
