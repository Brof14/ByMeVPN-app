package com.example.bymevpn.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object AppTypography {
    val BrandWhite = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Black,
        fontSize = 42.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.6).sp,
        color = AppColors.White,
        shadow = Shadow(
            color = AppColors.CyanGlow.copy(alpha = 0.35f),
            offset = Offset.Zero,
            blurRadius = 24f
        )
    )

    val BrandGreen = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Black,
        fontSize = 42.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.6).sp,
        color = AppColors.BrandGreen,
        shadow = Shadow(
            color = AppColors.NeonGreen.copy(alpha = 0.45f),
            offset = Offset.Zero,
            blurRadius = 28f
        )
    )

    val Slogan = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.4.sp,
        color = AppColors.SloganGrey,
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.5f),
            offset = Offset(0f, 2f),
            blurRadius = 4f
        )
    )

    val BtnWhite = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 17.sp,
        letterSpacing = 0.3.sp,
        color = AppColors.White,
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.3f),
            offset = Offset(0f, 2f),
            blurRadius = 3f
        )
    )

    val BtnDark = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 17.sp,
        letterSpacing = 0.3.sp,
        color = AppColors.DarkText,
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.2f),
            offset = Offset(0f, 1f),
            blurRadius = 2f
        )
    )
}

val Typography = Typography(
    headlineLarge = AppTypography.BrandWhite,
    bodyMedium = AppTypography.Slogan,
    labelLarge = AppTypography.BtnWhite
)
