package com.example.bymevpn.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bymevpn.theme.AppColors
import kotlin.math.sin

/**
 * Animated real-time telemetry speed wave rendered in the ByMeVPN logo palette
 * with hoisted Stroke and Brush objects to avoid per-frame allocations.
 */
@Composable
fun LiveSpeedWave(
    bytesIn: Long,
    bytesOut: Long,
    downloadBps: Long = 0L,
    uploadBps: Long = 0L,
    isConnected: Boolean,
    isRussian: Boolean = true,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave_phase")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isConnected) 1600 else 6000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase_anim"
    )

    val downBitrateStr = if (downloadBps > 0) formatBitrate(downloadBps, isRussian) else formatVolume(bytesIn, isConnected, isRussian)
    val upBitrateStr = if (uploadBps > 0) formatBitrate(uploadBps, isRussian) else formatVolume(bytesOut, isConnected, isRussian)
    val totalVolumeStr = "↓ ${formatVolume(bytesIn, isConnected, isRussian)} • ↑ ${formatVolume(bytesOut, isConnected, isRussian)}"

    val density = LocalDensity.current
    val wavePath = remember { Path() }
    val wavePath2 = remember { Path() }
    val downStroke = remember(density) {
        Stroke(width = with(density) { 2.dp.toPx() }, cap = StrokeCap.Round)
    }
    val upStroke = remember(density) {
        Stroke(width = with(density) { 1.5.dp.toPx() }, cap = StrokeCap.Round)
    }
    val idleLinePx = remember(density) { with(density) { 1.dp.toPx() } }
    val downBrush = remember {
        Brush.horizontalGradient(
            listOf(AppColors.SkyBlue, AppColors.BrandMint, AppColors.BrandMintBright)
        )
    }
    val upBrush = remember {
        Brush.horizontalGradient(
            listOf(AppColors.BrandBlue.copy(alpha = 0.75f), AppColors.SkyBlue.copy(alpha = 0.70f))
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.BgSurface)
            .border(1.dp, AppColors.BorderDefault, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
        ) {
            val w = size.width
            val h = size.height
            val midY = h * 0.5f

            if (isConnected) {
                wavePath.rewind()
                wavePath2.rewind()

                val points = 32
                val ampFactor = if (downloadBps > 100_000L) 1.25f else 1.0f

                for (i in 0..points) {
                    val x = (w / points) * i
                    val progress = i.toFloat() / points
                    val envelope = sin(progress * Math.PI.toFloat())
                    val y = midY + sin(progress * 8f + phase) * (h * 0.40f * ampFactor) * envelope
                    val y2 = midY + sin(progress * 6f - phase) * (h * 0.30f * ampFactor) * envelope

                    if (i == 0) {
                        wavePath.moveTo(x, y)
                        wavePath2.moveTo(x, y2)
                    } else {
                        wavePath.lineTo(x, y)
                        wavePath2.lineTo(x, y2)
                    }
                }

                drawPath(path = wavePath, brush = downBrush, style = downStroke)
                drawPath(path = wavePath2, brush = upBrush, style = upStroke)
            } else {
                drawLine(
                    color = AppColors.BorderDefault,
                    start = Offset(0f, midY),
                    end = Offset(w, midY),
                    strokeWidth = idleLinePx,
                    cap = StrokeCap.Round
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isConnected) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("↓", color = AppColors.BrandMintBright, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = downBitrateStr,
                        color = AppColors.TextPrimary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("↑", color = AppColors.SkyBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = upBitrateStr,
                        color = AppColors.TextPrimary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isRussian) "Всего передано: $totalVolumeStr" else "Transferred: $totalVolumeStr",
                color = AppColors.TextMuted,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

internal fun formatBitrate(bps: Long, isRussian: Boolean): String {
    if (bps <= 0L) return if (isRussian) "0.0 Кбит/с" else "0.0 Kbps"
    val kbit = (bps * 8) / 1024.0
    val mbit = kbit / 1024.0
    val unitM = if (isRussian) "Мбит/с" else "Mbps"
    val unitK = if (isRussian) "Кбит/с" else "Kbps"
    return if (mbit >= 1.0) {
        String.format(java.util.Locale.US, "%.1f %s", mbit, unitM)
    } else {
        String.format(java.util.Locale.US, "%.0f %s", kbit, unitK)
    }
}

internal fun formatVolume(totalBytes: Long, isConnected: Boolean, isRussian: Boolean): String {
    val unitMb = if (isRussian) "МБ" else "MB"
    val unitGb = if (isRussian) "ГБ" else "GB"
    if (!isConnected || totalBytes <= 0L) return "0 $unitMb"
    val mb = totalBytes.toDouble() / (1024.0 * 1024.0)
    return if (mb < 1000.0) {
        String.format(java.util.Locale.US, "%.1f %s", mb, unitMb)
    } else {
        val gb = mb / 1024.0
        String.format(java.util.Locale.US, "%.2f %s", gb, unitGb)
    }
}
