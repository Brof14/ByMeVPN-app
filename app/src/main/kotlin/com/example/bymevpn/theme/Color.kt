package com.example.bymevpn.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object AppColors {
    // Logo-Driven Design Tokens: Deep Midnight Obsidian-Sapphire Canvas & Glass Surfaces
    val BgCanvas = Color(0xFF030A1C)
    val BgSurface = Color(0xFF0A1632)
    val BgSurfaceAccent = Color(0xFF0E1F45)
    val BgSurfaceAccentAlt = Color(0xFF0C1C3D)
    val BgSurfaceElevated = Color(0xFF11254E)
    val BgInput = Color(0xFF07122B)

    // Luminous Sapphire & Cyan-Mint Edges
    val BorderDefault = Color(0xFF1C3566)
    val BorderSubtle = Color(0xFF14284E)
    val BorderAccent = Color(0xFF275494)
    val BorderActiveGreen = Color(0xFF2EE9B8)
    val BorderInput = Color(0xFF224178)
    val BorderTelegram = Color(0xFF235C99)
    val BorderSecondaryButton = Color(0xFF2B4D82)

    // Official Logo Palette: Sapphire Blue (#0077FF) -> Neon Cyan (#00D8FF) -> Emerald-Mint (#42F59B)
    val BrandBlue = Color(0xFF0077FF)
    val ElectricBlue = Color(0xFF0077FF)
    val SkyBlue = Color(0xFF00D8FF)
    val BrandMint = Color(0xFF2EE9B8)
    val BrandMintAccent = Color(0xFF00D8FF)
    val BrandMintBright = Color(0xFF42F59B)
    val BrandMintDark = Color(0xFF031326)
    val BrandMintDarkText = Color(0xFF031326)

    // Text & Typography
    val TextPrimary = Color(0xFFF0F6FF)
    val TextSecondary = Color(0xFF9EB4D4)
    val TextMuted = Color(0xFF6D86AB)
    val TextKicker = Color(0xFF7B95BA)
    val TextSoft = Color(0xFFB3D5EE)
    val White = Color(0xFFFFFFFF)

    // Navigation & Toolbars
    val NavBackground = Color(0xFF061028)
    val NavBorder = Color(0xFF19315C)
    val NavItemActive = Color(0xFF2EE9B8)
    val NavItemInactive = Color(0xFF738BAE)
    val TopBarIconBg = Color(0xFF0B1938)
    val TopBarIconBorder = Color(0xFF1E3C70)
    val TopBarBackBg = Color(0xFF0B1938)
    val TopBarBackBorder = Color(0xFF1E3C70)

    // Toggles & Controls
    val ToggleTrackOff = Color(0xFF1A2B4C)
    val ToggleTrackOn = Color(0xFF2EE9B8)
    val ToggleThumbOff = Color(0xFF91A6C7)
    val ToggleThumbOn = Color(0xFF031326)

    // Badges & Status
    val BadgeGreenBg = Color(0xFF0C2E3F)
    val BadgeGreenText = Color(0xFF42F59B)
    val BadgeRedBg = Color(0xFF2E1722)
    val BadgeRedText = Color(0xFFFF8F96)

    // Secondary Buttons
    val SecondaryButtonBg = Color(0xFF0E2046)
    val SecondaryButtonText = Color(0xFFEAF4FF)

    // Telegram Brand
    val TelegramBlue = Color(0xFF29B6F6)
    val TelegramBg = Color(0xFF0D284D)

    // Status Colors
    val StatusActive = Color(0xFF42F59B)
    val StatusInactive = Color(0xFF6D86AB)
    val StatusError = Color(0xFFFF5A67)
    val StatusWarning = Color(0xFFFFB74D)

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
        0.0f to Color(0x360077FF),
        0.38f to Color(0x1600D8FF),
        0.72f to Color(0x00030A1C),
        1.0f to AppColors.BgCanvas
    )

    // Background vertical fallback
    val Background = Brush.verticalGradient(
        0.0f to Color(0xFF061330),
        0.32f to AppColors.BgCanvas,
        1.0f to Color(0xFF020714)
    )

    // Official Logo Signature Gradient: Sapphire Blue -> Neon Cyan -> Emerald-Mint
    val Shield = Brush.linearGradient(
        colors = listOf(
            Color(0xFF0077FF),
            Color(0xFF00D8FF),
            Color(0xFF42F59B)
        )
    )

    // Primary CTA Button Gradient: Neon Cyan -> Emerald-Mint for crisp dark-sapphire text contrast
    val PrimaryButton = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF00D8FF),
            Color(0xFF2EE9B8),
            Color(0xFF42F59B)
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
            Color(0xFF07122B)
        )
    )

    // Active Plan / Active Tunnel Card Gradient
    val ActivePlanCard = Brush.linearGradient(
        colors = listOf(
            Color(0xFF0C2A4D),
            Color(0xFF0A1D3D)
        )
    )

    // Power button active inner glow
    val PowerButtonActive = Brush.linearGradient(
        colors = listOf(
            Color(0xFF0A2E57),
            Color(0xFF07213E)
        )
    )

    // Power button inactive inner circle
    val PowerButtonInactive = Brush.linearGradient(
        colors = listOf(
            Color(0xFF0D1E42),
            Color(0xFF08142E)
        )
    )
}
