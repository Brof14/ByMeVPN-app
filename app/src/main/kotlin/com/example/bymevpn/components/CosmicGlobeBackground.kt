package com.example.bymevpn.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.bymevpn.theme.AppColors

/**
 * Deep midnight obsidian-sapphire cosmic backdrop aligned with the ByMeVPN logo palette.
 */
@Composable
fun CosmicGlobeBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val glowStroke = remember { Stroke(width = 14f) }
    val horizonStroke = remember { Stroke(width = 2.4f) }
    val arcGlowStroke = remember { Stroke(width = 3.5f, cap = StrokeCap.Round) }
    val arcLineStroke = remember { Stroke(width = 1.3f, cap = StrokeCap.Round) }
    val contLeft = remember { Path() }
    val contRight = remember { Path() }
    val arcPath = remember { Path() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.BgCanvas)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF061330),
                        Color(0xFF040D24),
                        AppColors.BgCanvas,
                        Color(0xFF020714)
                    ),
                    startY = 0f,
                    endY = h
                )
            )

            val starField = listOf(
                Offset(w * 0.07f, h * 0.04f) to 1.3f,
                Offset(w * 0.20f, h * 0.025f) to 1.8f,
                Offset(w * 0.32f, h * 0.055f) to 1.1f,
                Offset(w * 0.76f, h * 0.035f) to 1.6f,
                Offset(w * 0.89f, h * 0.065f) to 1.4f,
                Offset(w * 0.14f, h * 0.11f) to 1.0f,
                Offset(w * 0.85f, h * 0.12f) to 1.2f,
                Offset(w * 0.95f, h * 0.16f) to 1.7f,
                Offset(w * 0.04f, h * 0.17f) to 1.4f,
                Offset(w * 0.60f, h * 0.02f) to 1.0f
            )
            for ((pos, r) in starField) {
                drawCircle(color = Color(0x99B5E8FF), radius = r, center = pos)
            }

            val globeCx = w * 0.5f
            val globeCy = h * 0.44f
            val globeR = w * 0.82f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF0C2252),
                        Color(0xFF08173A),
                        Color(0xFF050E26),
                        AppColors.BgCanvas
                    ),
                    center = Offset(globeCx, globeCy - globeR * 0.25f),
                    radius = globeR
                ),
                radius = globeR,
                center = Offset(globeCx, globeCy)
            )

            contLeft.rewind()
            contLeft.moveTo(w * 0.08f, h * 0.11f)
            contLeft.cubicTo(w * 0.22f, h * 0.14f, w * 0.34f, h * 0.21f, w * 0.30f, h * 0.29f)
            contLeft.cubicTo(w * 0.24f, h * 0.33f, w * 0.14f, h * 0.30f, w * 0.06f, h * 0.22f)
            contLeft.close()
            drawPath(contLeft, color = Color(0x240077FF), style = Fill)

            contRight.rewind()
            contRight.moveTo(w * 0.66f, h * 0.13f)
            contRight.cubicTo(w * 0.80f, h * 0.15f, w * 0.94f, h * 0.21f, w * 0.90f, h * 0.31f)
            contRight.cubicTo(w * 0.82f, h * 0.34f, w * 0.70f, h * 0.28f, w * 0.62f, h * 0.21f)
            contRight.close()
            drawPath(contRight, color = Color(0x240077FF), style = Fill)

            val atmosphereBrush = Brush.linearGradient(
                colors = listOf(
                    Color(0x000077FF),
                    Color(0x700077FF),
                    Color(0xD800D8FF),
                    Color(0xC042F59B),
                    Color(0x0042F59B)
                ),
                start = Offset(w * 0.06f, h * 0.09f),
                end = Offset(w * 0.94f, h * 0.09f)
            )

            drawArc(
                brush = atmosphereBrush,
                startAngle = 195f,
                sweepAngle = 150f,
                useCenter = false,
                topLeft = Offset(globeCx - globeR, globeCy - globeR),
                size = Size(globeR * 2f, globeR * 2f),
                style = glowStroke,
                alpha = 0.42f
            )

            drawArc(
                brush = atmosphereBrush,
                startAngle = 195f,
                sweepAngle = 150f,
                useCenter = false,
                topLeft = Offset(globeCx - globeR, globeCy - globeR),
                size = Size(globeR * 2f, globeR * 2f),
                style = horizonStroke,
                alpha = 0.92f
            )

            val nodes = listOf(
                Offset(w * 0.06f, h * 0.135f),
                Offset(w * 0.23f, h * 0.095f),
                Offset(w * 0.40f, h * 0.075f),
                Offset(w * 0.60f, h * 0.075f),
                Offset(w * 0.77f, h * 0.095f),
                Offset(w * 0.94f, h * 0.135f),
                Offset(w * 0.18f, h * 0.18f),
                Offset(w * 0.82f, h * 0.18f)
            )

            fun drawNetworkArc(p1: Offset, p2: Offset, controlYOffset: Float) {
                val midX = (p1.x + p2.x) / 2f
                val midY = (p1.y + p2.y) / 2f + controlYOffset
                arcPath.rewind()
                arcPath.moveTo(p1.x, p1.y)
                arcPath.quadraticBezierTo(midX, midY, p2.x, p2.y)
                drawPath(arcPath, color = Color(0x3800D8FF), style = arcGlowStroke)
                drawPath(arcPath, color = Color(0xB842F59B), style = arcLineStroke)
            }

            drawNetworkArc(nodes[0], nodes[1], -18f)
            drawNetworkArc(nodes[1], nodes[2], -14f)
            drawNetworkArc(nodes[2], nodes[3], -10f)
            drawNetworkArc(nodes[3], nodes[4], -14f)
            drawNetworkArc(nodes[4], nodes[5], -18f)
            drawNetworkArc(nodes[1], nodes[6], 14f)
            drawNetworkArc(nodes[4], nodes[7], 14f)

            for (pt in nodes) {
                drawCircle(color = Color(0x5500D8FF), radius = 6f, center = pt)
                drawCircle(color = Color(0xFFD9FFF5), radius = 2.5f, center = pt)
            }

            val bloomCenter = Offset(w * 0.5f, h * 0.165f)
            val bloomRadius = w * 0.42f
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to Color(0x3800D8FF),
                        0.36f to Color(0x260077FF),
                        0.72f to Color(0x1042F59B),
                        1.0f to Color.Transparent
                    ),
                    center = bloomCenter,
                    radius = bloomRadius
                ),
                radius = bloomRadius,
                center = bloomCenter
            )

            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to Color.Transparent,
                        0.40f to AppColors.BgCanvas.copy(alpha = 0.80f),
                        0.60f to AppColors.BgCanvas,
                        1.0f to AppColors.BgCanvas
                    ),
                    startY = h * 0.26f,
                    endY = h * 0.52f
                )
            )
        }

        content()
    }
}
