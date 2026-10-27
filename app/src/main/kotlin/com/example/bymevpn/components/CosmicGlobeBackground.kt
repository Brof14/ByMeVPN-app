package com.example.bymevpn.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Authentic deep space + curved Earth network background matching ByMeVPN design:
 * - Deep dark space backdrop
 * - Curved Earth horizon with glowing turquoise/cyan atmosphere
 * - Constellation network arcs connecting secure VPN nodes across the globe
 * - Soft radial cyan/green bloom behind the shield logo
 */
@Composable
fun CosmicGlobeBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF060A0E))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Vertical space gradient
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF071018),
                        Color(0xFF050B10),
                        Color(0xFF04080D),
                        Color(0xFF030508)
                    ),
                    startY = 0f,
                    endY = h
                )
            )

            // 2. Distant stars in upper atmosphere
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
                drawCircle(color = Color(0x99B8E2FF), radius = r, center = pos)
            }

            // 3. Globe body (large circle arching from top)
            val globeCx = w * 0.5f
            val globeCy = h * 0.44f
            val globeR = w * 0.82f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF0F202B),
                        Color(0xFF0A161E),
                        Color(0xFF070F15),
                        Color(0xFF04080D)
                    ),
                    center = Offset(globeCx, globeCy - globeR * 0.25f),
                    radius = globeR
                ),
                radius = globeR,
                center = Offset(globeCx, globeCy)
            )

            // Faint continents silhouettes
            val contLeft = Path().apply {
                moveTo(w * 0.08f, h * 0.11f)
                cubicTo(w * 0.22f, h * 0.14f, w * 0.34f, h * 0.21f, w * 0.30f, h * 0.29f)
                cubicTo(w * 0.24f, h * 0.33f, w * 0.14f, h * 0.30f, w * 0.06f, h * 0.22f)
                close()
            }
            drawPath(contLeft, color = Color(0x1F204055), style = Fill)

            val contRight = Path().apply {
                moveTo(w * 0.66f, h * 0.13f)
                cubicTo(w * 0.80f, h * 0.15f, w * 0.94f, h * 0.21f, w * 0.90f, h * 0.31f)
                cubicTo(w * 0.82f, h * 0.34f, w * 0.70f, h * 0.28f, w * 0.62f, h * 0.21f)
                close()
            }
            drawPath(contRight, color = Color(0x1F204055), style = Fill)

            // 4. Glowing Atmosphere Rim along the curvature
            val atmosphereBrush = Brush.linearGradient(
                colors = listOf(
                    Color(0x0000B4D8),
                    Color(0x6000D9F5),
                    Color(0xE000FFA3),
                    Color(0x8000E676),
                    Color(0x0000E676)
                ),
                start = Offset(w * 0.06f, h * 0.09f),
                end = Offset(w * 0.94f, h * 0.09f)
            )

            // Outer soft atmospheric glow
            drawArc(
                brush = atmosphereBrush,
                startAngle = 195f,
                sweepAngle = 150f,
                useCenter = false,
                topLeft = Offset(globeCx - globeR, globeCy - globeR),
                size = Size(globeR * 2f, globeR * 2f),
                style = Stroke(width = 14f),
                alpha = 0.40f
            )

            // Sharp horizon line
            drawArc(
                brush = atmosphereBrush,
                startAngle = 195f,
                sweepAngle = 150f,
                useCenter = false,
                topLeft = Offset(globeCx - globeR, globeCy - globeR),
                size = Size(globeR * 2f, globeR * 2f),
                style = Stroke(width = 2.4f),
                alpha = 0.90f
            )

            // 5. Global Network Nodes and Arcs
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
                val arc = Path().apply {
                    moveTo(p1.x, p1.y)
                    quadraticBezierTo(midX, midY, p2.x, p2.y)
                }
                drawPath(arc, color = Color(0x3500FFA3), style = Stroke(width = 3.5f, cap = StrokeCap.Round))
                drawPath(arc, color = Color(0xB500FFA3), style = Stroke(width = 1.3f, cap = StrokeCap.Round))
            }

            drawNetworkArc(nodes[0], nodes[1], -18f)
            drawNetworkArc(nodes[1], nodes[2], -14f)
            drawNetworkArc(nodes[2], nodes[3], -10f)
            drawNetworkArc(nodes[3], nodes[4], -14f)
            drawNetworkArc(nodes[4], nodes[5], -18f)
            drawNetworkArc(nodes[1], nodes[6], 14f)
            drawNetworkArc(nodes[4], nodes[7], 14f)

            for (pt in nodes) {
                drawCircle(color = Color(0x5500FFA3), radius = 6f, center = pt)
                drawCircle(color = Color(0xFFD4FFF3), radius = 2.5f, center = pt)
            }

            // 6. Radiant Cyan-Emerald Bloom centered behind the Shield Logo
            val bloomCenter = Offset(w * 0.5f, h * 0.165f)
            val bloomRadius = w * 0.42f
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to Color(0x3500FFA3),
                        0.35f to Color(0x2000B4D8),
                        0.70f to Color(0x0A004B80),
                        1.0f to Color.Transparent
                    ),
                    center = bloomCenter,
                    radius = bloomRadius
                ),
                radius = bloomRadius,
                center = bloomCenter
            )

            // Smooth fade to solid bottom background
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to Color.Transparent,
                        0.40f to Color(0xCC060A0E),
                        0.60f to Color(0xFF060A0E),
                        1.0f to Color(0xFF060A0E)
                    ),
                    startY = h * 0.26f,
                    endY = h * 0.52f
                )
            )
        }

        content()
    }
}
