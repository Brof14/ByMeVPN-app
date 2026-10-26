package com.example.bymevpn.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bymevpn.theme.AppColors
import kotlin.math.sin

/**
 * Animated real-time telemetry speed wave:
 * Displays responsive live waves of network activity and Mbit/s speed indicators.
 */
@Composable
fun LiveSpeedWave(
    bytesIn: Long,
    bytesOut: Long,
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave_phase")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f, // 2*PI
        animationSpec = infiniteRepeatable(
            animation = tween(if (isConnected) 1800 else 6000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase_anim"
    )

    // Calculate approximate speed in MB or dynamic readable rate
    val downSpeedStr = formatSpeed(bytesIn, isConnected)
    val upSpeedStr = formatSpeed(bytesOut, isConnected)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(36.dp)
        ) {
            val w = size.width
            val h = size.height
            val midY = h * 0.5f

            if (isConnected) {
                val wavePath = Path()
                val wavePath2 = Path()

                val points = 32
                for (i in 0..points) {
                    val x = (w / points) * i
                    val progress = i.toFloat() / points
                    // Envelope tapering off at ends
                    val envelope = sin(progress * Math.PI.toFloat())
                    val y = midY + sin(progress * 8f + phase) * (h * 0.38f) * envelope
                    val y2 = midY + sin(progress * 6f - phase) * (h * 0.28f) * envelope

                    if (i == 0) {
                        wavePath.moveTo(x, y)
                        wavePath2.moveTo(x, y2)
                    } else {
                        wavePath.lineTo(x, y)
                        wavePath2.lineTo(x, y2)
                    }
                }

                // Download Wave (Mint/Green)
                drawPath(
                    path = wavePath,
                    color = AppColors.BrandGreen,
                    style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
                )

                // Upload Wave (Cyan/Blue)
                drawPath(
                    path = wavePath2,
                    color = AppColors.SkyBlue.copy(alpha = 0.65f),
                    style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
                )
            } else {
                // Subtle quiet baseline
                drawLine(
                    color = AppColors.CardBorder,
                    start = Offset(0f, midY),
                    end = Offset(w, midY),
                    strokeWidth = 1.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (isConnected) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("↓", color = AppColors.BrandGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = downSpeedStr,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("↑", color = AppColors.SkyBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = upSpeedStr,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

private fun formatSpeed(totalBytes: Long, isConnected: Boolean): String {
    if (!isConnected || totalBytes <= 0L) return "0.0 Мб"
    val mb = totalBytes.toDouble() / (1024.0 * 1024.0)
    return if (mb < 1000.0) {
        String.format(java.util.Locale.US, "%.1f Мб", mb)
    } else {
        val gb = mb / 1024.0
        String.format(java.util.Locale.US, "%.2f Гб", gb)
    }
}
