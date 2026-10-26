package com.example.bymevpn.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object AppColors {
    // Deep carbon emerald dark background matching Figma
    val BgCanvas = Color(0xFF080C0E)
    val BgTop = Color(0xFF0A0F13)
    val BgMiddle = Color(0xFF080C0E)
    val BgBottom = Color(0xFF06090B)

    // Containers & Cards
    val CardBg = Color(0xFF101A1F)
    val CardBgElevated = Color(0xFF142228)
    val CardBorder = Color(0xFF16282E)
    val CardBorderActive = Color(0xFF26E875)

    // Inputs
    val InputBg = Color(0xFF0C1418)
    val InputBorder = Color(0xFF182A31)

    // Brand Neon Accents (Figma & Logo)
    val BrandGreen = Color(0xFF26E875)
    val BrandGreenGlow = Color(0xFF6EFFA6)
    val BrandGreenDark = Color(0xFF158241)
    val ElectricBlue = Color(0xFF0062FF)
    val SkyBlue = Color(0xFF00ACFF)
    val BrandBlue = Color(0xFF0062FF)
    val Cyan = Color(0xFF00FFA2)

    // Neutrals & Typography
    val White = Color(0xFFFFFFFF)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFF8B9FA8)
    val SloganGrey = Color(0xFF8B9FA8)
    val TextMuted = Color(0xFF566B74)
    val DarkText = Color(0xFF04150A)

    // Telegram Brand
    val TelegramBlue = Color(0xFF2AABEE)

    // Status Colors
    val StatusActive = Color(0xFF26E875)
    val StatusInactive = Color(0xFF566B74)
    val StatusError = Color(0xFFFF5252)
    val StatusWarning = Color(0xFFFFB74D)
}

object AppGradients {
    val Background = Brush.verticalGradient(
        0.0f to AppColors.BgTop,
        0.5f to AppColors.BgMiddle,
        1.0f to AppColors.BgBottom
    )

    // Official Shield Rim: Electric Blue -> Sky Blue -> Cyan -> Neon Green
    val Shield = Brush.linearGradient(
        colors = listOf(
            Color(0xFF0062FF),
            Color(0xFF00ACFF),
            Color(0xFF00FFA2),
            Color(0xFF26E875)
        )
    )

    // Brand Button Gradient (Figma Primary Button)
    val PrimaryButton = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF26E875),
            Color(0xFF6EFFA6)
        )
    )

    val BlueButton = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF0058F6),
            Color(0xFF008BFF)
        )
    )
}
