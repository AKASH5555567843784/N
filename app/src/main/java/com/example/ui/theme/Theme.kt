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
    primary = Color(0xFF06B6D4), // Glowing Cyan
    secondary = Color(0xFF10B981), // Electric Teal
    tertiary = Color(0xFF6366F1), // Cyberpunk Indigo
    background = Color(0xFF030712), // Deep Space Dark
    surface = Color(0xFF0E1626), // Steel Dark Glass
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color(0xFFE2E8F0),
    onSurface = Color(0xFFF1F5F9)
)

private val LightColorScheme = DarkColorScheme // Always force elegant dark Jarvis HUD as requested

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force premium dark mode
  dynamicColor: Boolean = false, // Keep themed colors consistent
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = DarkColorScheme,
    typography = Typography,
    content = content
  )
}
