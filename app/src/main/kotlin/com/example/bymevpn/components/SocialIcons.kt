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

/**
 * High-contrast Telegram plane vector icon.
 */
@Composable
fun TelegramLogoIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    color: Color = Color(0xFF2AABEE)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        val p = Path().apply {
            moveTo(w * 0.15f, h * 0.48f)
            lineTo(w * 0.85f, h * 0.18f)
            lineTo(w * 0.72f, h * 0.82f)
            lineTo(w * 0.46f, h * 0.62f)
            lineTo(w * 0.38f, h * 0.70f)
            lineTo(w * 0.40f, h * 0.55f)
            lineTo(w * 0.75f, h * 0.28f)
            lineTo(w * 0.32f, h * 0.52f)
            close()
        }
        drawPath(path = p, color = color, style = Fill)
    }
}

/**
 * YouTube play button vector icon.
 */
@Composable
fun YouTubeLogoIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Outer rounded red rect
        val rectPath = Path().apply {
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    left = 0f,
                    top = h * 0.15f,
                    right = w,
                    bottom = h * 0.85f,
                    radiusX = 6.dp.toPx(),
                    radiusY = 6.dp.toPx()
                )
            )
        }
        drawPath(path = rectPath, color = Color(0xFFFF0000), style = Fill)

        // Inner white play triangle
        val triPath = Path().apply {
            moveTo(w * 0.40f, h * 0.35f)
            lineTo(w * 0.68f, h * 0.50f)
            lineTo(w * 0.40f, h * 0.65f)
            close()
        }
        drawPath(path = triPath, color = Color.White, style = Fill)
    }
}

/**
 * Website globe vector icon with subtle glowing cyan tint.
 */
@Composable
fun WebsiteLogoIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    color: Color = Color(0xFF00D4FF)
) {
    GlobeIcon(
        modifier = modifier,
        color = color,
        size = size
    )
}

/**
 * Chat Support vector icon (speech bubble with dot indicator).
 */
@Composable
fun ChatSupportIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    color: Color = Color(0xFF00D4FF)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        val bubble = Path().apply {
            moveTo(w * 0.16f, h * 0.20f)
            lineTo(w * 0.84f, h * 0.20f)
            quadraticTo(w * 0.94f, h * 0.20f, w * 0.94f, h * 0.35f)
            lineTo(w * 0.94f, h * 0.65f)
            quadraticTo(w * 0.94f, h * 0.80f, w * 0.84f, h * 0.80f)
            lineTo(w * 0.45f, h * 0.80f)
            lineTo(w * 0.22f, h * 0.94f)
            lineTo(w * 0.26f, h * 0.80f)
            lineTo(w * 0.16f, h * 0.80f)
            quadraticTo(w * 0.06f, h * 0.80f, w * 0.06f, h * 0.65f)
            lineTo(w * 0.06f, h * 0.35f)
            quadraticTo(w * 0.06f, h * 0.20f, w * 0.16f, h * 0.20f)
            close()
        }
        drawPath(path = bubble, color = color, style = Fill)

        val dotRadius = w * 0.045f
        val dotY = h * 0.50f
        drawCircle(color = Color(0xFF081224), radius = dotRadius, center = Offset(w * 0.34f, dotY))
        drawCircle(color = Color(0xFF081224), radius = dotRadius, center = Offset(w * 0.50f, dotY))
        drawCircle(color = Color(0xFF081224), radius = dotRadius, center = Offset(w * 0.66f, dotY))
    }
}

