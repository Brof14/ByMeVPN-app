package com.example.bymevpn.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = AppColors.BrandMint,
    secondary = AppColors.SkyBlue,
    tertiary = AppColors.BrandBlue,
    background = AppColors.BgCanvas,
    surface = AppColors.BgSurface,
    surfaceVariant = AppColors.BgSurfaceElevated,
    surfaceContainerHigh = AppColors.BgSurfaceElevated,
    outline = AppColors.BorderDefault,
    onPrimary = AppColors.BrandMintDark,
    onSecondary = AppColors.BrandMintDark,
    onBackground = AppColors.TextPrimary,
    onSurface = AppColors.TextPrimary,
    onSurfaceVariant = AppColors.TextSecondary
)

@Composable
fun ByMeVPNTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
