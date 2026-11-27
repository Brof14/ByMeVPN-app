package com.example.bymevpn.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.bymevpn.theme.AppColors
import com.example.bymevpn.theme.AppGradients

@Composable
fun TelegramSignInDialog(
    isRu: Boolean,
    statusText: String?,
    waitSeconds: Int,
    onOpenTelegramAgain: () -> Unit,
    onCancel: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(AppColors.BgSurfaceElevated)
                .border(1.dp, AppColors.BorderAccent, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Brush.verticalGradient(listOf(AppColors.BrandBlue, Color(0xFF0055CC)))),
                    contentAlignment = Alignment.Center
                ) {
                    PaperPlaneIcon(color = Color.White, size = 20.dp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isRu) "Вход через Telegram" else "Telegram sign-in",
                        color = AppColors.TextPrimary,
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isRu) "Подтвердите вход в боте" else "Confirm sign-in in the bot",
                        color = AppColors.TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TelegramStep(1, if (isRu) "Откройте @ByMeVPNBot в Telegram" else "Open @ByMeVPNBot in Telegram")
            TelegramStep(2, if (isRu) "Нажмите «Старт» в чате с ботом" else "Tap Start in the bot chat")
            TelegramStep(3, if (isRu) "Вернитесь в приложение — вход подтвердится сам" else "Return to the app — sign-in completes automatically")

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.BgSurfaceAccent)
                    .border(1.dp, AppColors.BorderAccent, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularProgressIndicator(
                    color = AppColors.SkyBlue,
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = statusText ?: (if (isRu) "Ожидаем подтверждение…" else "Waiting for confirmation…"),
                        color = AppColors.TextPrimary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (isRu) "Проверяем автоматически · ${waitSeconds} с" else "Checking automatically · ${waitSeconds}s",
                        color = AppColors.TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppGradients.PrimaryButton)
                    .clickable(onClick = onOpenTelegramAgain),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isRu) "Открыть Telegram снова" else "Open Telegram again",
                    color = AppColors.BrandMintDarkText,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onCancel),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isRu) "Отмена" else "Cancel",
                    color = AppColors.TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun TelegramStep(number: Int, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(AppColors.BgSurfaceAccent),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$number",
                color = AppColors.SkyBlue,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            color = AppColors.TextSecondary,
            fontSize = 12.5.sp,
            lineHeight = 17.sp
        )
    }
}
