package com.example.bymevpn.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Authentic Google 'G' icon in official Google 4-colors.
 */
@Composable
fun GoogleLogoIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val center = Offset(w / 2f, h / 2f)
        val radius = w / 2f
        val strokeW = w * 0.22f

        val arcRect = Rect(
            left = center.x - radius + strokeW / 2f,
            top = center.y - radius + strokeW / 2f,
            right = center.x + radius - strokeW / 2f,
            bottom = center.y + radius - strokeW / 2f
        )

        // Blue arc
        drawArc(
            color = Color(0xFF4285F4),
            startAngle = -45f,
            sweepAngle = 75f,
            useCenter = false,
            topLeft = arcRect.topLeft,
            size = arcRect.size,
            style = Stroke(width = strokeW, cap = StrokeCap.Butt)
        )

        // Green arc
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 30f,
            sweepAngle = 80f,
            useCenter = false,
            topLeft = arcRect.topLeft,
            size = arcRect.size,
            style = Stroke(width = strokeW, cap = StrokeCap.Butt)
        )

        // Yellow arc
        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = 110f,
            sweepAngle = 85f,
            useCenter = false,
            topLeft = arcRect.topLeft,
            size = arcRect.size,
            style = Stroke(width = strokeW, cap = StrokeCap.Butt)
        )

        // Red arc
        drawArc(
            color = Color(0xFFEA4335),
            startAngle = 195f,
            sweepAngle = 80f,
            useCenter = false,
            topLeft = arcRect.topLeft,
            size = arcRect.size,
            style = Stroke(width = strokeW, cap = StrokeCap.Butt)
        )

        // Blue horizontal crossbar
        drawRect(
            color = Color(0xFF4285F4),
            topLeft = Offset(center.x, center.y - strokeW / 2f),
            size = androidx.compose.ui.geometry.Size(radius, strokeW)
        )
    }
}

/**
 * Authentic Apple silhouette icon in pure white.
 */
@Composable
fun AppleLogoIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    color: Color = Color.White
) {
    Canvas(modifier = modifier.size(size)) {
        val sw = this.size.width
        val sh = this.size.height

        val p = Path().apply {
            // Leaf
            moveTo(sw * 0.54f, sh * 0.05f)
            cubicTo(sw * 0.60f, sh * 0.05f, sw * 0.68f, sh * 0.12f, sw * 0.66f, sh * 0.22f)
            cubicTo(sw * 0.58f, sh * 0.22f, sw * 0.50f, sh * 0.15f, sw * 0.54f, sh * 0.05f)
            close()

            // Body
            moveTo(sw * 0.65f, sh * 0.26f)
            cubicTo(sw * 0.56f, sh * 0.26f, sw * 0.49f, sh * 0.31f, sw * 0.42f, sh * 0.31f)
            cubicTo(sw * 0.35f, sh * 0.31f, sw * 0.27f, sh * 0.26f, sw * 0.18f, sh * 0.26f)
            cubicTo(sw * 0.05f, sh * 0.26f, 0.0f, sh * 0.41f, 0.0f, sh * 0.60f)
            cubicTo(0.0f, sh * 0.77f, sw * 0.11f, sh * 0.95f, sw * 0.21f, sh * 0.95f)
            cubicTo(sw * 0.28f, sh * 0.95f, sw * 0.32f, sh * 0.89f, sw * 0.42f, sh * 0.89f)
            cubicTo(sw * 0.52f, sh * 0.89f, sw * 0.56f, sh * 0.95f, sw * 0.63f, sh * 0.95f)
            cubicTo(sw * 0.73f, sh * 0.95f, sw * 0.82f, sh * 0.79f, sw * 0.86f, sh * 0.72f)
            // Bite
            cubicTo(sw * 0.73f, sh * 0.67f, sw * 0.73f, sh * 0.49f, sw * 0.86f, sh * 0.43f)
            cubicTo(sw * 0.80f, sh * 0.33f, sw * 0.71f, sh * 0.26f, sw * 0.65f, sh * 0.26f)
            close()
        }

        drawPath(path = p, color = color, style = Fill)
    }
}
