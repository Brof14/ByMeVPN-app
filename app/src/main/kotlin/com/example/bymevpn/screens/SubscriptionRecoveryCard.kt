package com.example.bymevpn.screens

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bymevpn.components.ByMePrimaryButton
import com.example.bymevpn.data.api.SubscriptionStatus
import com.example.bymevpn.data.subscription.SubscriptionRefreshError
import com.example.bymevpn.theme.AppColors

@Composable
internal fun SubscriptionRecoveryCard(
    isRu: Boolean,
    isChecking: Boolean,
    accessConfirmationRequired: Boolean,
    status: SubscriptionStatus?,
    lastError: SubscriptionRefreshError?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reason = status?.trialUnavailableReason
    val title = when {
        isChecking -> if (isRu) "Обновляем подписку" else "Updating subscription"
        lastError != null -> if (isRu) "Не удалось обновить подписку" else "Could not refresh subscription"
        accessConfirmationRequired -> if (isRu) "Подтвердите устройство" else "Confirm this device"
        status?.isUndetermined == true && reason == "subscription_verification_required" ->
            if (isRu) "Не подтверждён срок подписки" else "Subscription expiry is not confirmed"
        else -> if (isRu) "Не удалось обновить подписку" else "Could not refresh subscription"
    }
    val description = when {
        isChecking -> if (isRu) {
            "Запрашиваем актуальные данные аккаунта."
        } else {
            "Retrieving the latest account information."
        }
        lastError != null -> lastError.userMessage(isRu)
        accessConfirmationRequired -> if (isRu) {
            "Подписка активна. Серверу нужно подтвердить это устройство перед подключением."
        } else {
            "Your subscription is active. The server needs to confirm this device before connecting."
        }
        status?.isUndetermined == true && reason == "subscription_verification_required" -> if (isRu) {
            "Ссылка подписки найдена, но панель не вернула подтверждённую дату окончания. Доступ не считается отменённым — повторите запрос."
        } else {
            "A subscription link was found, but the panel did not return a confirmed expiry date. Access has not been marked as cancelled. Try again."
        }
        status?.isUndetermined == true && reason == "subscription_check_unavailable" -> if (isRu) {
            "Сервер не смог получить данные подписки. Срок оплаченного доступа не меняется."
        } else {
            "The server could not retrieve subscription details. Your paid access period is unchanged."
        }
        else -> if (isRu) {
            "Сервер не вернул подтверждённый статус. Данные аккаунта и оплаченный срок не изменены."
        } else {
            "The server did not return a confirmed status. Your account data and paid access period are unchanged."
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(AppColors.BgSurface)
            .border(1.dp, AppColors.BorderDefault, RoundedCornerShape(22.dp))
            .padding(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isChecking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(19.dp),
                    color = AppColors.SkyBlue,
                    strokeWidth = 2.dp
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(if (accessConfirmationRequired) AppColors.StatusWarning else AppColors.SkyBlue)
                )
            }
            Text(
                text = title,
                color = AppColors.TextPrimary,
                fontSize = 19.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Text(
            text = description,
            color = AppColors.TextSecondary,
            fontSize = 14.sp,
            lineHeight = 21.sp
        )

        if (!lastError?.correlationId.isNullOrBlank()) {
            Text(
                text = (if (isRu) "Код обращения: " else "Reference: ") + lastError?.correlationId.orEmpty(),
                color = AppColors.TextMuted,
                fontSize = 11.sp
            )
        }

        Spacer(Modifier.height(2.dp))

        ByMePrimaryButton(
            text = when {
                isChecking -> if (isRu) "Проверяем…" else "Checking…"
                else -> if (isRu) "Повторить проверку" else "Try again"
            },
            onClick = onRetry,
            enabled = !isChecking,
            height = 54.dp,
            showArrow = false
        )
    }
}
