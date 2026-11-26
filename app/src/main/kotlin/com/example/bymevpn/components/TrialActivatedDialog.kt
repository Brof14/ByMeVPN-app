package com.example.bymevpn.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.bymevpn.theme.AppColors
import com.example.bymevpn.theme.AppGradients

@Composable
fun TrialActivatedDialog(
    isRussian: Boolean,
    serversReady: Boolean,
    onDismiss: () -> Unit
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(850, easing = FastOutSlowInEasing))
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AppColors.BorderAccent, RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            color = AppColors.BgSurfaceElevated,
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(AppColors.BgSurfaceAccent, AppColors.BgSurface)
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(124.dp)) {
                    Canvas(Modifier.size(120.dp)) {
                        val p = progress.value
                        val radius = size.minDimension * (0.24f + 0.22f * p)
                        drawCircle(
                            color = AppColors.SkyBlue.copy(alpha = (1f - p) * 0.62f),
                            radius = radius,
                            style = Stroke(width = 3.dp.toPx())
                        )
                        val rayRadius = size.minDimension * (0.34f + 0.13f * p)
                        repeat(12) { index ->
                            val angle = Math.toRadians(index * 30.0)
                            val cx = size.width / 2f + kotlin.math.cos(angle).toFloat() * rayRadius
                            val cy = size.height / 2f + kotlin.math.sin(angle).toFloat() * rayRadius
                            drawCircle(
                                color = if (index % 2 == 0) AppColors.BrandMintBright else AppColors.SkyBlue,
                                radius = (2.5f + (1f - p) * 2f).dp.toPx(),
                                center = androidx.compose.ui.geometry.Offset(cx, cy),
                                alpha = (1f - p * 0.55f).coerceIn(0f, 1f)
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .scale(0.72f + 0.28f * progress.value)
                            .clip(CircleShape)
                            .background(AppGradients.PrimaryButton),
                        contentAlignment = Alignment.Center
                    ) {
                        CheckmarkIcon(color = AppColors.BrandMintDarkText, size = 36.dp)
                    }
                }
                Text(
                    text = if (isRussian) "Пробный период активирован!" else "Your free trial is active!",
                    color = AppColors.TextPrimary,
                    fontSize = 21.sp,
                    lineHeight = 27.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = if (isRussian) "Вам доступны 3 дня ByMeVPN. Приятного пользования!" else "You have 3 days of ByMeVPN. Enjoy!",
                    color = AppColors.TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center
                )
                if (!serversReady) {
                    Text(
                        text = if (isRussian) "Список серверов обновляется — он появится на главном экране." else "Your server list is refreshing and will appear on Home.",
                        color = AppColors.TextMuted,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.size(2.dp))
                ByMePrimaryButton(
                    text = if (isRussian) "К подключению" else "Go to VPN",
                    onClick = onDismiss,
                    height = 54.dp
                )
            }
        }
    }
}
