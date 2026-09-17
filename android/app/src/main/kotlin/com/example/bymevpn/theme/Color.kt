package com.example.bymevpn.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object AppColors {
    // Deep midnight navy background
    val BgTop = Color(0xFF060A17)
    val BgMiddle = Color(0xFF0A1326)
    val BgBottom = Color(0xFF060B18)
    val BgGlow = Color(0xFF0D2248)

    // Brand colors
    val BrandGreen = Color(0xFF26E875)
    val NeonGreen = Color(0xFF00FF85)
    val ElectricBlue = Color(0xFF0062FF)
    val BrandBlue = ElectricBlue
    val SkyBlue = Color(0xFF00ACFF)
    val Cyan = Color(0xFF00C8FF)

    // Neutral / text colors
    val White = Color(0xFFFFFFFF)
    val DarkText = Color(0xFF082010)
    val SloganGrey = Color(0xFF8E9EB8)
    val DotInactive = Color(0xFF19263E)

    // Button gradients
    val BlueBtnStart = Color(0xFF0058F6)
    val BlueBtnEnd = Color(0xFF008BFF)
    val GreenBtnStart = Color(0xFF14DE6C)
    val GreenBtnEnd = Color(0xFF43F38B)
}

object AppGradients {
    // Screen background gradient
    val Background = Brush.verticalGradient(
        0.0f to AppColors.BgTop,
        0.45f to AppColors.BgMiddle,
        1.0f to AppColors.BgBottom
    )

    // Shield rim gradient: Electric Blue -> Cyan -> Neon Green -> Emerald
    val Shield = Brush.linearGradient(
        colors = listOf(
            Color(0xFF0062FF),
            Color(0xFF00ACFF),
            Color(0xFF00FFA2),
            Color(0xFF26E875)
        )
    )

    // Ribbon gradient: Cyan/Blue -> Bright Cyan -> Neon Mint -> Emerald
    val Ribbon = Brush.linearGradient(
        colors = listOf(
            Color(0xFF0099FF),
            Color(0xFF00D4FF),
            Color(0xFF00FF88),
            Color(0xFF2AE678)
        )
    )

    // Sign Up button gradient
    val SignUp = Brush.horizontalGradient(
        colors = listOf(
            AppColors.BlueBtnStart,
            AppColors.BlueBtnEnd
        )
    )

    // Log In button gradient
    val LogIn = Brush.horizontalGradient(
        colors = listOf(
            AppColors.GreenBtnStart,
            AppColors.GreenBtnEnd
        )
    )
}
