package com.example.bymevpn.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object AppColors {
    // Background — deep midnight navy blue gradient
    val BgTop = Color(0xFF0A0E1F)
    val BgMiddle = Color(0xFF0F1529)
    val BgBottom = Color(0xFF141B3F)

    // Brand green — bright emerald green
    val BrandGreen = Color(0xFF4BF091)
    val NeonGreen = Color(0xFF00FF88)

    // Brand blue — electric & deep blue
    val BrandBlue = Color(0xFF1A7FFF)
    val DeepBlue = Color(0xFF1155CC)
    val ElectricBlue = Color(0xFF007BFF)

    // Metallic and glass effects
    val White = Color(0xFFFFFFFF)
    val GlassLight = Color(0x1AFFFFFF)
    val GlassDark = Color(0x0DFFFFFF)
    val MetallicSilver = Color(0xFFC0C0C0)
    val CyanGlow = Color(0xFF00FFFF)

    // Text colors
    val SloganGrey = Color(0xFFA8B4CC)
    val SubtleGrey = Color(0xFF6B7280)
    val DarkText = Color(0xFF082010)
    val SnackBarBg = Color(0xFF1C2750)
}

object AppGradients {
    // Screen background gradient
    val Background = Brush.verticalGradient(
        0.0f to AppColors.BgTop,
        0.45f to AppColors.BgMiddle,
        1.0f to AppColors.BgBottom
    )

    // Shield rim gradient
    val ShieldRim = Brush.linearGradient(
        colors = listOf(
            Color(0xFF1A7FFF), // Electric blue
            Color(0xFF007BFF), // Premium blue
            Color(0xFF00FFFF), // Cyan glow
            Color(0xFF00FF88)  // Emerald green
        )
    )

    // Shield inner metallic gradient for 3D depth
    val ShieldMetallic = Brush.linearGradient(
        colors = listOf(
            Color(0xFF2A4FFF), // Light metallic blue
            Color(0xFF1A7FFF), // Main metallic
            Color(0xFF0D50CC)  // Dark metallic
        )
    )

    // Shield glassmorphism overlay
    val ShieldGlass = Brush.linearGradient(
        colors = listOf(
            AppColors.GlassLight,
            AppColors.GlassDark,
            AppColors.GlassLight
        )
    )

    // Sign Up button gradient
    val SignUp = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF1360E0), // Deep blue
            Color(0xFF1A7FFF), // Brand blue
            Color(0xFF2A90FF)  // Light blue
        )
    )

    // Log In button gradient
    val LogIn = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF00CC66), // Deep emerald
            Color(0xFF2EDE82), // Main green
            Color(0xFF4BF091)  // Bright green
        )
    )
}
