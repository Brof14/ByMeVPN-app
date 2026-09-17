package com.example.bymevpn.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.bymevpn.theme.AppColors
import com.example.bymevpn.theme.AppGradients

/**
 * Premium ByMeVPN Shield Logo drawn entirely with Jetpack Compose Canvas.
 * Matches the original Flutter CustomPainter pixel-for-pixel with enhanced Android animations.
 */
@Composable
fun ShieldLogo(
    modifier: Modifier = Modifier,
    size: Dp = 210.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shield_pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Canvas(
        modifier = modifier
            .size(width = size, height = size * 1.18f)
            .testTag("shield_logo")
            .semantics { contentDescription = "ByMeVPN Shield Logo with Infinity Symbol" }
    ) {
        val w = this.size.width
        val h = this.size.height

        // 1. Outer rim glow
        val rimPath = createShieldPath(w, h)
        drawPath(
            path = rimPath,
            brush = AppGradients.ShieldRim,
            alpha = glowAlpha
        )

        // 2. Premium metallic 3D body
        val inset = 0.12f
        val iW = w * (1f - inset * 2f)
        val iH = h * (1f - inset * 2.2f)
        translate(left = w * inset, top = h * inset) {
            val metallicPath = createShieldPath(iW, iH)
            drawPath(
                path = metallicPath,
                brush = AppGradients.ShieldMetallic
            )
        }

        // 3. Glassmorphism overlay
        val glassInset = 0.08f
        val gW = w * (1f - glassInset * 2f)
        val gH = h * (1f - glassInset * 2f)
        translate(left = w * glassInset, top = h * glassInset) {
            val glassPath = createShieldPath(gW, gH)
            drawPath(
                path = glassPath,
                brush = AppGradients.ShieldGlass,
                blendMode = BlendMode.SrcOver
            )
        }

        // 4. Accent rim stroke
        val glowInset = 0.02f
        val glowW = w * (1f - glowInset * 2f)
        val glowH = h * (1f - glowInset * 2f)
        translate(left = w * glowInset, top = h * glowInset) {
            val glowStrokePath = createShieldPath(glowW, glowH)
            drawPath(
                path = glowStrokePath,
                brush = AppGradients.ShieldRim,
                style = Stroke(width = w * 0.035f),
                alpha = 0.85f
            )
        }

        // 5. Infinity symbol (∞)
        drawInfinity(w, h)
    }
}

/**
 * Builds the mathematical shield outline inside a [w] x [h] bounding box.
 */
private fun createShieldPath(w: Float, h: Float): Path {
    val cr = 0.13f // Corner-radius fraction of width
    val p = Path()
    p.moveTo(w * cr, 0f)
    p.lineTo(w * (1f - cr), 0f)
    p.quadraticTo(w, 0f, w, h * cr)
    p.lineTo(w, h * 0.44f)
    p.cubicTo(w, h * 0.72f, w * 0.78f, h * 0.88f, w * 0.5f, h * 0.985f)
    p.cubicTo(w * 0.22f, h * 0.88f, 0f, h * 0.72f, 0f, h * 0.44f)
    p.lineTo(0f, h * cr)
    p.quadraticTo(0f, 0f, w * cr, 0f)
    p.close()
    return p
}

/**
 * Draws the centered infinity symbol using dual cubic Bézier loops.
 */
private fun DrawScope.drawInfinity(w: Float, h: Float) {
    val cx = w * 0.50f
    val cy = h * 0.465f

    val lx = w * 0.22f
    val ly = h * 0.11f
    val strokeWidth = w * 0.085f

    val infinityBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF1A7FFF),
            Color(0xFF007BFF),
            Color(0xFF00FFFF),
            Color(0xFF00FF88)
        ),
        start = Offset(cx - lx * 2f, cy),
        end = Offset(cx + lx * 2f, cy)
    )

    val stroke = Stroke(
        width = strokeWidth,
        cap = StrokeCap.Round,
        join = StrokeJoin.Round
    )

    // Left loop
    val left = Path().apply {
        moveTo(cx, cy)
        cubicTo(
            cx - lx * 0.5f, cy - ly * 2.2f,
            cx - lx * 2.0f, cy - ly * 2.2f,
            cx - lx * 2.0f, cy
        )
        cubicTo(
            cx - lx * 2.0f, cy + ly * 2.2f,
            cx - lx * 0.5f, cy + ly * 2.2f,
            cx, cy
        )
    }

    // Right loop
    val right = Path().apply {
        moveTo(cx, cy)
        cubicTo(
            cx + lx * 0.5f, cy - ly * 2.2f,
            cx + lx * 2.0f, cy - ly * 2.2f,
            cx + lx * 2.0f, cy
        )
        cubicTo(
            cx + lx * 2.0f, cy + ly * 2.2f,
            cx + lx * 0.5f, cy + ly * 2.2f,
            cx, cy
        )
    }

    drawPath(left, brush = infinityBrush, style = stroke)
    drawPath(right, brush = infinityBrush, style = stroke)
}
