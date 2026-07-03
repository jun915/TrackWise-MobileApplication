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

fun getThemeGradientColors(themeAccent: String, darkTheme: Boolean): List<Color> {
    return if (darkTheme) {
        when (themeAccent) {
            "Ocean Blue" -> listOf(Color(0xFF03395C), Color(0xFF021727), Color(0xFF01070F))
            "Forest Green" -> listOf(Color(0xFF063A25), Color(0xFF021B10), Color(0xFF010704))
            "Sunset Orange" -> listOf(Color(0xFF4A1A05), Color(0xFF210901), Color(0xFF080200))
            "Crimson Red" -> listOf(Color(0xFF450614), Color(0xFF1C0106), Color(0xFF070001))
            else -> listOf(Color(0xFF1D0B33), Color(0xFF0C0418), Color(0xFF03010A)) // Default Violet
        }
    } else {
        when (themeAccent) {
            "Ocean Blue" -> listOf(Color(0xFFBAE6FD), Color(0xFFF0F9FF), Color(0xFFFFFFFF))
            "Forest Green" -> listOf(Color(0xFFA7F3D0), Color(0xFFECFDF5), Color(0xFFFFFFFF))
            "Sunset Orange" -> listOf(Color(0xFFFFEDD5), Color(0xFFFFF7ED), Color(0xFFFFFFFF))
            "Crimson Red" -> listOf(Color(0xFFFECDD3), Color(0xFFFFF1F2), Color(0xFFFFFFFF))
            else -> listOf(Color(0xFFE9D5FF), Color(0xFFF5F3FF), Color(0xFFFFFFFF)) // Default Violet
        }
    }
}

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  themeAccent: String = "Default Violet",
  // Disable dynamic color by default for cohesive glassmorphism branding
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  var darkBg = DarkBgStart
  var darkSurface = DarkSurface
  var darkSurfaceCard = DarkSurfaceCard

  var lightBg = LightBgStart
  var lightSurfaceCard = LightSurfaceCard
  var lightBgMid = LightBgMid

  // Sync brand colors first based on user selected theme accent
  when (themeAccent) {
      "Ocean Blue" -> {
          BrandViolet = Color(0xFF0EA5E9)
          BrandCyan = Color(0xFF6366F1)
          BrandPink = Color(0xFF06B6D4)
          darkBg = Color(0xFF050B14)
          darkSurface = Color(0xFF091424)
          darkSurfaceCard = Color(0xFF0F1E35)
          lightBg = Color(0xFFF0F5FA)
          lightSurfaceCard = Color(0xFFFFFFFF)
          lightBgMid = Color(0xFFE1EDF7)
      }
      "Forest Green" -> {
          BrandViolet = Color(0xFF10B981)
          BrandCyan = Color(0xFFF59E0B)
          BrandPink = Color(0xFF06B6D4)
          darkBg = Color(0xFF040A08)
          darkSurface = Color(0xFF081410)
          darkSurfaceCard = Color(0xFF0E221B)
          lightBg = Color(0xFFF0F5F2)
          lightSurfaceCard = Color(0xFFFFFFFF)
          lightBgMid = Color(0xFFE2EDE5)
      }
      "Sunset Orange" -> {
          BrandViolet = Color(0xFFF97316)
          BrandCyan = Color(0xFFF59E0B)
          BrandPink = Color(0xFFEF4444)
          darkBg = Color(0xFF0C0805)
          darkSurface = Color(0xFF160F0A)
          darkSurfaceCard = Color(0xFF241810)
          lightBg = Color(0xFFFAF5F0)
          lightSurfaceCard = Color(0xFFFFFFFF)
          lightBgMid = Color(0xFFF5EBE0)
      }
      "Crimson Red" -> {
          BrandViolet = Color(0xFFEF4444)
          BrandCyan = Color(0xFFEC4899)
          BrandPink = Color(0xFFF97316)
          darkBg = Color(0xFF0C0507)
          darkSurface = Color(0xFF160A0D)
          darkSurfaceCard = Color(0xFF241014)
          lightBg = Color(0xFFFAF0F2)
          lightSurfaceCard = Color(0xFFFFFFFF)
          lightBgMid = Color(0xFFF5E0E4)
      }
      else -> { // "Default Violet"
          BrandViolet = Color(0xFF7C3AED)
          BrandCyan = Color(0xFF06B6D4)
          BrandPink = Color(0xFFEC4899)
          darkBg = DarkBgStart
          darkSurface = DarkSurface
          darkSurfaceCard = DarkSurfaceCard
          lightBg = LightBgStart
          lightSurfaceCard = LightSurfaceCard
          lightBgMid = LightBgMid
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
    background = darkBg,
    onBackground = Color(0xFFF1F5F9),
    surface = darkSurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = darkSurfaceCard,
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
    background = lightBg,
    onBackground = Color(0xFF0F172A),
    surface = lightSurfaceCard,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = lightBgMid,
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
