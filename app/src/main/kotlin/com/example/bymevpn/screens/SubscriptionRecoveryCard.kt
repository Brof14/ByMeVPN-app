package com.example.bymevpn.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bymevpn.components.ByMePrimaryButton
import com.example.bymevpn.components.ShieldLogo
import com.example.bymevpn.theme.AppColors
import com.example.bymevpn.theme.AppGradients

@Composable
internal fun SubscriptionRecoveryCard(
    isRu: Boolean,
    isChecking: Boolean,
    accessConfirmationRequired: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(AppGradients.AccentCard)
            .border(1.dp, AppColors.BorderAccent, RoundedCornerShape(24.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(AppColors.BgSurfaceAccent)
                    .border(1.dp, AppColors.BorderAccent, RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (isChecking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = AppColors.SkyBlue,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    ShieldLogo(size = 56.dp, isActive = false)
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isRu) "СТАТУС АККАУНТА" else "ACCOUNT STATUS",
                    color = AppColors.SkyBlue,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = when {
                        isChecking -> if (isRu) "Проверяем доступ" else "Checking access"
                        accessConfirmationRequired -> if (isRu) "Нужно подтвердить устройство" else "Device confirmation needed"
                        else -> if (isRu) "Статус пока недоступен" else "Status is unavailable"
                    },
                    color = AppColors.TextPrimary,
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        Text(
            text = when {
                isChecking -> if (isRu) {
                    "Получаем актуальные данные аккаунта и проверяем доступ к серверам."
                } else {
                    "Retrieving your latest account details and checking server access."
                }
                accessConfirmationRequired -> if (isRu) {
                    "Подписка найдена, но сервер ещё не подтвердил это устройство. Обновите статус, чтобы завершить проверку."
                } else {
                    "Your subscription was found, but the server has not confirmed this device yet. Refresh the status to complete the check."
                }
                else -> if (isRu) {
                    "Не удалось получить ответ сервера. Это не означает, что подписка отменена или срок доступа изменился. Проверьте соединение и повторите запрос."
                } else {
                    "The server did not return a status. This does not mean your subscription was cancelled or its validity changed. Check your connection and try again."
                }
            },
            color = AppColors.TextSecondary,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(15.dp))
                .background(AppColors.BgSurface.copy(alpha = 0.82f))
                .border(1.dp, AppColors.BorderDefault, RoundedCornerShape(15.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = if (isRu) "ПОСЛЕДНЯЯ ПРОВЕРКА" else "LATEST CHECK",
                    color = AppColors.TextMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.9.sp
                )
                Text(
                    text = when {
                        isChecking -> if (isRu) "Запрос выполняется" else "Request in progress"
                        accessConfirmationRequired -> if (isRu) "Ожидается подтверждение" else "Awaiting confirmation"
                        else -> if (isRu) "Нет подтверждённого ответа" else "No confirmed response"
                    },
                    color = AppColors.TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(if (isChecking) AppColors.SkyBlue else AppColors.StatusWarning)
            )
        }

        Spacer(Modifier.height(18.dp))

        ByMePrimaryButton(
            text = when {
                isChecking -> if (isRu) "Проверяем…" else "Checking…"
                else -> if (isRu) "Повторить проверку" else "Try again"
            },
            onClick = onRetry,
            enabled = !isChecking,
            height = 54.dp,
            showArrow = !isChecking
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = if (isRu) {
                "Повторная проверка не запускает оплату и не меняет тариф."
            } else {
                "Checking again does not charge you or change your plan."
            },
            modifier = Modifier.fillMaxWidth(),
            color = AppColors.TextMuted,
            fontSize = 10.5.sp,
            lineHeight = 15.sp,
            textAlign = TextAlign.Center
        )
    }
}
