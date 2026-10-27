package com.example.bymevpn.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object AppColors {
    // V2 Design Tokens: Canvas & Surfaces
    val BgCanvas = Color(0xFF080E10)
    val BgSurface = Color(0xFF111A1D)
    val BgSurfaceAccent = Color(0xFF142A25)
    val BgSurfaceAccentAlt = Color(0xFF132A26)
    val BgSurfaceElevated = Color(0xFF142321)
    val BgInput = Color(0xFF111B1D)

    // Borders
    val BorderDefault = Color(0xFF203137)
    val BorderSubtle = Color(0xFF203033)
    val BorderAccent = Color(0xFF285A4C)
    val BorderActiveGreen = Color(0xFF38EFB6)
    val BorderInput = Color(0xFF2A4245)
    val BorderTelegram = Color(0xFF25525A)
    val BorderSecondaryButton = Color(0xFF587971)

    // Primary CTA Mint & Brand Accents
    val BrandMint = Color(0xFF38EFB6)
    val BrandMintAccent = Color(0xFF3CDEAC)
    val BrandMintBright = Color(0xFF43E0AC)
    val BrandMintDark = Color(0xFF062019)
    val BrandMintDarkText = Color(0xFF0E2D23)

    // Text & Typography
    val TextPrimary = Color(0xFFEEF4F4)
    val TextSecondary = Color(0xFF91A6A5)
    val TextMuted = Color(0xFF698589)
    val TextKicker = Color(0xFF698589)
    val TextSoft = Color(0xFFA0C2AE)
    val White = Color(0xFFFFFFFF)

    // Navigation & Toolbars
    val NavBackground = Color(0xFF0F1719)
    val NavBorder = Color(0xFF1D2C2E)
    val NavItemActive = Color(0xFF38E2B0)
    val NavItemInactive = Color(0xFF667E80)
    val TopBarIconBg = Color(0xFF121D20)
    val TopBarIconBorder = Color(0xFF26373A)
    val TopBarBackBg = Color(0xFF111D20)
    val TopBarBackBorder = Color(0xFF25383B)

    // Toggles & Controls
    val ToggleTrackOff = Color(0xFF303E40)
    val ToggleTrackOn = Color(0xFF33E1AD)
    val ToggleThumbOff = Color(0xFFA5B2B3)
    val ToggleThumbOn = Color(0xFF09231C)

    // Badges & Status
    val BadgeGreenBg = Color(0xFF1A3B2C)
    val BadgeGreenText = Color(0xFF65E3B1)
    val BadgeRedBg = Color(0xFF2B2424)
    val BadgeRedText = Color(0xFFE3A29A)

    // Secondary Buttons
    val SecondaryButtonBg = Color(0xFFEDF9F4)
    val SecondaryButtonText = Color(0xFF0E2D23)

    // Telegram Brand
    val TelegramBlue = Color(0xFF39BFFF)
    val TelegramBg = Color(0xFF123A4D)

    // Status Colors
    val StatusActive = Color(0xFF38EFB6)
    val StatusInactive = Color(0xFF698589)
    val StatusError = Color(0xFFFF5353)
    val StatusWarning = Color(0xFFFFB74D)

    // Backward compatibility aliases
    val BrandGreen = BrandMint
    val BrandGreenGlow = Color(0xFF6EFFA6)
    val BrandGreenDark = BrandMintDark
    val ElectricBlue = Color(0xFF1E92FF)
    val SkyBlue = Color(0xFF06C8BF)
    val BrandBlue = Color(0xFF1E92FF)
    val Cyan = Color(0xFF21E7A4)
    val CardBg = BgSurface
    val CardBgElevated = BgSurfaceAccent
    val CardBorder = BorderDefault
    val CardBorderActive = BorderActiveGreen
    val InputBg = BgInput
    val InputBorder = BorderInput
    val SloganGrey = TextSecondary
    val DarkText = BrandMintDark
    val BgTop = BgCanvas
    val BgMiddle = BgCanvas
    val BgBottom = BgCanvas
}

object AppGradients {
    // Canvas background with top radial glow
    val Canvas = Brush.radialGradient(
        0.0f to Color(0x57113939),
        0.48f to Color(0x00113939),
        1.0f to AppColors.BgCanvas
    )

    // Background vertical fallback
    val Background = Brush.verticalGradient(
        0.0f to AppColors.BgCanvas,
        0.5f to AppColors.BgCanvas,
        1.0f to AppColors.BgCanvas
    )

    // Official Shield Rim: Electric Blue -> Sky Blue -> Cyan -> Neon Green
    val Shield = Brush.linearGradient(
        colors = listOf(
            Color(0xFF1E92FF),
            Color(0xFF06C8BF),
            Color(0xFF21E7A4)
        )
    )

    // Primary CTA Button Gradient (#38EFB6 -> #48D8B7)
    val PrimaryButton = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF38EFB6),
            Color(0xFF48D8B7)
        )
    )

    // Accent Card Gradient (#132A26 -> #142321 -> #101E1D)
    val AccentCard = Brush.linearGradient(
        colors = listOf(
            Color(0xFF132A26),
            Color(0xFF142321),
            Color(0xFF101E1D)
        )
    )

    // Active Plan Card Gradient (#19312B -> #112221)
    val ActivePlanCard = Brush.linearGradient(
        colors = listOf(
            Color(0xFF19312B),
            Color(0xFF112221)
        )
    )

    // Power button active inner circle
    val PowerButtonActive = Brush.linearGradient(
        colors = listOf(
            Color(0xFF123C34),
            Color(0xFF102620)
        )
    )

    // Power button inactive inner circle
    val PowerButtonInactive = Brush.linearGradient(
        colors = listOf(
            Color(0xFF1A2B2C),
            Color(0xFF101B1E)
        )
    )
}
