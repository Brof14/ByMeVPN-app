package com.example.bymevpn.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import com.example.bymevpn.theme.AppColors
import com.example.bymevpn.theme.AppGradients

/**
 * Screen background featuring deep navy gradient with abstract ambient laser lines & glow circles.
 */
@Composable
fun GradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush = AppGradients.Background)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 5 subtle glow discs
            val shapeCount = 5
            for (i in 0 until shapeCount) {
                val centerX = w * (0.2f + (i * 0.15f))
                val centerY = h * (0.3f + (i * 0.1f))
                val radius = w * (0.1f + (i * 0.02f))
                val alpha = (0.20f - (i * 0.035f)).coerceAtLeast(0.04f)

                drawCircle(
                    color = AppColors.GlassLight.copy(alpha = alpha),
                    radius = radius,
                    center = Offset(centerX, centerY)
                )
            }

            // 3 diagonal laser glow lines
            for (i in 0 until 3) {
                val startX = w * (i * 0.3f)
                val startY = h * (0.1f + (i * 0.2f))
                val endX = w * (0.8f - (i * 0.2f))
                val endY = h * (0.9f - (i * 0.2f))
                val strokeWidth = 2f + (i * 2f)
                val lineAlpha = (0.12f - (i * 0.03f)).coerceAtLeast(0.03f)

                drawLine(
                    color = AppColors.CyanGlow.copy(alpha = lineAlpha),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
        }

        content()
    }
}
