package com.example.bymevpn.screens

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.bymevpn.components.BottomNavTab
import com.example.bymevpn.components.ByMeBottomNav
import com.example.bymevpn.components.ByMeCardGroup
import com.example.bymevpn.components.ByMeSettingRow
import com.example.bymevpn.components.ByMeToggle
import com.example.bymevpn.components.ByMeTopBar
import com.example.bymevpn.components.ChevronRightIcon
import com.example.bymevpn.components.GlobeIcon
import com.example.bymevpn.components.LockIcon
import com.example.bymevpn.components.PowerIcon
import com.example.bymevpn.components.ShieldSecurityIcon
import com.example.bymevpn.components.SmartphoneIcon
import com.example.bymevpn.components.rememberByMeHaptics
import com.example.bymevpn.data.LocaleManager
import com.example.bymevpn.data.settings.AppSettings
import com.example.bymevpn.data.settings.SettingsRepository
import com.example.bymevpn.theme.AppColors
import kotlinx.coroutines.launch

private data class DnsOption(
    val id: String,
    val nameRu: String,
    val nameEn: String,
    val descRu: String,
    val descEn: String
)

private val DNS_OPTIONS = listOf(
    DnsOption(
        id = "cloudflare",
        nameRu = "Cloudflare (1.1.1.1)",
        nameEn = "Cloudflare (1.1.1.1)",
        descRu = "Быстрый, приватный, по умолчанию",
        descEn = "Fast, privacy-focused, default"
    ),
    DnsOption(
        id = "adguard",
        nameRu = "AdGuard DNS",
        nameEn = "AdGuard DNS",
        descRu = "Блокирует рекламу и фишинговые сайты",
        descEn = "Blocks ads and malicious trackers"
    ),
    DnsOption(
        id = "google",
        nameRu = "Google (8.8.8.8)",
        nameEn = "Google (8.8.8.8)",
        descRu = "Высокая глобальная стабильность",
        descEn = "High global availability"
    )
)

@Composable
fun AdvancedSettingsScreen(
    onBackClick: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val settingsRepo = remember { SettingsRepository.getInstance(context) }
    val settings by settingsRepo.settingsFlow.collectAsState(initial = AppSettings())
    val language by LocaleManager.currentLanguage.collectAsState()
    val isRu = LocaleManager.isRussian(language = language)
    val scope = rememberCoroutineScope()
    val haptics = rememberByMeHaptics()

    var showDnsDialog by remember { mutableStateOf(false) }

    if (showDnsDialog) {
        Dialog(
            onDismissRequest = { showDnsDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(AppColors.BgSurfaceElevated)
                    .border(1.dp, AppColors.BorderDefault, RoundedCornerShape(24.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isRu) "DNS-сервер" else "DNS Provider",
                    color = AppColors.TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isRu) "Выберите DNS для разрешения доменных имён внутри туннеля."
                    else "Choose which DNS resolver to use inside the VPN tunnel.",
                    color = AppColors.TextSecondary,
                    fontSize = 13.sp
                )

                DNS_OPTIONS.forEach { option ->
                    val selected = settings.dnsMode == option.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(15.dp))
                            .background(if (selected) AppColors.BgSurfaceAccent else AppColors.BgInput)
                            .border(
                                1.dp,
                                if (selected) AppColors.BrandMint else AppColors.BorderDefault,
                                RoundedCornerShape(15.dp)
                            )
                            .clickable {
                                haptics.selection()
                                scope.launch {
                                    settingsRepo.updateDnsMode(option.id)
                                    showDnsDialog = false
                                }
                                Toast.makeText(
                                    context,
                                    if (isRu) "DNS применится при следующем подключении" else "DNS applied on next connection",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(if (selected) AppColors.BrandMint else Color.Transparent)
                                .border(1.5.dp, if (selected) AppColors.BrandMint else AppColors.TextMuted, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Text("✓", color = AppColors.BrandMintDarkText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isRu) option.nameRu else option.nameEn,
                                color = AppColors.TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isRu) option.descRu else option.descEn,
                                color = AppColors.TextSecondary,
                                fontSize = 11.5.sp
                            )
                        }
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.BgCanvas)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ByMeTopBar(
                title = if (isRu) "Расширенные настройки" else "Advanced settings",
                onBackClick = onBackClick,
                onMoreClick = null
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 26.dp)
            ) {
                Text(
                    text = if (isRu) "Тонкая настройка сетевого туннеля, протоколов и безопасности."
                    else "Fine-tune network tunneling, protocols, and security.",
                    color = AppColors.TextSecondary,
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )

                // Section: Сеть и DNS
                Text(
                    text = if (isRu) "Сеть и DNS" else "Network & DNS",
                    color = AppColors.TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 10.dp, start = 2.dp)
                )

                ByMeCardGroup {
                    val currentDns = DNS_OPTIONS.find { it.id == settings.dnsMode }
                    ByMeSettingRow(
                        title = if (isRu) "DNS-сервер" else "DNS Provider",
                        subtitle = if (isRu) currentDns?.nameRu ?: settings.dnsMode else currentDns?.nameEn ?: settings.dnsMode,
                        icon = { GlobeIcon(color = Color(0xFF7AC9CB), size = 20.dp) },
                        trailing = { ChevronRightIcon(color = AppColors.TextMuted, size = 18.dp) },
                        onClick = {
                            haptics.tap()
                            showDnsDialog = true
                        },
                        showBottomDivider = true
                    )
                    ByMeSettingRow(
                        title = if (isRu) "Автопереподключение" else "Auto-reconnect",
                        subtitle = if (isRu) "Восстанавливать туннель при смене Wi-Fi или сотовой сети"
                        else "Restore tunnel when switching Wi-Fi or cellular",
                        icon = { PowerIcon(color = Color(0xFF7AC9CB), size = 20.dp) },
                        trailing = {
                            ByMeToggle(
                                checked = settings.autoReconnect,
                                onCheckedChange = { checked ->
                                    haptics.selection()
                                    scope.launch { settingsRepo.updateAutoReconnect(checked) }
                                }
                            )
                        },
                        showBottomDivider = false
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Section: Уведомления и интерфейс
                Text(
                    text = if (isRu) "Уведомления и скорость" else "Notifications & Speed",
                    color = AppColors.TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 10.dp, start = 2.dp)
                )

                ByMeCardGroup {
                    ByMeSettingRow(
                        title = if (isRu) "Скорость в строке состояния" else "Speed in notification",
                        subtitle = if (isRu) "Отображать текущую скорость в шторке уведомлений"
                        else "Show live upload/download speed in notification shade",
                        icon = { SmartphoneIcon(color = Color(0xFF7AC9CB), size = 20.dp) },
                        trailing = {
                            ByMeToggle(
                                checked = settings.showSpeedInNotification,
                                onCheckedChange = { checked ->
                                    haptics.selection()
                                    scope.launch { settingsRepo.updateShowSpeed(checked) }
                                }
                            )
                        },
                        showBottomDivider = true
                    )
                    ByMeSettingRow(
                        title = if (isRu) "Уведомления о статусе" else "Status notifications",
                        subtitle = if (isRu) "Оповещать при успешном подключении и обрывах"
                        else "Notify on connection success or network loss",
                        icon = { ShieldSecurityIcon(color = Color(0xFF7AC9CB), size = 20.dp) },
                        trailing = {
                            ByMeToggle(
                                checked = settings.connectionNotifications,
                                onCheckedChange = { checked ->
                                    haptics.selection()
                                    scope.launch { settingsRepo.updateNotifications(checked) }
                                }
                            )
                        },
                        showBottomDivider = false
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Section: Системная защита
                Text(
                    text = if (isRu) "Защита системы" else "System protection",
                    color = AppColors.TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 10.dp, start = 2.dp)
                )

                ByMeCardGroup {
                    ByMeSettingRow(
                        title = if (isRu) "Постоянный VPN и Kill Switch" else "Always-on VPN & Kill Switch",
                        subtitle = if (isRu) "Настраивается в параметрах Android: «Постоянный VPN» и «Блокировать соединения без VPN»"
                        else "Configured in Android settings: Always-on VPN and Block connections without VPN",
                        icon = { LockIcon(color = Color(0xFF7AC9CB), size = 20.dp) },
                        trailing = { ChevronRightIcon(color = AppColors.TextMuted, size = 18.dp) },
                        onClick = {
                            haptics.tap()
                            runCatching {
                                context.startActivity(Intent(Settings.ACTION_VPN_SETTINGS))
                            }.onFailure {
                                Toast.makeText(
                                    context,
                                    if (isRu) "Не удалось открыть настройки VPN" else "Could not open VPN settings",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        showBottomDivider = false
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))
            }

            ByMeBottomNav(
                currentTab = BottomNavTab.Settings,
                onTabSelected = { tab ->
                    when (tab) {
                        BottomNavTab.Home -> onNavigateToHome()
                        BottomNavTab.Settings -> onNavigateToSettings()
                        BottomNavTab.Account -> onNavigateToAccount()
                    }
                }
            )
        }
    }
}
