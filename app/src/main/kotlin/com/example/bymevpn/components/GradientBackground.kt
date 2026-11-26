package com.example.bymevpn.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * High-performance screen background with cached gradient layers.
 */
@Composable
fun GradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val bgBrush = remember {
        Brush.verticalGradient(
            0.0f to Color(0xFF040814),
            0.45f to Color(0xFF081224),
            1.0f to Color(0xFF040814)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush = bgBrush)
            .drawWithCache {
                val glowRadius = (size.width * 0.55f).coerceAtLeast(1f)
                val glowCenter = Offset(size.width / 2f, size.height * 0.16f)
                val radial = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to Color(0xFF0F2B5C).copy(alpha = 0.45f),
                        0.35f to Color(0xFF0A1C3E).copy(alpha = 0.28f),
                        0.70f to Color(0xFF061126).copy(alpha = 0.12f),
                        1.0f to Color.Transparent
                    ),
                    center = glowCenter,
                    radius = glowRadius
                )
                onDrawBehind {
                    drawCircle(
                        brush = radial,
                        radius = glowRadius,
                        center = glowCenter
                    )
                }
            }
    ) {
        content()
    }
}
