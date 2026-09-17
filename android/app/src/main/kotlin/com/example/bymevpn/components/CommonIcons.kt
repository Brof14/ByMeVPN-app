package com.example.bymevpn.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * High-contrast Mail vector icon.
 */
@Composable
fun MailIcon(
    color: Color = Color(0xFF94A3B8),
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeW = 1.6.dp.toPx()

        val r = RoundRect(
            left = 1f,
            top = h * 0.15f,
            right = w - 1f,
            bottom = h * 0.85f,
            radiusX = 3.dp.toPx(),
            radiusY = 3.dp.toPx()
        )
        val path = Path().apply { addRoundRect(r) }
        drawPath(path, color = color, style = Stroke(strokeW))

        val foldPath = Path().apply {
            moveTo(1f, h * 0.20f)
            lineTo(w / 2f, h * 0.55f)
            lineTo(w - 1f, h * 0.20f)
        }
        drawPath(foldPath, color = color, style = Stroke(strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/**
 * High-contrast Lock vector icon.
 */
@Composable
fun LockIcon(
    color: Color = Color(0xFF94A3B8),
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeW = 1.6.dp.toPx()

        // Shackle
        val shacklePath = Path().apply {
            moveTo(w * 0.30f, h * 0.45f)
            lineTo(w * 0.30f, h * 0.25f)
            cubicTo(w * 0.30f, h * 0.08f, w * 0.70f, h * 0.08f, w * 0.70f, h * 0.25f)
            lineTo(w * 0.70f, h * 0.45f)
        }
        drawPath(shacklePath, color = color, style = Stroke(strokeW, cap = StrokeCap.Round))

        // Body
        val bodyRect = RoundRect(
            left = w * 0.18f,
            top = h * 0.42f,
            right = w * 0.82f,
            bottom = h * 0.90f,
            radiusX = 3.dp.toPx(),
            radiusY = 3.dp.toPx()
        )
        val bodyPath = Path().apply { addRoundRect(bodyRect) }
        drawPath(bodyPath, color = color, style = Stroke(strokeW))

        // Keyhole
        drawCircle(
            color = color,
            radius = 1.8.dp.toPx(),
            center = Offset(w * 0.50f, h * 0.62f)
        )
    }
}

/**
 * Password Visibility Eye Icon.
 */
@Composable
fun EyeIcon(
    visible: Boolean,
    color: Color = Color(0xFF94A3B8),
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeW = 1.6.dp.toPx()

        val eyePath = Path().apply {
            moveTo(w * 0.10f, h * 0.50f)
            cubicTo(w * 0.30f, h * 0.22f, w * 0.70f, h * 0.22f, w * 0.90f, h * 0.50f)
            cubicTo(w * 0.70f, h * 0.78f, w * 0.30f, h * 0.78f, w * 0.10f, h * 0.50f)
            close()
        }
        drawPath(eyePath, color = color, style = Stroke(strokeW))

        drawCircle(
            color = color,
            radius = 2.5.dp.toPx(),
            center = Offset(w * 0.50f, h * 0.50f)
        )

        if (!visible) {
            drawLine(
                color = color,
                start = Offset(w * 0.18f, h * 0.20f),
                end = Offset(w * 0.82f, h * 0.80f),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )
        }
    }
}
