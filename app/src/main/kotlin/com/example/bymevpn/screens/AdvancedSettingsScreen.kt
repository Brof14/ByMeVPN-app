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
import com.example.bymevpn.components.CheckmarkIcon
import com.example.bymevpn.components.ChevronRightIcon
import com.example.bymevpn.components.GlobeIcon
import com.example.bymevpn.components.LockIcon
import com.example.bymevpn.components.ShieldSecurityIcon
import com.example.bymevpn.components.SmartphoneIcon
import com.example.bymevpn.components.SplitTunnelIcon
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
    onNavigateToSplitTunneling: () -> Unit = {},
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
                    .background(AppColors.BgSurface)
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
                            .background(if (selected) AppColors.BgSurfaceElevated else AppColors.BgInput)
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
                                CheckmarkIcon(color = AppColors.BrandMintDark, size = 14.dp)
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
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isRu) "Сеть и маршрутизация" else "Network & Routing",
                    color = AppColors.TextKicker,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                ByMeCardGroup {
                    val currentDns = DNS_OPTIONS.find { it.id == settings.dnsMode }
                    ByMeSettingRow(
                        title = if (isRu) "DNS-сервер" else "DNS Provider",
                        subtitle = if (isRu) currentDns?.nameRu ?: settings.dnsMode else currentDns?.nameEn ?: settings.dnsMode,
                        icon = { GlobeIcon(color = AppColors.SkyBlue, size = 20.dp) },
                        trailing = { ChevronRightIcon(color = AppColors.TextMuted, size = 18.dp) },
                        onClick = {
                            haptics.tap()
                            showDnsDialog = true
                        },
                        showBottomDivider = true
                    )
                    val excludedCount = settings.excludedApps.size
                    val splitSubtitle = when {
                        settings.splitTunnelingEnabled && excludedCount > 0 ->
                            if (isRu) "Включено · исключено $excludedCount" else "Enabled · $excludedCount excluded"
                        settings.splitTunnelingEnabled ->
                            if (isRu) "Включено · выберите приложения" else "Enabled · choose apps"
                        else ->
                            if (isRu) "Исключить приложения из VPN" else "Bypass VPN for selected apps"
                    }
                    ByMeSettingRow(
                        title = if (isRu) "Раздельное туннелирование" else "Split tunneling",
                        subtitle = splitSubtitle,
                        icon = { SplitTunnelIcon(color = AppColors.SkyBlue, size = 20.dp) },
                        trailing = { ChevronRightIcon(color = AppColors.TextMuted, size = 18.dp) },
                        onClick = {
                            haptics.tap()
                            onNavigateToSplitTunneling()
                        },
                        showBottomDivider = false
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = if (isRu) "Уведомления и скорость" else "Notifications & Speed",
                    color = AppColors.TextKicker,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                ByMeCardGroup {
                    ByMeSettingRow(
                        title = if (isRu) "Скорость в шторке" else "Speed in notification",
                        subtitle = if (isRu) "Скорость загрузки и отдачи в уведомлении"
                        else "Show download and upload speed in notification",
                        icon = { SmartphoneIcon(color = AppColors.SkyBlue, size = 20.dp) },
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
                        subtitle = if (isRu) "Статус подключения в системном уведомлении"
                        else "Connection status in system notification",
                        icon = { ShieldSecurityIcon(color = AppColors.SkyBlue, size = 20.dp) },
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

                Text(
                    text = if (isRu) "Защита системы" else "System protection",
                    color = AppColors.TextKicker,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                ByMeCardGroup {
                    ByMeSettingRow(
                        title = if (isRu) "Постоянный VPN и Kill Switch" else "Always-on VPN & Kill Switch",
                        subtitle = if (isRu) "Системные параметры Android для защиты от утечек"
                        else "Android VPN settings for Always-on and leak protection",
                        icon = { LockIcon(color = AppColors.SkyBlue, size = 20.dp) },
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

                Spacer(modifier = Modifier.height(24.dp))
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
