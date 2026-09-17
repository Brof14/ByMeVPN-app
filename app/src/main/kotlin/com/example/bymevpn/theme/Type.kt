package com.example.bymevpn.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object AppTypography {
    // "ByMe" in clean pure white, crisp bold modern geometric sans-serif
    val BrandWhite = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 38.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp,
        color = AppColors.White
    )

    // "VPN" in bright neon lime/emerald green
    val BrandGreen = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 38.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp,
        color = AppColors.BrandGreen
    )

    // "Speed. Anonymity. Honesty." in high-contrast legible slate grey
    val Slogan = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 15.5.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.3.sp,
        color = AppColors.SloganGrey
    )

    // Button label: pure white, clean bold text
    val BtnWhite = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 16.5.sp,
        letterSpacing = 0.2.sp,
        color = AppColors.White
    )
}

val Typography = Typography(
    headlineLarge = AppTypography.BrandWhite,
    bodyMedium = AppTypography.Slogan,
    labelLarge = AppTypography.BtnWhite
)
