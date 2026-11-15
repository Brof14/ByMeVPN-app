package com.example.bymevpn.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bymevpn.components.BottomNavTab
import com.example.bymevpn.components.ByMeBottomNav
import com.example.bymevpn.components.ByMeCardGroup
import com.example.bymevpn.components.ByMeInfoBox
import com.example.bymevpn.components.ByMePrimaryButton
import com.example.bymevpn.components.ByMeSecondaryButton
import com.example.bymevpn.components.ByMeTopBar
import com.example.bymevpn.data.LocaleManager
import com.example.bymevpn.data.storage.SecureTokenStorage
import com.example.bymevpn.data.subscription.SubscriptionManager
import com.example.bymevpn.theme.AppColors
import kotlinx.coroutines.launch

/** This screen deliberately never invents a payment method, charge date or cancellation state. */
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
    val t: (String, String) -> String = { ru, en -> if (isRu) ru else en }
    val manager = remember { SubscriptionManager.getInstance(context) }
    val isExternalProfile = remember { SecureTokenStorage.getInstance(context).isExternalProfileMode() }
    val subscription by manager.subscriptionStatus.collectAsState()
    val scope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }
    var lastRefreshFailed by remember { mutableStateOf(false) }

    suspend fun refresh() {
        refreshing = true
        try {
            lastRefreshFailed = manager.refreshStatus(forceBackend = true).isFailure
        } finally {
            refreshing = false
        }
    }

    LaunchedEffect(Unit) { refresh() }

    val status = when {
        isExternalProfile -> t("Управляется поставщиком подписки", "Managed by the subscription provider")
        subscription?.autoRenew == true -> t("Включено", "Enabled")
        subscription?.autoRenew == false -> t("Выключено", "Disabled")
        else -> t("Статус неизвестен", "Status unavailable")
    }

    Column(modifier.fillMaxSize().background(AppColors.BgCanvas)) {
        ByMeTopBar(
            title = t("Автопродление", "Auto-renewal"),
            onBackClick = onBackClick,
            onMoreClick = null
        )
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                t("Управление подпиской", "Manage your subscription"),
                color = AppColors.TextPrimary,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(14.dp))
            ByMeCardGroup {
                Column(Modifier.padding(18.dp)) {
                    Text(t("ТЕКУЩИЙ СТАТУС", "CURRENT STATUS"),
                        fontSize = 12.sp, color = AppColors.TextSecondary,
                        fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text(status, fontSize = 20.sp, color = AppColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        when {
                            isExternalProfile -> t(
                                "ByMeVPN не управляет оплатой сторонней подписки.",
                                "ByMeVPN does not manage third-party subscription payments."
                            )
                            lastRefreshFailed -> t(
                                "Не удалось получить свежие данные. Показан последний известный статус; проверьте его повторно.",
                                "Fresh data is unavailable. Showing the last known status; please retry."
                            )
                            subscription?.autoRenew == null -> t(
                                "Сервер пока не предоставил подтверждённый статус автопродления.",
                                "The server has not provided a verified renewal status."
                            )
                            else -> t("Статус получен от сервера подписок.", "Status reported by the subscription server.")
                        },
                        fontSize = 13.sp, lineHeight = 19.sp,
                        color = AppColors.TextSecondary
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            if (!isExternalProfile) {
                ByMeInfoBox(
                    t(
                        "Данные карты, сумма и дата следующего списания пока не доступны через API приложения. Мы не показываем расчётные значения как подтверждённые.",
                        "Card details, next charge amount and date are not available through the app API. Estimated values are never presented as verified."
                    )
                )
                Spacer(Modifier.height(16.dp))
                ByMePrimaryButton(
                    text = t("Управлять оплатой в Telegram", "Manage billing in Telegram"),
                    onClick = {
                        // The bot provides the existing saved-payment-method and
                        // auto-renewal controls. App API has no mutation contract yet.
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/ByMeVPNbot"))
                            )
                        }.onFailure {
                            Toast.makeText(
                                context, t("Не удалось открыть Telegram", "Could not open Telegram"),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    t(
                        "Управление доступно для подписок, привязанных к вашему Telegram-аккаунту.",
                        "Available for subscriptions linked to your Telegram account."
                    ),
                    color = AppColors.TextSecondary, fontSize = 12.sp, lineHeight = 18.sp
                )
                Spacer(Modifier.height(16.dp))
                ByMeSecondaryButton(
                    text = if (refreshing) t("Проверяем…", "Checking…")
                        else t("Обновить статус", "Refresh status"),
                    onClick = { if (!refreshing) scope.launch { refresh() } },
                    enabled = !refreshing
                )
            }
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
