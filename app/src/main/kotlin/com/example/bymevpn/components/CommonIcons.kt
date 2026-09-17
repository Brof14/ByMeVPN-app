package com.example.bymevpn.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

/**
 * Globe vector icon for Servers navigation tab.
 */
@Composable
fun GlobeIcon(
    color: Color = Color(0xFF26E875),
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val r = w * 0.44f
        val center = Offset(w / 2f, h / 2f)
        val strokeW = 1.8.dp.toPx()

        // Outer circle
        drawCircle(color = color, radius = r, center = center, style = Stroke(strokeW))

        // Horizontal equator line
        drawLine(
            color = color,
            start = Offset(center.x - r, center.y),
            end = Offset(center.x + r, center.y),
            strokeWidth = strokeW
        )

        // Vertical meridian ellipse
        val meridianPath = Path().apply {
            moveTo(center.x, center.y - r)
            cubicTo(
                center.x - r * 0.55f, center.y - r * 0.5f,
                center.x - r * 0.55f, center.y + r * 0.5f,
                center.x, center.y + r
            )
            cubicTo(
                center.x + r * 0.55f, center.y + r * 0.5f,
                center.x + r * 0.55f, center.y - r * 0.5f,
                center.x, center.y - r
            )
        }
        drawPath(meridianPath, color = color, style = Stroke(strokeW))
    }
}

/**
 * Account/User profile vector icon.
 */
@Composable
fun AccountIcon(
    color: Color = Color(0xFF94A3B8),
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val r = w * 0.44f
        val center = Offset(w / 2f, h / 2f)
        val strokeW = 1.8.dp.toPx()

        // Outer circle
        drawCircle(color = color, radius = r, center = center, style = Stroke(strokeW))

        // Head
        drawCircle(
            color = color,
            radius = r * 0.35f,
            center = Offset(center.x, center.y - r * 0.22f),
            style = Stroke(strokeW)
        )

        // Torso arc
        val torsoPath = Path().apply {
            moveTo(center.x - r * 0.65f, center.y + r * 0.62f)
            cubicTo(
                center.x - r * 0.45f, center.y + r * 0.15f,
                center.x + r * 0.45f, center.y + r * 0.15f,
                center.x + r * 0.65f, center.y + r * 0.62f
            )
        }
        drawPath(torsoPath, color = color, style = Stroke(strokeW, cap = StrokeCap.Round))
    }
}

/**
 * Change Server / Location switch vector icon.
 */
@Composable
fun ChangeServerIcon(
    color: Color = Color.White,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeW = 1.8.dp.toPx()

        // Server rack box with small horizontal lines
        val r = RoundRect(
            left = 1f,
            top = 2f,
            right = w - 1f,
            bottom = h - 2f,
            radiusX = 3.dp.toPx(),
            radiusY = 3.dp.toPx()
        )
        val path = Path().apply { addRoundRect(r) }
        drawPath(path, color = color, style = Stroke(strokeW))

        // Division line
        drawLine(
            color = color,
            start = Offset(2f, h * 0.5f),
            end = Offset(w - 2f, h * 0.5f),
            strokeWidth = strokeW
        )

        // Indicator dots
        drawCircle(color = color, radius = 1.4.dp.toPx(), center = Offset(w * 0.25f, h * 0.26f))
        drawCircle(color = color, radius = 1.4.dp.toPx(), center = Offset(w * 0.25f, h * 0.74f))
        drawLine(
            color = color,
            start = Offset(w * 0.45f, h * 0.26f),
            end = Offset(w * 0.80f, h * 0.26f),
            strokeWidth = 1.4.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(w * 0.45f, h * 0.74f),
            end = Offset(w * 0.80f, h * 0.74f),
            strokeWidth = 1.4.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

/**
 * Vector rendering for country flag pill (e.g. Netherlands flag matching reference image).
 */
@Composable
fun CountryFlagView(
    countryCode: String,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp
) {
    Box(
        modifier = modifier
            .size(width = size * 1.4f, height = size)
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(4.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height

            when (countryCode.uppercase()) {
                "NL" -> {
                    // Netherlands: Red, White, Blue horizontal
                    val stripeH = h / 3f
                    drawRect(color = Color(0xFFA21B27), topLeft = Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(w, stripeH))
                    drawRect(color = Color(0xFFFFFFFF), topLeft = Offset(0f, stripeH), size = androidx.compose.ui.geometry.Size(w, stripeH))
                    drawRect(color = Color(0xFF1E4582), topLeft = Offset(0f, stripeH * 2f), size = androidx.compose.ui.geometry.Size(w, stripeH))
                }
                "DE" -> {
                    // Germany: Black, Red, Gold horizontal
                    val stripeH = h / 3f
                    drawRect(color = Color(0xFF111111), topLeft = Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(w, stripeH))
                    drawRect(color = Color(0xFFDD0000), topLeft = Offset(0f, stripeH), size = androidx.compose.ui.geometry.Size(w, stripeH))
                    drawRect(color = Color(0xFFFFCE00), topLeft = Offset(0f, stripeH * 2f), size = androidx.compose.ui.geometry.Size(w, stripeH))
                }
                "US" -> {
                    // USA: Red & white stripes + blue canton
                    val stripeH = h / 5f
                    for (i in 0..4) {
                        val c = if (i % 2 == 0) Color(0xFFB22234) else Color(0xFFFFFFFF)
                        drawRect(color = c, topLeft = Offset(0f, i * stripeH), size = androidx.compose.ui.geometry.Size(w, stripeH))
                    }
                    drawRect(color = Color(0xFF3C3B6E), topLeft = Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(w * 0.42f, stripeH * 3f))
                }
                "GB" -> {
                    // UK: Blue background with cross
                    drawRect(color = Color(0xFF012169), topLeft = Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(w, h))
                    drawLine(color = Color.White, start = Offset(0f, 0f), end = Offset(w, h), strokeWidth = 3.dp.toPx())
                    drawLine(color = Color.White, start = Offset(w, 0f), end = Offset(0f, h), strokeWidth = 3.dp.toPx())
                    drawRect(color = Color.White, topLeft = Offset(w * 0.38f, 0f), size = androidx.compose.ui.geometry.Size(w * 0.24f, h))
                    drawRect(color = Color.White, topLeft = Offset(0f, h * 0.35f), size = androidx.compose.ui.geometry.Size(w, h * 0.30f))
                    drawRect(color = Color(0xFFC8102E), topLeft = Offset(w * 0.43f, 0f), size = androidx.compose.ui.geometry.Size(w * 0.14f, h))
                    drawRect(color = Color(0xFFC8102E), topLeft = Offset(0f, h * 0.40f), size = androidx.compose.ui.geometry.Size(w, h * 0.20f))
                }
                "JP" -> {
                    // Japan: White with red circle
                    drawRect(color = Color(0xFFFFFFFF), topLeft = Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(w, h))
                    drawCircle(color = Color(0xFFBC002D), radius = h * 0.32f, center = Offset(w / 2f, h / 2f))
                }
                else -> {
                    drawRect(color = Color(0xFF1E293B), topLeft = Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(w, h))
                }
            }
        }
    }
}

