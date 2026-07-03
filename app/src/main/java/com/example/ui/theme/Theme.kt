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

private val DarkColorScheme =
  darkColorScheme(
    primary = BrandViolet,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF2E1065),
    onPrimaryContainer = Color(0xFFEDE9FE),
    secondary = BrandCyan,
    onSecondary = Color(0xFF083344),
    secondaryContainer = Color(0xFF083344),
    onSecondaryContainer = Color(0xFFECFEFF),
    tertiary = BrandPink,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF500724),
    onTertiaryContainer = Color(0xFFFCE7F3),
    background = DarkBgStart,
    onBackground = Color(0xFFF1F5F9),
    surface = DarkSurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = DarkSurfaceCard,
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF475569)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = BrandViolet,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = Color(0xFF4C1D95),
    secondary = BrandCyan,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFECFEFF),
    onSecondaryContainer = Color(0xFF164E63),
    tertiary = BrandPink,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFCE7F3),
    onTertiaryContainer = Color(0xFF500724),
    background = LightBgStart,
    onBackground = Color(0xFF0F172A),
    surface = LightSurfaceCard,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = LightBgMid,
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFF94A3B8)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  themeAccent: String = "Default Violet",
  // Disable dynamic color by default for cohesive glassmorphism branding
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  // Sync brand colors first based on user selected theme accent
  when (themeAccent) {
      "Ocean Blue" -> {
          BrandViolet = Color(0xFF0EA5E9)
          BrandCyan = Color(0xFF6366F1)
          BrandPink = Color(0xFF06B6D4)
      }
      "Forest Green" -> {
          BrandViolet = Color(0xFF10B981)
          BrandCyan = Color(0xFFF59E0B)
          BrandPink = Color(0xFF06B6D4)
      }
      "Sunset Orange" -> {
          BrandViolet = Color(0xFFF97316)
          BrandCyan = Color(0xFFF59E0B)
          BrandPink = Color(0xFFEF4444)
      }
      "Crimson Red" -> {
          BrandViolet = Color(0xFFEF4444)
          BrandCyan = Color(0xFFEC4899)
          BrandPink = Color(0xFFF97316)
      }
      else -> { // "Default Violet"
          BrandViolet = Color(0xFF7C3AED)
          BrandCyan = Color(0xFF06B6D4)
          BrandPink = Color(0xFFEC4899)
      }
  }

  // Dynamically build the color scheme based on current BrandViolet, BrandCyan, etc.
  val activeDarkScheme = darkColorScheme(
    primary = BrandViolet,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = BrandViolet.copy(alpha = 0.15f),
    onPrimaryContainer = Color(0xFFEDE9FE),
    secondary = BrandCyan,
    onSecondary = Color(0xFF083344),
    secondaryContainer = Color(0xFF083344),
    onSecondaryContainer = Color(0xFFECFEFF),
    tertiary = BrandPink,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF500724),
    onTertiaryContainer = Color(0xFFFCE7F3),
    background = DarkBgStart,
    onBackground = Color(0xFFF1F5F9),
    surface = DarkSurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = DarkSurfaceCard,
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF475569)
  )

  val activeLightScheme = lightColorScheme(
    primary = BrandViolet,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = BrandViolet.copy(alpha = 0.15f),
    onPrimaryContainer = Color(0xFF4C1D95),
    secondary = BrandCyan,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFECFEFF),
    onSecondaryContainer = Color(0xFF164E63),
    tertiary = BrandPink,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFCE7F3),
    onTertiaryContainer = Color(0xFF500724),
    background = LightBgStart,
    onBackground = Color(0xFF0F172A),
    surface = LightSurfaceCard,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = LightBgMid,
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFF94A3B8)
  )

  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> activeDarkScheme
      else -> activeLightScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
