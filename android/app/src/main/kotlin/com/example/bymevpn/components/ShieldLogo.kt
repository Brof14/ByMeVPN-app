package com.example.bymevpn.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * ByMeVPN Shield logo matching the reference design 1:1.
 * Optimized with drawWithCache to eliminate GC allocations and run at steady 60-120 FPS.
 */
@Composable
fun ShieldLogo(
    modifier: Modifier = Modifier,
    size: Dp = 212.dp
) {
    Spacer(
        modifier = modifier
            .size(width = size, height = size * 1.18f)
            .testTag("shield_logo")
            .semantics { contentDescription = "ByMeVPN Shield Logo with Crossover Ribbon" }
            .drawWithCache {
                val w = this.size.width
                val h = this.size.height

                val shieldStrokeW = w * 0.118f
                val halfStroke = shieldStrokeW / 2.0f

                val shieldPath = createShieldPath(w, h, halfStroke)
                val innerFillPath = createShieldPath(w, h, halfStroke + 2f)

                val innerFillBrush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0C1B38),
                        Color(0xFF040814)
                    ),
                    startY = 0f,
                    endY = h
                )

                val shieldBrush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0062FF), // Royal electric blue
                        Color(0xFF00A6FF), // Bright sky blue / cyan
                        Color(0xFF00FFA2), // Bright neon mint
                        Color(0xFF24E872)  // Neon lime green
                    ),
                    start = Offset(0f, h),
                    end = Offset(w, 0f)
                )

                val glowStroke = Stroke(
                    width = shieldStrokeW * 1.45f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )

                val mainStroke = Stroke(
                    width = shieldStrokeW,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )

                // Crossover Ribbon calculations
                val cx = w * 0.50f
                val cy = h * 0.44f
                val x0 = w * 0.215f
                val y0 = h * 0.122f
                val k = 0.65f
                val ribbonStrokeW = w * 0.115f

                val ribbonBrush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0099FF),
                        Color(0xFF00D4FF),
                        Color(0xFF00FF88),
                        Color(0xFF2AE678)
                    ),
                    start = Offset(cx - x0, cy),
                    end = Offset(cx + x0, cy)
                )

                val strand1 = Path().apply {
                    moveTo(cx - x0, cy - y0)
                    cubicTo(
                        cx - x0 + k * x0, cy - y0,
                        cx + x0 - k * x0, cy + y0,
                        cx + x0, cy + y0
                    )
                }

                val strand2 = Path().apply {
                    moveTo(cx - x0, cy + y0)
                    cubicTo(
                        cx - x0 + k * x0, cy + y0,
                        cx + x0 - k * x0, cy - y0,
                        cx + x0, cy - y0
                    )
                }

                val ribbonGlowStroke = Stroke(
                    width = ribbonStrokeW * 1.35f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )

                val ribbonMainStroke = Stroke(
                    width = ribbonStrokeW,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )

                val shadowPath = Path().apply {
                    moveTo(cx - x0 * 0.35f, cy + y0 * 0.35f)
                    cubicTo(
                        cx - x0 * 0.12f, cy + y0 * 0.12f,
                        cx + x0 * 0.12f, cy - y0 * 0.12f,
                        cx + x0 * 0.35f, cy - y0 * 0.35f
                    )
                }
                val shadowStroke = Stroke(
                    width = ribbonStrokeW * 1.25f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )

                onDrawBehind {
                    // 0. Dark inner fill
                    drawPath(path = innerFillPath, brush = innerFillBrush, style = Fill)

                    // 1. Soft neon glow around rim
                    drawPath(path = shieldPath, brush = shieldBrush, style = glowStroke, alpha = 0.40f)

                    // 2. Crisp main shield rim
                    drawPath(path = shieldPath, brush = shieldBrush, style = mainStroke)

                    // 3. Ribbon glow
                    drawPath(strand1, brush = ribbonBrush, style = ribbonGlowStroke, alpha = 0.38f)
                    drawPath(strand2, brush = ribbonBrush, style = ribbonGlowStroke, alpha = 0.38f)

                    // 4. Ribbon Strand 1 (underneath)
                    drawPath(strand1, brush = ribbonBrush, style = ribbonMainStroke)

                    // 5. Intersect drop-shadow
                    drawPath(shadowPath, color = Color(0xB0020612), style = shadowStroke)

                    // 6. Ribbon Strand 2 (on top)
                    drawPath(strand2, brush = ribbonBrush, style = ribbonMainStroke)
                }
            }
    )
}

private fun createShieldPath(w: Float, h: Float, inset: Float): Path {
    val effW = w - inset * 2f
    val effH = h - inset * 2f
    val crestH = effH * 0.045f
    val cr = effW * 0.16f

    val p = Path()
    p.moveTo(inset + effW * 0.50f, inset)

    p.quadraticTo(
        inset + effW * 0.78f,
        inset + crestH * 0.35f,
        inset + effW - cr,
        inset + crestH
    )
    p.quadraticTo(
        inset + effW,
        inset + crestH,
        inset + effW,
        inset + crestH + cr
    )
    p.lineTo(inset + effW, inset + effH * 0.38f)
    p.cubicTo(
        inset + effW,
        inset + effH * 0.70f,
        inset + effW * 0.80f,
        inset + effH * 0.88f,
        inset + effW * 0.50f,
        inset + effH
    )
    p.cubicTo(
        inset + effW * 0.20f,
        inset + effH * 0.88f,
        inset,
        inset + effH * 0.70f,
        inset,
        inset + effH * 0.38f
    )
    p.lineTo(inset, inset + crestH + cr)
    p.quadraticTo(
        inset,
        inset + crestH,
        inset + cr,
        inset + crestH
    )
    p.quadraticTo(
        inset + effW * 0.22f,
        inset + crestH * 0.35f,
        inset + effW * 0.50f,
        inset
    )
    p.close()
    return p
}
