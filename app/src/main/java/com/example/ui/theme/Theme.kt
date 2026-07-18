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
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

fun createTypography(fontSize: String, fontStyle: String): Typography {
    val multiplier = when (fontSize) {
        "Small" -> 0.85f
        "Large" -> 1.2f
        else -> 1.0f // Medium
    }

    val family = when (fontStyle) {
        "Sans Serif" -> FontFamily.SansSerif
        "Serif" -> FontFamily.Serif
        "Monospace" -> FontFamily.Monospace
        "Cursive" -> FontFamily.Cursive
        else -> FontFamily.Default
    }

    return Typography(
        displayLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Normal,
            fontSize = (57 * multiplier).sp,
            lineHeight = (64 * multiplier).sp,
            letterSpacing = (-0.25).sp
        ),
        displayMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Normal,
            fontSize = (45 * multiplier).sp,
            lineHeight = (52 * multiplier).sp,
            letterSpacing = 0.sp
        ),
        displaySmall = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Normal,
            fontSize = (36 * multiplier).sp,
            lineHeight = (44 * multiplier).sp,
            letterSpacing = 0.sp
        ),
        headlineLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Normal,
            fontSize = (32 * multiplier).sp,
            lineHeight = (40 * multiplier).sp,
            letterSpacing = 0.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Normal,
            fontSize = (28 * multiplier).sp,
            lineHeight = (36 * multiplier).sp,
            letterSpacing = 0.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Normal,
            fontSize = (24 * multiplier).sp,
            lineHeight = (32 * multiplier).sp,
            letterSpacing = 0.sp
        ),
        titleLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Normal,
            fontSize = (22 * multiplier).sp,
            lineHeight = (28 * multiplier).sp,
            letterSpacing = 0.sp
        ),
        titleMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Medium,
            fontSize = (16 * multiplier).sp,
            lineHeight = (24 * multiplier).sp,
            letterSpacing = 0.15.sp
        ),
        titleSmall = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Medium,
            fontSize = (14 * multiplier).sp,
            lineHeight = (20 * multiplier).sp,
            letterSpacing = 0.1.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Normal,
            fontSize = (16 * multiplier).sp,
            lineHeight = (24 * multiplier).sp,
            letterSpacing = 0.5.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Normal,
            fontSize = (14 * multiplier).sp,
            lineHeight = (20 * multiplier).sp,
            letterSpacing = 0.25.sp
        ),
        bodySmall = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Normal,
            fontSize = (12 * multiplier).sp,
            lineHeight = (16 * multiplier).sp,
            letterSpacing = 0.4.sp
        ),
        labelLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Medium,
            fontSize = (14 * multiplier).sp,
            lineHeight = (20 * multiplier).sp,
            letterSpacing = 0.1.sp
        ),
        labelMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Medium,
            fontSize = (12 * multiplier).sp,
            lineHeight = (16 * multiplier).sp,
            letterSpacing = 0.5.sp
        ),
        labelSmall = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Medium,
            fontSize = (11 * multiplier).sp,
            lineHeight = (16 * multiplier).sp,
            letterSpacing = 0.5.sp
        )
    )
}

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
            "Ocean Blue" -> listOf(Color(0xFF0F172A), Color(0xFF0B1120), Color(0xFF070A13))
            "Forest Green" -> listOf(Color(0xFF063A25), Color(0xFF021B10), Color(0xFF010704))
            "Sunset Orange" -> listOf(Color(0xFF4A1A05), Color(0xFF210901), Color(0xFF080200))
            "Crimson Red" -> listOf(Color(0xFF450614), Color(0xFF1C0106), Color(0xFF070001))
            else -> listOf(Color(0xFF1D0B33), Color(0xFF0C0418), Color(0xFF03010A)) // Default Violet
        }
    } else {
        when (themeAccent) {
            "Ocean Blue" -> listOf(Color(0xFFCBD5E1), Color(0xFFF1F5F9), Color(0xFFFFFFFF))
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
  fontSize: String = "Medium",
  fontStyle: String = "Default",
  // Disable dynamic color by default for cohesive glassmorphism branding
  dynamicColor: Boolean = false,
  bgType: String = "none",
  bgColorName: String = "",
  bgGradientName: String = "",
  bgImageName: String = "",
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
          BrandViolet = Color(0xFF6366F1) // Indigo/Blue from Amanah
          BrandCyan = Color(0xFF0EA5E9)   // Bright sky blue
          BrandPink = Color(0xFF10B981)   // Emerald green
          darkBg = Color(0xFF090D16)      // Deep slate dark
          darkSurface = Color(0xFF0F172A) // Slate 900
          darkSurfaceCard = Color(0xFF1E293B) // Slate 800
          lightBg = Color(0xFFF1F5F9)     // Slate 100
          lightSurfaceCard = Color(0xFFFFFFFF)
          lightBgMid = Color(0xFFE2E8F0)  // Slate 200
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

  // Override colors based on active background selection to adapt tiles/icons dynamically
  if (bgType == "color") {
      val name = bgColorName.lowercase()
      if (name.contains("lavender") || name.contains("purple") || name.contains("plum")) {
          BrandViolet = Color(0xFF8B5CF6) // Royal Lavender
          BrandCyan = Color(0xFFD946EF)   // Pink/Fuchsia
          BrandPink = Color(0xFF06B6D4)   // Teal/Cyan
      } else if (name.contains("sky") || name.contains("ocean") || name.contains("periwinkle")) {
          BrandViolet = Color(0xFF2563EB) // Sky Blue
          BrandCyan = Color(0xFF06B6D4)   // Electric Cyan
          BrandPink = Color(0xFF6366F1)   // Violet/Indigo
      } else if (name.contains("mint") || name.contains("forest") || name.contains("sage") || name.contains("olive")) {
          BrandViolet = Color(0xFF10B981) // Emerald Green
          BrandCyan = Color(0xFF14B8A6)   // Teal
          BrandPink = Color(0xFFF59E0B)   // Amber
      } else if (name.contains("rose") || name.contains("burgundy") || name.contains("crimson")) {
          BrandViolet = Color(0xFFEF4444) // Vibrant Red
          BrandCyan = Color(0xFFEC4899)   // Hot Pink
          BrandPink = Color(0xFFF97316)   // Orange
      } else if (name.contains("amber") || name.contains("rust") || name.contains("peach") || name.contains("terracotta") || name.contains("coral") || name.contains("gold") || name.contains("bronze")) {
          BrandViolet = Color(0xFFF97316) // Energetic Orange
          BrandCyan = Color(0xFFF59E0B)   // Vivid Amber
          BrandPink = Color(0xFFEF4444)   // Red
      } else if (name.contains("teal") || name.contains("marine")) {
          BrandViolet = Color(0xFF0D9488) // Deep Teal
          BrandCyan = Color(0xFF0891B2)   // Cyan
          BrandPink = Color(0xFF059669)   // Green
      }
  } else if (bgType == "gradient") {
      val name = bgGradientName.lowercase()
      if (name.contains("sunset")) {
          BrandViolet = Color(0xFFF97316) // Sunset Orange
          BrandCyan = Color(0xFFEF4444)   // Red
          BrandPink = Color(0xFFF59E0B)   // Amber
      } else if (name.contains("ocean")) {
          BrandViolet = Color(0xFF0284C7) // Ocean Blue
          BrandCyan = Color(0xFF0891B2)   // Cyan
          BrandPink = Color(0xFF0D9488)   // Teal
      } else if (name.contains("emerald") || name.contains("forest")) {
          BrandViolet = Color(0xFF059669) // Emerald Green
          BrandCyan = Color(0xFF10B981)   // Bright Green
          BrandPink = Color(0xFF84CC16)   // Lime
      } else if (name.contains("aurora")) {
          BrandViolet = Color(0xFF7C3AED) // Royal Violet
          BrandCyan = Color(0xFF6366F1)   // Electric Indigo
          BrandPink = Color(0xFFEC4899)   // Hot Pink
      } else if (name.contains("cosmic")) {
          BrandViolet = Color(0xFF8B5CF6) // Violet
          BrandCyan = Color(0xFFD946EF)   // Fuchsia
          BrandPink = Color(0xFFEF4444)   // Red
      } else if (name.contains("volcanic") || name.contains("ash")) {
          BrandViolet = Color(0xFF64748B) // Slate Gray
          BrandCyan = Color(0xFF71717A)   // Zinc
          BrandPink = Color(0xFF3B82F6)   // Blue
      } else if (name.contains("royal") || name.contains("silk")) {
          BrandViolet = Color(0xFF6D28D9) // Royal Violet
          BrandCyan = Color(0xFFDB2777)   // Deep Pink
          BrandPink = Color(0xFF2563EB)   // Royal Blue
      }
  } else if (bgType == "image") {
      val url = bgImageName.lowercase()
      if (url.contains("forest") || url.contains("wood") || url.contains("grass") || url.contains("leaves") || url.contains("olive") || url.contains("green")) {
          BrandViolet = Color(0xFF10B981) // Emerald Green
          BrandCyan = Color(0xFF14B8A6)   // Teal
          BrandPink = Color(0xFFF59E0B)   // Amber
      } else if (url.contains("sunset") || url.contains("desert") || url.contains("sand") || url.contains("orange") || url.contains("gold") || url.contains("rust")) {
          BrandViolet = Color(0xFFF97316) // Energetic Orange
          BrandCyan = Color(0xFFF59E0B)   // Vivid Amber
          BrandPink = Color(0xFFEF4444)   // Red
      } else if (url.contains("beach") || url.contains("ocean") || url.contains("sea") || url.contains("water") || url.contains("sky") || url.contains("blue")) {
          BrandViolet = Color(0xFF0EA5E9) // Sky Blue
          BrandCyan = Color(0xFF06B6D4)   // Electric Cyan
          BrandPink = Color(0xFF6366F1)   // Indigo
      } else if (url.contains("lavender") || url.contains("flower") || url.contains("purple") || url.contains("rose")) {
          BrandViolet = Color(0xFF8B5CF6) // Royal Lavender
          BrandCyan = Color(0xFFEC4899)   // Hot Pink
          BrandPink = Color(0xFF0EA5E9)   // Sky Blue
      } else {
          // Default beautiful dynamic teal adaptive color for custom images or other textures
          BrandViolet = Color(0xFF0EA5E9) // Sky Blue
          BrandCyan = Color(0xFF10B981)   // Emerald Green
          BrandPink = Color(0xFF6366F1)   // Indigo
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

  MaterialTheme(colorScheme = colorScheme, typography = createTypography(fontSize, fontStyle), content = content)
}
