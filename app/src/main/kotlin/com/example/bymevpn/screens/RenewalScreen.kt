package com.example.bymevpn.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bymevpn.components.BottomNavTab
import com.example.bymevpn.components.ByMeBottomNav
import com.example.bymevpn.components.ByMePrimaryButton
import com.example.bymevpn.components.ByMeSecondaryButton
import com.example.bymevpn.components.ByMeTopBar
import com.example.bymevpn.data.LocaleManager
import com.example.bymevpn.data.subscription.SubscriptionManager
import com.example.bymevpn.theme.AppColors
import kotlinx.coroutines.launch

@Composable
fun RenewalScreen(
    onBackClick: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val language by LocaleManager.currentLanguage.collectAsState()
    val isRu = LocaleManager.isRussian(language = language)
    val manager = remember { SubscriptionManager.getInstance(context) }
    val subscription by manager.subscriptionStatus.collectAsState()
    val scope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }
    val status = when (subscription?.autoRenew) {
        true -> if (isRu) "Включено по данным сервера" else "Enabled, server-reported"
        false -> if (isRu) "Отключено по данным сервера" else "Disabled, server-reported"
        null -> if (isRu) "Не удалось подтвердить" else "Not confirmed"
    }

    Column(modifier.fillMaxSize().background(AppColors.BgCanvas)) {
        ByMeTopBar(title = if (isRu) "Автопродление" else "Auto-renewal", onBackClick = onBackClick, onMoreClick = null)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(26.dp)) {
            Text(
                if (isRu) "Статус: $status" else "Status: $status",
                color = AppColors.TextPrimary,
                fontSize = 19.sp
            )
            Spacer(Modifier.height(16.dp))
            Text(
                if (isRu) {
                    "Новые оплаты в Android не сохраняют карту для автосписаний. Если автопродление подключено через Telegram-бота, управлять им нужно в боте."
                } else {
                    "Android checkout does not save a payment method for recurring charges. Manage any existing Telegram-bot auto-renewal in the bot."
                },
                color = AppColors.TextSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(10.dp))
            Text(
                if (isRu) "Дата следующего списания, сумма и привязанная карта не предоставляются текущим API приложения. Мы не показываем неподтверждённые данные."
                else "The current app API does not supply the next charge date, amount, or linked card. Unverified billing details are not displayed.",
                color = AppColors.TextMuted,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
            Spacer(Modifier.height(20.dp))
            ByMePrimaryButton(
                text = if (isRu) "Управлять в Telegram" else "Manage in Telegram",
                onClick = {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/ByMeVPNbot")))
                    }.onFailure {
                        Toast.makeText(
                            context,
                            if (isRu) "Найдите @ByMeVPNbot в Telegram." else "Find @ByMeVPNbot in Telegram.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            )
            Spacer(Modifier.height(10.dp))
            Text(
                if (isRu) "В боте: Мои подписки → Оплата и автопродление."
                else "In the bot: My subscriptions → Payments and auto-renewal.",
                color = AppColors.TextSecondary,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(20.dp))
            ByMeSecondaryButton(
                text = if (refreshing) {
                    if (isRu) "Обновляем…" else "Refreshing…"
                } else {
                    if (isRu) "Обновить статус" else "Refresh status"
                },
                onClick = {
                    if (!refreshing) scope.launch {
                        refreshing = true
                        try {
                            val result = manager.refreshStatus(forceBackend = true)
                            if (result.isFailure) Toast.makeText(
                                context,
                                if (isRu) "Не удалось обновить статус. Проверьте сеть."
                                else "Could not refresh status. Check your connection.",
                                Toast.LENGTH_SHORT
                            ).show()
                        } finally {
                            refreshing = false
                        }
                    }
                }
            )
        }
        ByMeBottomNav(currentTab = BottomNavTab.Account, onTabSelected = { tab ->
            when (tab) {
                BottomNavTab.Home -> onNavigateToHome()
                BottomNavTab.Settings -> onNavigateToSettings()
                BottomNavTab.Account -> onNavigateToAccount()
            }
        })
    }
}
