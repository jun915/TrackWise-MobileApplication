package com.example.ui.theme

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.ui.TrackWiseViewModel

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.BoxScope

data class BgColorOption(
    val name: String,
    val lightColor: Color,
    val darkColor: Color
)

data class BgGradientOption(
    val name: String,
    val lightColors: List<Color>,
    val darkColors: List<Color>
)

object BackgroundPresets {
    val colors = listOf(
        BgColorOption("Lavender & Amethyst", Color(0xFFF5EEFF), Color(0xFF1E0E3B)),
        BgColorOption("Sky & Deep Ocean", Color(0xFFE0F2FE), Color(0xFF0C2D48)),
        BgColorOption("Mint & Deep Forest", Color(0xFFE8FDF2), Color(0xFF042E22)),
        BgColorOption("Rose & Dark Burgundy", Color(0xFFFFF1F2), Color(0xFF330511)),
        BgColorOption("Amber & Warm Rust", Color(0xFFFFFBEB), Color(0xFF2E1103)),
        BgColorOption("Teal & Slate Marine", Color(0xFFE0F7FA), Color(0xFF00332D)),
        BgColorOption("Peach & Terracotta", Color(0xFFFFF5EB), Color(0xFF2E150E)),
        BgColorOption("Plum & Royal Purple", Color(0xFFFDF2FA), Color(0xFF330235)),
        BgColorOption("Sage & Olive", Color(0xFFF4F7F3), Color(0xFF18220E)),
        BgColorOption("Periwinkle & Royal Blue", Color(0xFFEEF2FF), Color(0xFF13103D)),
        BgColorOption("Gold & Bronze", Color(0xFFFFFDF5), Color(0xFF221A04)),
        BgColorOption("Crimson & Night Blood", Color(0xFFFFECEE), Color(0xFF2D050B)),
        BgColorOption("Coral & Charcoal Orange", Color(0xFFFFF4F0), Color(0xFF240E06)),
        BgColorOption("Slate & Midnight Obsidian", Color(0xFFF8FAFC), Color(0xFF0A0F1D)),
        BgColorOption("Midnight Black", Color(0xFF08080C), Color(0xFF08080C)),
        BgColorOption("Deep Charcoal", Color(0xFF1A1A1E), Color(0xFF1A1A1E)),
        BgColorOption("Navy Abyss", Color(0xFF020C1B), Color(0xFF020C1B)),
        BgColorOption("Obsidian Void", Color(0xFF0A0518), Color(0xFF0A0518)),
        BgColorOption("Dark Emerald", Color(0xFF021510), Color(0xFF021510)),
        BgColorOption("Royal Velvet", Color(0xFF150424), Color(0xFF150424)),
        BgColorOption("Vampire Red", Color(0xFF1D0308), Color(0xFF1D0308))
    )

    val gradients = listOf(
        BgGradientOption("Sunset Glow", listOf(Color(0xFFFFEDD5), Color(0xFFFEE2E2)), listOf(Color(0xFF3B0011), Color(0xFF361000))),
        BgGradientOption("Ocean Breeze", listOf(Color(0xFFE0F2FE), Color(0xFFD0F5F7)), listOf(Color(0xFF011425), Color(0xFF00151E))),
        BgGradientOption("Emerald Forest", listOf(Color(0xFFD1FAE5), Color(0xFFE6FDF0)), listOf(Color(0xFF012217), Color(0xFF03140F))),
        BgGradientOption("Aurora Night", listOf(Color(0xFFE8E8FF), Color(0xFFFBE3F3)), listOf(Color(0xFF131136), Color(0xFF240431))),
        BgGradientOption("Cosmic Dust", listOf(Color(0xFFF3EFFF), Color(0xFFFFE0E3)), listOf(Color(0xFF110224), Color(0xFF2C020B))),
        BgGradientOption("Volcanic Ash", listOf(Color(0xFFF1F5F9), Color(0xFFE2E8F0)), listOf(Color(0xFF0B101E), Color(0xFF01040A))),
        BgGradientOption("Royal Silk", listOf(Color(0xFFFBE8FF), Color(0xFFF3EFFF)), listOf(Color(0xFF1E0744), Color(0xFF13103D))),
        BgGradientOption("Midnight Abyss", listOf(Color(0xFF0C0728), Color(0xFF03010C)), listOf(Color(0xFF0C0728), Color(0xFF03010C))),
        BgGradientOption("Dark Nebula", listOf(Color(0xFF1B003A), Color(0xFF070014)), listOf(Color(0xFF1B003A), Color(0xFF070014))),
        BgGradientOption("Forest Shadow", listOf(Color(0xFF021E12), Color(0xFF010805)), listOf(Color(0xFF021E12), Color(0xFF010805))),
        BgGradientOption("Charcoal Slate", listOf(Color(0xFF1C1E21), Color(0xFF0B0C0E)), listOf(Color(0xFF1C1E21), Color(0xFF0B0C0E))),
        BgGradientOption("Blood Rose", listOf(Color(0xFF330006), Color(0xFF0E0002)), listOf(Color(0xFF330006), Color(0xFF0E0002))),
        BgGradientOption("Abyssal Trench", listOf(Color(0xFF001524), Color(0xFF00050A)), listOf(Color(0xFF001524), Color(0xFF00050A)))
    )

    val textures = listOf(
        "https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=800", // Clock Wallpaper (Default)
        "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800", // Sand/Gold texture
        "https://images.unsplash.com/photo-1518531933037-91b2f5f229cc?w=800", // Silk/Satin
        "https://images.unsplash.com/photo-1533090161767-e6ffed986c88?w=800", // Marble
        "https://images.unsplash.com/photo-1517816743773-6e0fd518b4a6?w=800", // Crumpled Paper
        "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?w=800", // Holographic/Foil
        "https://images.unsplash.com/photo-1508962914676-134849a727f0?w=800", // Dark Carbon
        "https://images.unsplash.com/photo-1541123437800-1bb1317badc2?w=800", // Wood Grain
        "https://images.unsplash.com/photo-1583847268964-b28dc8f51f92?w=800", // Linen Fabric
        "https://images.unsplash.com/photo-1618220179428-22790b461013?w=800", // Terrazzo
        "https://images.unsplash.com/photo-1541701494587-cb58502866ab?w=800"  // Abstract Paint Texture
    )

    val abstractImages = listOf(
        "https://images.unsplash.com/photo-1557683316-973673baf926?w=800", // Pastel Gradients
        "https://images.unsplash.com/photo-1541701494587-cb58502866ab?w=800", // Fluid Art
        "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800", // Neon Wave
        "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?w=800", // Cyberpunk Abstract
        "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800", // Geometric Shapes
        "https://images.unsplash.com/photo-1579783902614-a3fb3927b6a5?w=800", // Organic Waves
        "https://images.unsplash.com/photo-1531315630201-bb15abeb1653?w=800", // Smoky Pastel
        "https://images.unsplash.com/photo-1504333631150-c82014e52ec7?w=800", // Dark Abstract Glass
        "https://images.unsplash.com/photo-1579783922614-a3fb3927b6a5?w=800", // Watercolor Splatter
        "https://images.unsplash.com/photo-1526047932273-341f2a7631f9?w=800"  // Flowing lines
    )

    val cityscapes = listOf(
        "https://images.unsplash.com/photo-1503899036084-c55cdd92da26?w=800", // Tokyo Night
        "https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?w=800", // NYC Skyline
        "https://images.unsplash.com/photo-1502602898657-3e91760cbb34?w=800", // Eiffel Tower
        "https://images.unsplash.com/photo-1515621061946-eff1c2a352bd?w=800", // Cyberpunk Alley
        "https://images.unsplash.com/photo-1513635269975-59663e0ca1ad?w=800", // London Bridge
        "https://images.unsplash.com/photo-1428908728789-d2de25dbd4e2?w=800", // Rainy City Street
        "https://images.unsplash.com/photo-1506012787146-f92b2d7d6d96?w=800", // Golden Gate Bridge
        "https://images.unsplash.com/photo-1512453979798-5ea266f8880c?w=800", // Dubai Skyscrapers
        "https://images.unsplash.com/photo-1527631746610-bca00a040d60?w=800", // Venice Canals
        "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=800"  // Minimalist Architecture
    )

    val landscapes = listOf(
        "https://images.unsplash.com/photo-1441974231531-c6227db76b6e?w=800", // Forest Path
        "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=800", // Misty Mountains
        "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800", // Ocean Sunset
        "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=800", // Autumn Leaves
        "https://images.unsplash.com/photo-1509316975850-ff9c5deb0cd9?w=800", // Desert Dunes
        "https://images.unsplash.com/photo-1483728642387-6c3bdd6c93e5?w=800", // Snowy Peaks
        "https://images.unsplash.com/photo-1500382017468-9049fed747ef?w=800", // Lavender Fields
        "https://images.unsplash.com/photo-1482862549707-f63cb32c5fd9?w=800", // Aurora Borealis
        "https://images.unsplash.com/photo-1511497584788-876760111969?w=800", // Deep Green Forest
        "https://images.unsplash.com/photo-1506929562872-bb421503ef21?w=800"  // Tropical Beach
    )
}

@Composable
fun AppBackground(
    viewModel: TrackWiseViewModel,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val bgType by viewModel.appBgType.collectAsState()
    val bgColorName by viewModel.appBgColor.collectAsState()
    val bgGradientName by viewModel.appBgGradient.collectAsState()
    val bgImageUrl by viewModel.appBgImage.collectAsState()
    val bgCustomUri by viewModel.appBgCustomUri.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        when (bgType) {
            "color" -> {
                val colorOpt = BackgroundPresets.colors.find { it.name == bgColorName } ?: BackgroundPresets.colors.first()
                val activeColor = if (isDark) colorOpt.darkColor else colorOpt.lightColor
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(activeColor)
                )
                val overlayColor = if (isDark) Color.Black.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.25f)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(overlayColor)
                )
            }
            "gradient" -> {
                val gradOpt = BackgroundPresets.gradients.find { it.name == bgGradientName } ?: BackgroundPresets.gradients.first()
                val activeColors = if (isDark) gradOpt.darkColors else gradOpt.lightColors
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(activeColors))
                )
                val overlayColor = if (isDark) Color.Black.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.25f)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(overlayColor)
                )
            }
            "image" -> {
                // If custom URI is selected, use it. Otherwise use preset URL
                val imageSource = if (bgImageUrl == "custom" && bgCustomUri.isNotEmpty()) {
                    bgCustomUri
                } else {
                    bgImageUrl
                }

                if (imageSource.isNotEmpty()) {
                    AsyncImage(
                        model = imageSource,
                        contentDescription = "App Background Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Transparency / Scrim Overlay to ensure maximum contrast and legibility for all screens
                val overlayColor = if (isDark) Color.Black.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.75f)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(overlayColor)
                )
            }
            else -> { // "none"
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                )
            }
        }

        // Render contents over background
        content()

        // Top status bar subtle gradient scrim to ensure mobile time, battery %, and network signal are always clearly visible
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.45f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}
