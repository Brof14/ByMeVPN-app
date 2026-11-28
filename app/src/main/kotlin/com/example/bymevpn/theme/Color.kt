package com.example.bymevpn.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object AppColors {
    // Logo-Driven Design Tokens: Deep Midnight Obsidian-Sapphire Canvas & Glass Surfaces
    val BgCanvas = Color(0xFF0A0F16)
    val BgSurface = Color(0xFF121C26)
    val BgSurfaceAccent = Color(0xFF17242F)
    val BgSurfaceAccentAlt = Color(0xFF15222D)
    val BgSurfaceElevated = Color(0xFF1D2A35)
    val BgInput = Color(0xFF1A2631)

    // Luminous Sapphire & Cyan-Mint Edges
    val BorderDefault = Color(0xFF263842)
    val BorderSubtle = Color(0xFF202D37)
    val BorderAccent = Color(0xFF315565)
    val BorderActiveGreen = Color(0xFF3AC7A8)
    val BorderInput = Color(0xFF293E4A)
    val BorderTelegram = Color(0xFF275978)
    val BorderSecondaryButton = Color(0xFF314A57)

    // Official Logo Palette: Sapphire Blue (#0077FF) -> Neon Cyan (#00D8FF) -> Emerald-Mint (#42F59B)
    val BrandBlue = Color(0xFF1989C5)
    val ElectricBlue = Color(0xFF1989C5)
    val SkyBlue = Color(0xFF49BFCC)
    val BrandMint = Color(0xFF3AC7A8)
    val BrandMintAccent = Color(0xFF49BFCC)
    val BrandMintBright = Color(0xFF65D7AE)
    val BrandMintDark = Color(0xFF0B1B1B)
    val BrandMintDarkText = Color(0xFF0B1B1B)

    // Text & Typography
    val TextPrimary = Color(0xFFF3F5F7)
    val TextSecondary = Color(0xFFA8B2BC)
    val TextMuted = Color(0xFF7E8994)
    val TextKicker = Color(0xFF91A0AA)
    val TextSoft = Color(0xFFB6D4D8)
    val White = Color(0xFFFFFFFF)

    // Navigation & Toolbars
    val NavBackground = Color(0xFF0D151D)
    val NavBorder = Color(0xFF202F39)
    val NavItemActive = Color(0xFF3AC7A8)
    val NavItemInactive = Color(0xFF7E8994)
    val TopBarIconBg = Color(0xFF17232D)
    val TopBarIconBorder = Color(0xFF2A3D49)
    val TopBarBackBg = Color(0xFF17232D)
    val TopBarBackBorder = Color(0xFF2A3D49)

    // Toggles & Controls
    val ToggleTrackOff = Color(0xFF263640)
    val ToggleTrackOn = Color(0xFF3AC7A8)
    val ToggleThumbOff = Color(0xFFA4B1BB)
    val ToggleThumbOn = Color(0xFF0B1B1B)

    // Badges & Status
    val BadgeGreenBg = Color(0xFF15352F)
    val BadgeGreenText = Color(0xFF65D7AE)
    val BadgeRedBg = Color(0xFF321F25)
    val BadgeRedText = Color(0xFFF4A1A5)

    // Secondary Buttons
    val SecondaryButtonBg = Color(0xFF1A2933)
    val SecondaryButtonText = Color(0xFFE9EEF1)

    // Telegram Brand
    val TelegramBlue = Color(0xFF35A9DD)
    val TelegramBg = Color(0xFF182B37)

    // Status Colors
    val StatusActive = Color(0xFF65D7AE)
    val StatusInactive = Color(0xFF7E8994)
    val StatusError = Color(0xFFF16F78)
    val StatusWarning = Color(0xFFE9AF68)

    // Backward compatibility aliases
    val BrandGreen = BrandMint
    val BrandGreenGlow = BrandMintBright
    val BrandGreenDark = BrandMintDark
    val Teal = BrandMint
    val NeonTeal = BrandMintBright
    val Cyan = SkyBlue
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
    // Deep Midnight Obsidian-Sapphire backdrop with top Cyan-Sapphire radial glow
    val Canvas = Brush.radialGradient(
        0.0f to Color(0x263BA9C2),
        0.38f to Color(0x1249BFCC),
        0.72f to Color(0x000A0F16),
        1.0f to AppColors.BgCanvas
    )

    // Background vertical fallback
    val Background = Brush.verticalGradient(
        0.0f to Color(0xFF121C26),
        0.32f to AppColors.BgCanvas,
        1.0f to Color(0xFF080D12)
    )

    // Official Logo Signature Gradient: Sapphire Blue -> Neon Cyan -> Emerald-Mint
    val Shield = Brush.linearGradient(
        colors = listOf(
            Color(0xFF1989C5),
            Color(0xFF49BFCC),
            Color(0xFF65D7AE)
        )
    )

    // Primary CTA Button Gradient: Neon Cyan -> Emerald-Mint for crisp dark-sapphire text contrast
    val PrimaryButton = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF49BFCC),
            Color(0xFF3AC7A8),
            Color(0xFF65D7AE)
        )
    )

    // Disabled CTA Gradient
    val DisabledButton = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF142342),
            Color(0xFF172B4F)
        )
    )

    // Glass-beveled Sapphire Accent Card
    val AccentCard = Brush.linearGradient(
        colors = listOf(
            Color(0xFF0E214A),
            Color(0xFF091838),
            Color(0xFF1A2631)
        )
    )

    // Active Plan / Active Tunnel Card Gradient
    val ActivePlanCard = Brush.linearGradient(
        colors = listOf(
            Color(0xFF193343),
            Color(0xFF142630)
        )
    )

    // Power button active inner glow
    val PowerButtonActive = Brush.linearGradient(
        colors = listOf(
            Color(0xFF183040),
            Color(0xFF142530)
        )
    )

    // Power button inactive inner circle
    val PowerButtonInactive = Brush.linearGradient(
        colors = listOf(
            Color(0xFF1A2A35),
            Color(0xFF121F29)
        )
    )
}
