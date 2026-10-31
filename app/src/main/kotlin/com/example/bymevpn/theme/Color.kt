package com.example.bymevpn.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object AppColors {
    // V2 Design Tokens: Canvas & Surfaces
    val BgCanvas = Color(0xFF090F1D)
    val BgSurface = Color(0xFF121C2B)
    val BgSurfaceAccent = Color(0xFF172536)
    val BgSurfaceAccentAlt = Color(0xFF142638)
    val BgSurfaceElevated = Color(0xFF18283A)
    val BgInput = Color(0xFF111C2B)

    // Borders
    val BorderDefault = Color(0xFF263647)
    val BorderSubtle = Color(0xFF203246)
    val BorderAccent = Color(0xFF346071)
    val BorderActiveGreen = Color(0xFF69D3BC)
    val BorderInput = Color(0xFF2B4051)
    val BorderTelegram = Color(0xFF315B70)
    val BorderSecondaryButton = Color(0xFF526F80)

    // Primary CTA Mint & Brand Accents
    val BrandMint = Color(0xFF69D3BC)
    val BrandMintAccent = Color(0xFF66C7BE)
    val BrandMintBright = Color(0xFF79D9C4)
    val BrandMintDark = Color(0xFF0A1C29)
    val BrandMintDarkText = Color(0xFF0A2431)

    // Text & Typography
    val TextPrimary = Color(0xFFEEF4F4)
    val TextSecondary = Color(0xFF9CACBD)
    val TextMuted = Color(0xFF7D94A8)
    val TextKicker = Color(0xFF7D94A8)
    val TextSoft = Color(0xFFA7C4CC)
    val White = Color(0xFFFFFFFF)

    // Navigation & Toolbars
    val NavBackground = Color(0xFF101A29)
    val NavBorder = Color(0xFF243447)
    val NavItemActive = Color(0xFF71D2C3)
    val NavItemInactive = Color(0xFF8096A8)
    val TopBarIconBg = Color(0xFF152335)
    val TopBarIconBorder = Color(0xFF2B4053)
    val TopBarBackBg = Color(0xFF142235)
    val TopBarBackBorder = Color(0xFF2C4154)

    // Toggles & Controls
    val ToggleTrackOff = Color(0xFF303E40)
    val ToggleTrackOn = Color(0xFF71D0C4)
    val ToggleThumbOff = Color(0xFFA5B2B3)
    val ToggleThumbOn = Color(0xFF0A2230)

    // Badges & Status
    val BadgeGreenBg = Color(0xFF17373B)
    val BadgeGreenText = Color(0xFF8BDFCC)
    val BadgeRedBg = Color(0xFF2B2424)
    val BadgeRedText = Color(0xFFE3A29A)

    // Secondary Buttons
    val SecondaryButtonBg = Color(0xFFEAF8F8)
    val SecondaryButtonText = Color(0xFF0A2431)

    // Telegram Brand
    val TelegramBlue = Color(0xFF39BFFF)
    val TelegramBg = Color(0xFF16374C)

    // Status Colors
    val StatusActive = Color(0xFF69D3BC)
    val StatusInactive = Color(0xFF7D94A8)
    val StatusError = Color(0xFFFF5353)
    val StatusWarning = Color(0xFFFFB74D)

    // Backward compatibility aliases
    val BrandGreen = BrandMint
    val BrandGreenGlow = Color(0xFF82D8C6)
    val BrandGreenDark = BrandMintDark
    val ElectricBlue = Color(0xFF468FF0)
    val SkyBlue = Color(0xFF53BFCF)
    val BrandBlue = Color(0xFF468FF0)
    val Cyan = Color(0xFF79D9BC)
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
        0.0f to Color(0x27152E57),
        0.48f to Color(0x00152E57),
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
            Color(0xFF468FF0),
            Color(0xFF53BFCF),
            Color(0xFF79D9BC)
        )
    )

    // Primary CTA Button Gradient (#38EFB6 -> #48D8B7)
    val PrimaryButton = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF69D3BC),
            Color(0xFF64C0D4)
        )
    )

    // Accent Card Gradient (#132A26 -> #142321 -> #101E1D)
    val AccentCard = Brush.linearGradient(
        colors = listOf(
            Color(0xFF142638),
            Color(0xFF18283A),
            Color(0xFF101E1D)
        )
    )

    // Active Plan Card Gradient (#19312B -> #112221)
    val ActivePlanCard = Brush.linearGradient(
        colors = listOf(
            Color(0xFF1B3044),
            Color(0xFF122438)
        )
    )

    // Power button active inner circle
    val PowerButtonActive = Brush.linearGradient(
        colors = listOf(
            Color(0xFF203E57),
            Color(0xFF142C42)
        )
    )

    // Power button inactive inner circle
    val PowerButtonInactive = Brush.linearGradient(
        colors = listOf(
            Color(0xFF1B2A3F),
            Color(0xFF111D30)
        )
    )
}
