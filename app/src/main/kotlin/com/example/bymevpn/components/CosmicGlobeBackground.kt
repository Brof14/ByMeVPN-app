package com.example.bymevpn.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.bymevpn.theme.AppColors

/**
 * Lightweight obsidian-sapphire backdrop cached per size so auth scrolling and typing stay at full frame rate.
 */
@Composable
fun CosmicGlobeBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.BgCanvas)
            .drawWithCache {
                val glowRadius = size.width * 0.72f
                val glowCenter = Offset(size.width * 0.5f, size.height * 0.08f)
                val radial = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to Color(0x280077FF),
                        0.45f to Color(0x1000D8FF),
                        1.0f to Color.Transparent
                    ),
                    center = glowCenter,
                    radius = glowRadius.coerceAtLeast(1f)
                )
                onDrawBehind {
                    drawRect(brush = radial)
                }
            }
    ) {
        content()
    }
}
