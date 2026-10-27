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
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun PowerIcon(
    color: Color = Color.White,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeW = (size.value * 0.08f).coerceAtLeast(1.8f).dp.toPx()

        // Vertical line at top
        drawLine(
            color = color,
            start = Offset(w * 0.5f, h * 0.10f),
            end = Offset(w * 0.5f, h * 0.48f),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )

        // Circular arc from ~50 deg to ~310 deg
        val arcPath = Path().apply {
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(
                    left = w * 0.16f,
                    top = h * 0.16f,
                    right = w * 0.84f,
                    bottom = h * 0.88f
                ),
                startAngleDegrees = -60f,
                sweepAngleDegrees = 300f,
                forceMoveTo = false
            )
        }
        drawPath(arcPath, color = color, style = Stroke(strokeW, cap = StrokeCap.Round))
    }
}

@Composable
fun SettingsGearIcon(
    color: Color = Color.White,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val r = w * 0.38f
        val strokeW = 1.8.dp.toPx()
        val c = Offset(w / 2f, h / 2f)

        // Outer gear ring
        drawCircle(color = color, radius = r, center = c, style = Stroke(strokeW))
        // Center hole
        drawCircle(color = color, radius = r * 0.35f, center = c, style = Stroke(strokeW))

        // 6 cogs
        for (i in 0 until 6) {
            val angle = Math.toRadians((i * 60.0))
            val cosA = Math.cos(angle).toFloat()
            val sinA = Math.sin(angle).toFloat()
            drawLine(
                color = color,
                start = Offset(c.x + r * cosA, c.y + r * sinA),
                end = Offset(c.x + (r + 2.5.dp.toPx()) * cosA, c.y + (r + 2.5.dp.toPx()) * sinA),
                strokeWidth = strokeW * 1.5f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun PersonProfileIcon(
    color: Color = Color.White,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeW = 1.8.dp.toPx()

        // Head
        drawCircle(
            color = color,
            radius = w * 0.20f,
            center = Offset(w * 0.5f, h * 0.34f),
            style = Stroke(strokeW)
        )

        // Shoulders arc
        val shoulderPath = Path().apply {
            moveTo(w * 0.18f, h * 0.86f)
            cubicTo(
                w * 0.22f, h * 0.62f,
                w * 0.78f, h * 0.62f,
                w * 0.82f, h * 0.86f
            )
        }
        drawPath(shoulderPath, color = color, style = Stroke(strokeW, cap = StrokeCap.Round))
    }
}

@Composable
fun QuestionHelpIcon(
    color: Color = Color.White,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeW = 1.8.dp.toPx()
        val c = Offset(w / 2f, h / 2f)

        // Circle outline
        drawCircle(color = color, radius = w * 0.44f, center = c, style = Stroke(strokeW))

        // Question mark hook
        val qPath = Path().apply {
            moveTo(w * 0.38f, h * 0.38f)
            cubicTo(
                w * 0.38f, h * 0.25f,
                w * 0.62f, h * 0.25f,
                w * 0.62f, h * 0.40f
            )
            cubicTo(
                w * 0.62f, h * 0.50f,
                w * 0.50f, h * 0.52f,
                w * 0.50f, h * 0.62f
            )
        }
        drawPath(qPath, color = color, style = Stroke(strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Question mark dot
        drawCircle(color = color, radius = 1.3.dp.toPx(), center = Offset(w * 0.50f, h * 0.74f))
    }
}

@Composable
fun MoreHorizontalIcon(
    color: Color = Color.White,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val r = 1.8.dp.toPx()
        drawCircle(color = color, radius = r, center = Offset(w * 0.26f, h * 0.5f))
        drawCircle(color = color, radius = r, center = Offset(w * 0.50f, h * 0.5f))
        drawCircle(color = color, radius = r, center = Offset(w * 0.74f, h * 0.5f))
    }
}

@Composable
fun ChevronLeftIcon(
    color: Color = Color.White,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeW = 2.dp.toPx()
        val path = Path().apply {
            moveTo(w * 0.65f, h * 0.22f)
            lineTo(w * 0.35f, h * 0.50f)
            lineTo(w * 0.65f, h * 0.78f)
        }
        drawPath(path, color = color, style = Stroke(strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun CloseXIcon(
    color: Color = Color.White,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeW = 2.dp.toPx()
        drawLine(color = color, start = Offset(w * 0.25f, h * 0.25f), end = Offset(w * 0.75f, h * 0.75f), strokeWidth = strokeW, cap = StrokeCap.Round)
        drawLine(color = color, start = Offset(w * 0.75f, h * 0.25f), end = Offset(w * 0.25f, h * 0.75f), strokeWidth = strokeW, cap = StrokeCap.Round)
    }
}

@Composable
fun CheckmarkIcon(
    color: Color = Color(0xFF38EFB6),
    modifier: Modifier = Modifier,
    size: Dp = 18.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeW = 2.dp.toPx()
        val path = Path().apply {
            moveTo(w * 0.22f, h * 0.50f)
            lineTo(w * 0.44f, h * 0.72f)
            lineTo(w * 0.82f, h * 0.28f)
        }
        drawPath(path, color = color, style = Stroke(strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun RefreshIcon(
    color: Color = Color.White,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeW = 1.8.dp.toPx()
        val arcPath = Path().apply {
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(w * 0.18f, h * 0.18f, w * 0.82f, h * 0.82f),
                startAngleDegrees = 45f,
                sweepAngleDegrees = 270f,
                forceMoveTo = false
            )
        }
        drawPath(arcPath, color = color, style = Stroke(strokeW, cap = StrokeCap.Round))
        // arrow tip
        val tip = Path().apply {
            moveTo(w * 0.80f, h * 0.32f)
            lineTo(w * 0.88f, h * 0.18f)
            lineTo(w * 0.72f, h * 0.16f)
        }
        drawPath(tip, color = color, style = Stroke(strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun SmartphoneIcon(
    color: Color = Color.White,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeW = 1.8.dp.toPx()
        val r = RoundRect(w * 0.25f, h * 0.10f, w * 0.75f, h * 0.90f, 3.dp.toPx(), 3.dp.toPx())
        drawPath(Path().apply { addRoundRect(r) }, color = color, style = Stroke(strokeW))
        drawLine(color = color, start = Offset(w * 0.44f, h * 0.80f), end = Offset(w * 0.56f, h * 0.80f), strokeWidth = strokeW, cap = StrokeCap.Round)
    }
}

@Composable
fun DevicesMultiIcon(
    color: Color = Color.White,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeW = 1.6.dp.toPx()

        // Laptop / desktop monitor
        val r = RoundRect(w * 0.10f, h * 0.20f, w * 0.65f, h * 0.68f, 2.dp.toPx(), 2.dp.toPx())
        drawPath(Path().apply { addRoundRect(r) }, color = color, style = Stroke(strokeW))
        drawLine(color = color, start = Offset(w * 0.25f, h * 0.82f), end = Offset(w * 0.50f, h * 0.82f), strokeWidth = strokeW, cap = StrokeCap.Round)
        drawLine(color = color, start = Offset(w * 0.375f, h * 0.68f), end = Offset(w * 0.375f, h * 0.82f), strokeWidth = strokeW)

        // Phone beside it
        val phoneR = RoundRect(w * 0.68f, h * 0.35f, w * 0.90f, h * 0.82f, 2.dp.toPx(), 2.dp.toPx())
        drawPath(Path().apply { addRoundRect(phoneR) }, color = color, style = Stroke(strokeW))
    }
}

@Composable
fun CreditCardIcon(
    color: Color = Color.White,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeW = 1.8.dp.toPx()
        val r = RoundRect(w * 0.10f, h * 0.22f, w * 0.90f, h * 0.78f, 3.dp.toPx(), 3.dp.toPx())
        drawPath(Path().apply { addRoundRect(r) }, color = color, style = Stroke(strokeW))
        drawLine(color = color, start = Offset(w * 0.10f, h * 0.42f), end = Offset(w * 0.90f, h * 0.42f), strokeWidth = strokeW)
    }
}

@Composable
fun ShieldSecurityIcon(
    color: Color = Color.White,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeW = 1.8.dp.toPx()
        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.12f)
            lineTo(w * 0.85f, h * 0.25f)
            lineTo(w * 0.85f, h * 0.58f)
            cubicTo(w * 0.85f, h * 0.78f, w * 0.5f, h * 0.90f, w * 0.5f, h * 0.90f)
            cubicTo(w * 0.5f, h * 0.90f, w * 0.15f, h * 0.78f, w * 0.15f, h * 0.58f)
            lineTo(w * 0.15f, h * 0.25f)
            close()
        }
        drawPath(path, color = color, style = Stroke(strokeW, join = StrokeJoin.Round))
    }
}

@Composable
fun SparkleStarIcon(
    color: Color = Color.White,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.10f)
            lineTo(w * 0.60f, h * 0.40f)
            lineTo(w * 0.90f, h * 0.50f)
            lineTo(w * 0.60f, h * 0.60f)
            lineTo(w * 0.50f, h * 0.90f)
            lineTo(w * 0.40f, h * 0.60f)
            lineTo(w * 0.10f, h * 0.50f)
            lineTo(w * 0.40f, h * 0.40f)
            close()
        }
        drawPath(path, color = color, style = Fill)
    }
}

@Composable
fun InfoCircleIcon(
    color: Color = Color.White,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeW = 1.6.dp.toPx()
        val c = Offset(w / 2f, h / 2f)
        drawCircle(color = color, radius = w * 0.44f, center = c, style = Stroke(strokeW))
        drawLine(color = color, start = Offset(w * 0.50f, h * 0.45f), end = Offset(w * 0.50f, h * 0.74f), strokeWidth = strokeW, cap = StrokeCap.Round)
        drawCircle(color = color, radius = 1.2.dp.toPx(), center = Offset(w * 0.50f, h * 0.30f))
    }
}

@Composable
fun ChatHeartIcon(
    color: Color = Color.White,
    modifier: Modifier = Modifier,
    size: Dp = 26.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeW = 1.8.dp.toPx()
        val chatPath = Path().apply {
            moveTo(w * 0.85f, h * 0.65f)
            cubicTo(w * 0.92f, h * 0.52f, w * 0.92f, h * 0.35f, w * 0.80f, h * 0.22f)
            cubicTo(w * 0.65f, h * 0.10f, w * 0.35f, h * 0.10f, w * 0.20f, h * 0.22f)
            cubicTo(w * 0.08f, h * 0.35f, w * 0.08f, h * 0.55f, w * 0.20f, h * 0.68f)
            lineTo(w * 0.18f, h * 0.88f)
            lineTo(w * 0.38f, h * 0.80f)
            cubicTo(w * 0.42f, h * 0.82f, w * 0.46f, h * 0.83f, w * 0.50f, h * 0.83f)
            close()
        }
        drawPath(chatPath, color = color, style = Stroke(strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun BellNewsIcon(
    color: Color = Color.White,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeW = 1.8.dp.toPx()
        val bellPath = Path().apply {
            moveTo(w * 0.25f, h * 0.70f)
            lineTo(w * 0.75f, h * 0.70f)
            lineTo(w * 0.70f, h * 0.40f)
            cubicTo(w * 0.70f, h * 0.25f, w * 0.62f, h * 0.16f, w * 0.50f, h * 0.16f)
            cubicTo(w * 0.38f, h * 0.16f, w * 0.30f, h * 0.25f, w * 0.30f, h * 0.40f)
            close()
        }
        drawPath(bellPath, color = color, style = Stroke(strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(color = color, start = Offset(w * 0.18f, h * 0.72f), end = Offset(w * 0.82f, h * 0.72f), strokeWidth = strokeW, cap = StrokeCap.Round)
        drawCircle(color = color, radius = 1.8.dp.toPx(), center = Offset(w * 0.50f, h * 0.84f))
    }
}
