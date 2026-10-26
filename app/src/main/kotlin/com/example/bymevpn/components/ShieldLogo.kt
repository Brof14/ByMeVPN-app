package com.example.bymevpn.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Authentic ByMeVPN Shield Logo matching the user's uploaded official brand identity:
 * - Geometric modern shield contour (Electric Blue on left -> Cyan at top -> Emerald/Neon Green on right)
 * - Intertwined infinity loop with 3D gradient crossover ribbon
 * - Atmospheric pulsating neon backglow
 */
@Composable
fun ShieldLogo(
    modifier: Modifier = Modifier,
    size: Dp = 180.dp,
    isActive: Boolean = true,
    pulseAlpha: Float = 1.0f,
    glowAlpha: Float = 1.0f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shield_ambient")
    val ambientPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient_pulse"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height

            // 1. Ambient Glow behind the shield when active
            if (isActive) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x3526E875),
                            Color(0x180062FF),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.5f, h * 0.5f),
                        radius = w * 0.58f * ambientPulse
                    ),
                    radius = w * 0.58f * ambientPulse,
                    center = Offset(w * 0.5f, h * 0.5f)
                )
            }

            // 2. Shield Path Definition
            val strokeW = w * 0.082f
            val halfStroke = strokeW / 2f

            fun buildShieldPath(offset: Float): Path {
                return Path().apply {
                    val top = offset
                    val bottom = h - offset
                    val left = offset
                    val right = w - offset
                    val midX = w * 0.5f

                    moveTo(left + (right - left) * 0.12f, top + (bottom - top) * 0.10f)
                    // Top curve gently arching up towards center
                    quadraticBezierTo(midX, top, right - (right - left) * 0.12f, top + (bottom - top) * 0.10f)
                    // Right vertical/curved flank
                    cubicTo(
                        right, top + (bottom - top) * 0.35f,
                        right - (right - left) * 0.08f, top + (bottom - top) * 0.65f,
                        midX, bottom
                    )
                    // Left flank curving from bottom point back to top-left
                    cubicTo(
                        left + (right - left) * 0.08f, top + (bottom - top) * 0.65f,
                        left, top + (bottom - top) * 0.35f,
                        left + (right - left) * 0.12f, top + (bottom - top) * 0.10f
                    )
                    close()
                }
            }

            val outerShield = buildShieldPath(halfStroke)
            val innerFill = buildShieldPath(halfStroke + 2f)

            // 3. Dark inner shield fill
            val fillBrush = Brush.verticalGradient(
                colors = if (isActive) {
                    listOf(Color(0xFF0F2228), Color(0xFF071014))
                } else {
                    listOf(Color(0xFF0C1418), Color(0xFF060A0C))
                },
                startY = 0f,
                endY = h
            )
            drawPath(innerFill, brush = fillBrush, style = Fill)

            // 4. Shield Border Gradient
            val shieldBorderBrush = if (isActive) {
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0055FF), // Electric Blue (bottom-left)
                        Color(0xFF00ACFF), // Sky Blue (mid-left)
                        Color(0xFF00FFA2), // Cyan / Mint (top-center)
                        Color(0xFF26E875)  // Neon Green (right)
                    ),
                    start = Offset(0f, h),
                    end = Offset(w, 0f)
                )
            } else {
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1E353D),
                        Color(0xFF284852),
                        Color(0xFF1E353D)
                    ),
                    start = Offset(0f, h),
                    end = Offset(w, 0f)
                )
            }

            // Outer soft glow stroke
            if (isActive) {
                drawPath(
                    outerShield,
                    brush = shieldBorderBrush,
                    style = Stroke(
                        width = strokeW * 1.4f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    ),
                    alpha = 0.4f * glowAlpha
                )
            }

            // Crisp main shield stroke
            drawPath(
                outerShield,
                brush = shieldBorderBrush,
                style = Stroke(
                    width = strokeW,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                ),
                alpha = pulseAlpha
            )

            // 5. Centered Infinity Ribbon (Crossover loop)
            val ribbonStroke = w * 0.088f
            val loopCx = w * 0.5f
            val loopCy = h * 0.44f
            val loopRx = w * 0.22f
            val loopRy = h * 0.13f

            val leftLoop = Path().apply {
                moveTo(loopCx, loopCy)
                cubicTo(
                    loopCx - loopRx * 0.6f, loopCy - loopRy * 1.25f,
                    loopCx - loopRx * 1.5f, loopCy - loopRy * 0.9f,
                    loopCx - loopRx * 1.5f, loopCy
                )
                cubicTo(
                    loopCx - loopRx * 1.5f, loopCy + loopRy * 0.9f,
                    loopCx - loopRx * 0.6f, loopCy + loopRy * 1.25f,
                    loopCx, loopCy
                )
            }

            val rightLoop = Path().apply {
                moveTo(loopCx, loopCy)
                cubicTo(
                    loopCx + loopRx * 0.6f, loopCy - loopRy * 1.25f,
                    loopCx + loopRx * 1.5f, loopCy - loopRy * 0.9f,
                    loopCx + loopRx * 1.5f, loopCy
                )
                cubicTo(
                    loopCx + loopRx * 1.5f, loopCy + loopRy * 0.9f,
                    loopCx + loopRx * 0.6f, loopCy + loopRy * 1.25f,
                    loopCx, loopCy
                )
            }

            val loopBrush = if (isActive) {
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0088FF),
                        Color(0xFF00E5FF),
                        Color(0xFF26E875)
                    ),
                    start = Offset(loopCx - loopRx * 1.5f, loopCy),
                    end = Offset(loopCx + loopRx * 1.5f, loopCy)
                )
            } else {
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF25404B),
                        Color(0xFF335766),
                        Color(0xFF25404B)
                    ),
                    start = Offset(loopCx - loopRx * 1.5f, loopCy),
                    end = Offset(loopCx + loopRx * 1.5f, loopCy)
                )
            }

            // Glow for infinity loop
            if (isActive) {
                drawPath(leftLoop, brush = loopBrush, style = Stroke(width = ribbonStroke * 1.35f, cap = StrokeCap.Round), alpha = 0.35f * glowAlpha)
                drawPath(rightLoop, brush = loopBrush, style = Stroke(width = ribbonStroke * 1.35f, cap = StrokeCap.Round), alpha = 0.35f * glowAlpha)
            }

            // Sharp infinity loops
            drawPath(leftLoop, brush = loopBrush, style = Stroke(width = ribbonStroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(rightLoop, brush = loopBrush, style = Stroke(width = ribbonStroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}
