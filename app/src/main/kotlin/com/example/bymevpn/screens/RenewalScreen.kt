package com.example.bymevpn.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bymevpn.BuildConfig
import com.example.bymevpn.components.BottomNavTab
import com.example.bymevpn.components.ByMeBottomNav
import com.example.bymevpn.components.ByMePrimaryButton
import com.example.bymevpn.components.ByMeSecondaryButton
import com.example.bymevpn.components.ByMeTopBar
import com.example.bymevpn.data.LocaleManager
import com.example.bymevpn.data.api.ByMeApiClient
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
    val api = remember { ByMeApiClient(context) }
    val scope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(true) }
    var enabled by remember { mutableStateOf<Boolean?>(null) }
    var canEnable by remember { mutableStateOf(false) }
    var showConfirmation by remember { mutableStateOf(false) }
    var showEnableConfirmation by remember { mutableStateOf(false) }
    var cancelling by remember { mutableStateOf(false) }
    var enabling by remember { mutableStateOf(false) }

    suspend fun refreshRenewal() {
        refreshing = true
        try {
            val renewal = api.getAutoRenewal()
            enabled = renewal.enabled
            canEnable = renewal.canEnable
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            enabled = null
            canEnable = false
        } finally {
            refreshing = false
        }
    }
    LaunchedEffect(Unit) { refreshRenewal() }

    if (showEnableConfirmation) {
        AlertDialog(
            onDismissRequest = { if (!enabling) showEnableConfirmation = false },
            containerColor = AppColors.BgSurfaceElevated,
            titleContentColor = AppColors.TextPrimary,
            textContentColor = AppColors.TextSecondary,
            title = { Text(if (isRu) "Включить автопродление?" else "Enable auto-renewal?") },
            text = {
                Text(
                    if (isRu) "Будут использованы только ранее сохранённый способ оплаты и данное вами согласие. Списание произойдёт при продлении подписки. Отключить можно в любой момент."
                    else "Only your previously saved payment method and consent will be used. The charge is scheduled for subscription renewal. You can turn it off at any time."
                )
            },
            confirmButton = {
                TextButton(enabled = !enabling, onClick = {
                    enabling = true
                    scope.launch {
                        try {
                            api.enableAutoRenewal()
                            enabled = true
                            canEnable = false
                            showEnableConfirmation = false
                        } catch (e: kotlinx.coroutines.CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            showEnableConfirmation = false
                            Toast.makeText(context, if (isRu) "Не удалось подтвердить включение. Обновите статус." else "Activation could not be confirmed. Refresh the status.", Toast.LENGTH_LONG).show()
                            refreshRenewal()
                        } finally {
                            enabling = false
                        }
                    }
                }) { Text(if (enabling) "…" else if (isRu) "Включить" else "Enable", color = AppColors.BrandMint) }
            },
            dismissButton = {
                TextButton(enabled = !enabling, onClick = { showEnableConfirmation = false }) {
                    Text(if (isRu) "Отмена" else "Cancel", color = AppColors.TextSecondary)
                }
            }
        )
    }
    val status = when {
        refreshing -> if (isRu) "Проверяем…" else "Checking…"
        enabled == true -> if (isRu) "Включено" else "On"
        enabled == false -> if (isRu) "Отключено" else "Off"
        else -> if (isRu) "Не удалось проверить" else "Could not check"
    }

    if (showConfirmation) {
        AlertDialog(
            onDismissRequest = { if (!cancelling) showConfirmation = false },
            containerColor = AppColors.BgSurfaceElevated,
            titleContentColor = AppColors.TextPrimary,
            textContentColor = AppColors.TextSecondary,
            title = { Text(if (isRu) "Отключить автопродление?" else "Turn off auto-renewal?") },
            text = { Text(if (isRu) "Следующего автоматического списания не будет. Оплаченный доступ сохранится до конца срока." else "There will be no next automatic charge. Your paid access remains until expiry.") },
            confirmButton = {
                TextButton(enabled = !cancelling, onClick = {
                    cancelling = true
                    scope.launch {
                        try {
                            api.cancelAutoRenewal()
                            enabled = false
                            showConfirmation = false
                        } catch (e: kotlinx.coroutines.CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            enabled = null
                            showConfirmation = false
                            Toast.makeText(context, if (isRu) "Не удалось подтвердить отключение. Обновите статус." else "Cancellation could not be confirmed. Refresh the status.", Toast.LENGTH_LONG).show()
                        } finally {
                            cancelling = false
                        }
                    }
                }) { Text(if (isRu) "Отключить" else "Turn off", color = AppColors.StatusError) }
            },
            dismissButton = {
                TextButton(enabled = !cancelling, onClick = { showConfirmation = false }) {
                    Text(if (isRu) "Отмена" else "Cancel", color = AppColors.TextSecondary)
                }
            }
        )
    }

    Column(modifier.fillMaxSize().background(AppColors.BgCanvas)) {
        ByMeTopBar(title = if (isRu) "Автопродление" else "Auto-renewal", onBackClick = onBackClick, onMoreClick = null)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(AppColors.BgSurface)
                    .border(1.dp, AppColors.BorderDefault, RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Text(
                    text = if (isRu) "Статус: $status" else "Status: $status",
                    color = if (enabled == true) AppColors.BrandMint else AppColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = if (BuildConfig.EXTERNAL_CHECKOUT_ENABLED) {
                        if (isRu) {
                            "Отключение автопродления не отменяет уже оплаченный доступ. Подключить его можно через Telegram-бота."
                        } else {
                            "Turning off auto-renewal keeps your current paid access. To enable it, use the Telegram bot."
                        }
                    } else {
                        if (isRu) {
                            "Отключение автопродления не отменяет уже оплаченный доступ."
                        } else {
                            "Turning off auto-renewal keeps your current paid access."
                        }
                    },
                    color = AppColors.TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (isRu) "Детали карты и дата списания не передаются в приложение."
                    else "Card details and the next billing date are not stored in the app.",
                    color = AppColors.TextMuted,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }

            Spacer(Modifier.height(20.dp))
            if (enabled == true) {
                ByMePrimaryButton(
                    text = if (isRu) "Отключить автопродление" else "Turn off auto-renewal",
                    onClick = { showConfirmation = true },
                    enabled = !refreshing && !cancelling
                )
                Spacer(Modifier.height(12.dp))
            } else if (enabled == false && canEnable) {
                ByMePrimaryButton(
                    text = if (isRu) "Включить автопродление" else "Enable auto-renewal",
                    onClick = { showEnableConfirmation = true },
                    enabled = !refreshing && !cancelling && !enabling
                )
                Spacer(Modifier.height(12.dp))
            }
            if (BuildConfig.EXTERNAL_CHECKOUT_ENABLED && enabled == false && !canEnable) {
                ByMeSecondaryButton(
                    text = if (isRu) "Настроить способ оплаты в Telegram" else "Set up payment method in Telegram",
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
                Spacer(Modifier.height(8.dp))
                Text(
                    if (isRu) "Требуется сохранённый способ оплаты."
                    else "Requires a saved payment method.",
                    color = AppColors.TextMuted,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(12.dp))
            }
            ByMeSecondaryButton(
                text = if (refreshing) {
                    if (isRu) "Обновляем…" else "Refreshing…"
                } else {
                    if (isRu) "Обновить статус" else "Refresh status"
                },
                onClick = {
                    if (!refreshing) scope.launch { refreshRenewal() }
                },
                enabled = !refreshing && !cancelling
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
