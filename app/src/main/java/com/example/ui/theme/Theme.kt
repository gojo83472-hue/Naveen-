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

private val LightColorScheme =
  lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF006064),
    secondary = androidx.compose.ui.graphics.Color(0xFFC2185B),
    tertiary = androidx.compose.ui.graphics.Color(0xFF7B1FA2),
    background = androidx.compose.ui.graphics.Color(0xFFF5F5F7),
    surface = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    onBackground = androidx.compose.ui.graphics.Color(0xFF13131A),
    onSurface = androidx.compose.ui.graphics.Color(0xFF13131A),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFE0F7FA),
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFFFCE4EC),
    onPrimary = androidx.compose.ui.graphics.Color.White,
    onSecondary = androidx.compose.ui.graphics.Color.White
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
