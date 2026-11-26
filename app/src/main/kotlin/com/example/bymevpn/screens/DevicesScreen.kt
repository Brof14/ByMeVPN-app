package com.example.bymevpn.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bymevpn.components.BottomNavTab
import com.example.bymevpn.components.ByMeBottomNav
import com.example.bymevpn.components.ByMePrimaryButton
import com.example.bymevpn.components.ByMeTopBar
import com.example.bymevpn.components.SmartphoneIcon
import com.example.bymevpn.data.AccountRepository
import kotlinx.coroutines.launch
import android.widget.Toast
import com.example.bymevpn.BuildConfig
import com.example.bymevpn.data.LocaleManager
import com.example.bymevpn.data.subscription.SubscriptionManager
import com.example.bymevpn.theme.AppColors

internal fun visibleDeviceCount(devices: List<com.example.bymevpn.data.api.DeviceItem>): Int =
    devices.size

@Composable
fun DevicesScreen(
    onBackClick: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAccount: () -> Unit,
    onNavigateToPricing: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val subManager = remember { SubscriptionManager.getInstance(context) }
    val subscription by subManager.subscriptionStatus.collectAsState()
    val devicesList by AccountRepository.userDevices.collectAsState()
    val deviceListUnavailable by AccountRepository.deviceListUnavailable.collectAsState()
    val language by LocaleManager.currentLanguage.collectAsState()
    val isRu = LocaleManager.isRussian(language = language)
    val scope = rememberCoroutineScope()
    var pendingRemoveId by remember { mutableStateOf<String?>(null) }
    var isRemoving by remember { mutableStateOf(false) }
    var isRefreshingDevices by remember { mutableStateOf(false) }
    val otherDevices = devicesList.filterNot { it.isCurrent }

    LaunchedEffect(Unit) {
        AccountRepository.refreshDevices(context)
        subManager.refreshStatus(forceBackend = true)
    }

    val effectiveMax = subscription?.maxDevices?.takeIf { it > 0 }
    val effectiveConnected = visibleDeviceCount(devicesList)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.BgCanvas)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ByMeTopBar(
                title = if (isRu) "Мои устройства" else "My devices",
                onBackClick = onBackClick,
                onMoreClick = null
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 26.dp, vertical = 12.dp)
            ) {
                // Large device count display matching mockup 15_devices
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = if (deviceListUnavailable) "—" else "$effectiveConnected",
                        color = AppColors.TextPrimary,
                        fontSize = 60.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-3).sp
                    )
                    Text(
                        text = effectiveMax?.let { if (isRu) " из $it" else " of $it" } ?: "",
                        color = AppColors.BrandMint,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 10.dp, start = 4.dp)
                    )
                }

                Text(
                    text = if (deviceListUnavailable) {
                        if (isRu) "Не удалось получить подтверждённый список с сервера." else "Could not retrieve the confirmed device list."
                    } else if (effectiveMax == null) {
                        if (devicesList.isEmpty()) {
                            if (isRu) "Зарегистрированных устройств пока нет." else "No registered devices yet."
                        } else {
                            if (isRu) "Сейчас отображается $effectiveConnected устройств." else "$effectiveConnected devices are shown now."
                        }
                    } else if (isRu) "Подключено устройств · максимум $effectiveMax" else "Devices connected · up to $effectiveMax",
                    color = AppColors.TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )

                if (effectiveMax != null && !deviceListUnavailable) {
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { (effectiveConnected.toFloat() / effectiveMax).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = AppColors.SkyBlue,
                        trackColor = AppColors.BorderDefault
                    )
                    Spacer(Modifier.height(20.dp))
                }

                if (deviceListUnavailable) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(17.dp))
                            .background(AppColors.BgSurface)
                            .border(1.dp, AppColors.BorderDefault, RoundedCornerShape(17.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = if (isRu) "Список устройств временно недоступен" else "Device list is temporarily unavailable",
                            color = AppColors.TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (isRu) "Мы не меняли лимит и не удаляли устройства. Повторите загрузку." else "Your device limit and registered devices were not changed. Try loading again.",
                            color = AppColors.TextSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(14.dp))
                        ByMePrimaryButton(
                            text = if (isRefreshingDevices) {
                                if (isRu) "Загружаем…" else "Loading…"
                            } else {
                                if (isRu) "Повторить" else "Retry"
                            },
                            onClick = {
                                if (!isRefreshingDevices) scope.launch {
                                    isRefreshingDevices = true
                                    try {
                                        AccountRepository.refreshDevices(context)
                                    } finally {
                                        isRefreshingDevices = false
                                    }
                                }
                            },
                            enabled = !isRefreshingDevices,
                            height = 46.dp,
                            showArrow = false
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                }

                val localModel = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}".trim()
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                val localOs = "Android ${android.os.Build.VERSION.RELEASE}"

                // This local device can be shown before the server registers it, but
                // it must not be counted or presented as confirmed until it is listed.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(17.dp))
                        .background(AppColors.BgSurface)
                        .border(1.dp, AppColors.BorderAccent, RoundedCornerShape(17.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(37.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(AppColors.BgSurfaceAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        SmartphoneIcon(color = AppColors.SkyBlue, size = 21.dp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = localModel,
                            color = AppColors.TextPrimary,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$localOs · " + (if (isRu) "это устройство" else "this device"),
                            color = AppColors.TextSecondary,
                            fontSize = 11.5.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(AppColors.BrandMintBright)
                            )
                            Text(
                                text = if (devicesList.any { it.isCurrent }) {
                                    if (isRu) "Это устройство" else "This device"
                                } else {
                                    if (isRu) "Ещё не подтверждено сервером" else "Not yet confirmed by the server"
                                },
                                color = AppColors.BrandMint,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                if (otherDevices.isNotEmpty()) {
                    Text(
                        text = if (isRu) "Другие устройства" else "Other devices",
                        color = AppColors.TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 10.dp, start = 2.dp)
                    )
                }

                if (otherDevices.isNotEmpty()) {
                    otherDevices.forEach { dev ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(89.dp)
                                .clip(RoundedCornerShape(17.dp))
                                .background(AppColors.BgSurface)
                                .border(1.dp, AppColors.BorderDefault, RoundedCornerShape(17.dp))
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(37.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(AppColors.BgSurfaceAccent),
                                contentAlignment = Alignment.Center
                            ) {
                                SmartphoneIcon(color = AppColors.SkyBlue, size = 21.dp)
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = dev.deviceModel,
                                    color = AppColors.TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${dev.platform} · ${dev.lastActive}",
                                    color = AppColors.TextSecondary,
                                    fontSize = 11.5.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { pendingRemoveId = dev.id }
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isRu) "Удалить" else "Remove",
                                    color = AppColors.StatusError,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                } else {
                    Text(
                        text = if (isRu) "Пока отображается только это устройство." else "Only this device is shown for now.",
                        color = AppColors.TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (BuildConfig.EXTERNAL_CHECKOUT_ENABLED && effectiveMax != null && effectiveMax in setOf(2, 5) && subscription?.isUsable == true) {
                    val isFull = effectiveConnected >= effectiveMax
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(17.dp))
                            .background(AppColors.BgSurface)
                            .border(1.dp, AppColors.BorderAccent, RoundedCornerShape(17.dp))
                            .padding(18.dp)
                    ) {
                        Text(
                            text = if (isFull) {
                                if (isRu) "Лимит устройств достигнут" else "Device limit reached"
                            } else {
                                if (isRu) "Нужно больше устройств?" else "Need more devices?"
                            },
                            color = AppColors.BrandMint,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (isRu) {
                                "Доплата за 5 или 10 устройств рассчитывается за оставшийся срок вашей подписки. Выберите лимит и проверьте сумму в Telegram-боте перед оплатой."
                            } else {
                                "Upgrades to 5 or 10 devices are prorated over your remaining subscription period. Choose a limit and review the final price in the Telegram bot."
                            },
                            color = AppColors.TextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 19.sp
                        )
                        Spacer(Modifier.height(14.dp))
                        ByMePrimaryButton(
                            text = if (isRu) "Увеличить лимит в Telegram" else "Upgrade devices in Telegram",
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/ByMeVPNbot"))
                                runCatching { context.startActivity(intent) }.onFailure {
                                    Toast.makeText(
                                        context,
                                        if (isRu) "Не удалось открыть Telegram. Найдите @ByMeVPNbot."
                                        else "Could not open Telegram. Search for @ByMeVPNbot.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (isRu) "В боте откройте подписку → Мои устройства → Увеличить лимит."
                                else "In the bot: Subscription → My devices → Upgrade device limit.",
                            color = AppColors.TextMuted,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                }

                // Info Box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(17.dp))
                        .background(AppColors.BgSurface)
                        .border(1.dp, AppColors.BorderDefault, RoundedCornerShape(17.dp))
                        .padding(18.dp)
                ) {
                    Text(
                        text = if (isRu) "Добавить устройство" else "Add a device",
                        color = AppColors.SkyBlue,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isRu) "Войдите в ByMeVPN на другом устройстве с тем же аккаунтом. Оно появится здесь после регистрации на сервере."
                            else "Sign in to ByMeVPN on another device with the same account. It will appear here once registered with the server.",
                        color = AppColors.TextSecondary,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            ByMeBottomNav(
                currentTab = BottomNavTab.Account,
                onTabSelected = { tab ->
                    when (tab) {
                        BottomNavTab.Home -> onNavigateToHome()
                        BottomNavTab.Settings -> onNavigateToSettings()
                        BottomNavTab.Account -> onNavigateToAccount()
                    }
                }
            )
        }
        val targetId = pendingRemoveId
        if (targetId != null) {
            AlertDialog(
                onDismissRequest = { if (!isRemoving) pendingRemoveId = null },
                containerColor = AppColors.BgSurfaceElevated,
                titleContentColor = AppColors.TextPrimary,
                textContentColor = AppColors.TextSecondary,
                title = { Text(if (isRu) "Удалить устройство?" else "Remove device?") },
                text = { Text(if (isRu) "Устройство потеряет доступ к VPN. Продолжить?" else "This device will lose VPN access. Continue?") },
                confirmButton = {
                    TextButton(enabled = !isRemoving, onClick = {
                        isRemoving = true
                        scope.launch {
                            val result = AccountRepository.removeDevice(context, targetId)
                            isRemoving = false
                            if (result.isSuccess) pendingRemoveId = null
                            else Toast.makeText(
                                context,
                                com.example.bymevpn.data.api.AccountActionErrorMessages.deleteDevice(
                                    result.exceptionOrNull(), isRu
                                ),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }) { Text(if (isRu) "Удалить" else "Remove", color = AppColors.StatusError) }
                },
                dismissButton = {
                    TextButton(enabled = !isRemoving, onClick = { pendingRemoveId = null }) {
                        Text(if (isRu) "Отмена" else "Cancel", color = AppColors.TextSecondary)
                    }
                }
            )
        }
    }
}
