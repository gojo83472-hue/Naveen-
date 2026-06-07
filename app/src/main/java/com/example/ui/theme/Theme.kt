package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = NeonCyan,
    secondary = NeonPink,
    tertiary = BrightViolet,
    background = ObsidianBlack,
    surface = SurfaceDark,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    primaryContainer = SurfaceCard,
    secondaryContainer = GridBorder,
    onPrimary = ObsidianBlack,
    onSecondary = ObsidianBlack
  )

private val LightColorScheme = DarkColorScheme // Uskha is dark mode only for high-end luxury vibe!

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark mode
  dynamicColor: Boolean = false, // Use our handcrafted design system for consistent premium styling
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
