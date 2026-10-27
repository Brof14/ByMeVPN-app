package com.example.bymevpn.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import com.example.bymevpn.components.ByMeSettingRow
import com.example.bymevpn.components.ByMeToggle
import com.example.bymevpn.components.ByMeTopBar
import com.example.bymevpn.components.ChevronRightIcon
import com.example.bymevpn.components.GlobeIcon
import com.example.bymevpn.components.PowerIcon
import com.example.bymevpn.components.SettingsGearIcon
import com.example.bymevpn.components.ShieldSecurityIcon
import com.example.bymevpn.components.SparkleStarIcon
import com.example.bymevpn.data.LocaleManager
import com.example.bymevpn.data.settings.SettingsRepository
import com.example.bymevpn.theme.AppColors

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onNavigateToHome: () -> Unit = onBackClick,
    onNavigateToAccount: () -> Unit = {},
    onNavigateToAdvanced: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val settingsRepo = remember { SettingsRepository.getInstance(context) }

    val autoConnect by settingsRepo.autoConnectOnLaunch.collectAsState(initial = false)
    val bypassRuSites by settingsRepo.bypassRussianSites.collectAsState(initial = true)
    val backgroundProtection by settingsRepo.batteryOptimizationDismissed.collectAsState(initial = true)
    val adBlock by settingsRepo.adBlockEnabled.collectAsState(initial = false)

    var localAutoConnect by remember { mutableStateOf(autoConnect) }
    var localBypassRu by remember { mutableStateOf(bypassRuSites) }
    var localBgProtection by remember { mutableStateOf(backgroundProtection) }
    var localAdBlock by remember { mutableStateOf(adBlock) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.BgCanvas)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ByMeTopBar(
                title = "Настройки",
                onBackClick = onBackClick,
                onMoreClick = { /* More actions */ }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 26.dp)
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Приложение работает за вас.",
                    color = AppColors.TextPrimary,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Основные настройки уже подобраны. При желании их можно изменить.",
                    color = Color(0xFF8EA4A2),
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Section: Подключение
                Text(
                    text = "Подключение",
                    color = Color(0xFF8BA3A4),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 10.dp, bottom = 10.dp, start = 2.dp)
                )

                ByMeCardGroup {
                    ByMeSettingRow(
                        title = "Подключаться автоматически",
                        subtitle = "При запуске приложения",
                        icon = { PowerIcon(color = Color(0xFF62CBB0), size = 20.dp) },
                        trailing = {
                            ByMeToggle(
                                checked = localAutoConnect,
                                onCheckedChange = { checked ->
                                    localAutoConnect = checked
                                    settingsRepo.setAutoConnectOnLaunch(checked)
                                }
                            )
                        }
                    )
                    ByMeSettingRow(
                        title = "Российские сайты напрямую",
                        subtitle = "Банки и местные сервисы без VPN",
                        icon = { GlobeIcon(color = Color(0xFF62CBB0), size = 20.dp) },
                        trailing = {
                            ByMeToggle(
                                checked = localBypassRu,
                                onCheckedChange = { checked ->
                                    localBypassRu = checked
                                    settingsRepo.setBypassRussianSites(checked)
                                }
                            )
                        },
                        showBottomDivider = false
                    )
                }

                // Section: Комфорт
                Text(
                    text = "Комфорт",
                    color = Color(0xFF8BA3A4),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 22.dp, bottom = 10.dp, start = 2.dp)
                )

                ByMeCardGroup {
                    ByMeSettingRow(
                        title = "Защита в фоне",
                        subtitle = "Не отключать VPN при сворачивании",
                        icon = { ShieldSecurityIcon(color = Color(0xFF62CBB0), size = 20.dp) },
                        trailing = {
                            ByMeToggle(
                                checked = localBgProtection,
                                onCheckedChange = { checked ->
                                    localBgProtection = checked
                                    settingsRepo.setBatteryOptimizationDismissed(checked)
                                }
                            )
                        }
                    )
                    ByMeSettingRow(
                        title = "Блокировка рекламы",
                        subtitle = "Убирать часть баннеров и трекеров",
                        icon = { SparkleStarIcon(color = Color(0xFF62CBB0), size = 20.dp) },
                        trailing = {
                            ByMeToggle(
                                checked = localAdBlock,
                                onCheckedChange = { checked ->
                                    localAdBlock = checked
                                    settingsRepo.setAdBlockEnabled(checked)
                                }
                            )
                        },
                        showBottomDivider = false
                    )
                }

                // Section: Дополнительно
                Text(
                    text = "Дополнительно",
                    color = Color(0xFF8BA3A4),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 22.dp, bottom = 10.dp, start = 2.dp)
                )

                ByMeCardGroup {
                    ByMeSettingRow(
                        title = "Дополнительные настройки",
                        subtitle = "DNS, приложения и защита при разрыве",
                        icon = { SettingsGearIcon(color = Color(0xFF62CBB0), size = 20.dp) },
                        trailing = { ChevronRightIcon(color = Color(0xFF71898A), size = 18.dp) },
                        onClick = onNavigateToAdvanced
                    )
                    ByMeSettingRow(
                        title = "Язык",
                        subtitle = "Русский",
                        icon = { GlobeIcon(color = Color(0xFF62CBB0), size = 20.dp) },
                        trailing = { ChevronRightIcon(color = Color(0xFF71898A), size = 18.dp) },
                        onClick = {
                            Toast.makeText(context, "Выбран Русский язык", Toast.LENGTH_SHORT).show()
                        },
                        showBottomDivider = false
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                ByMeInfoBox(
                    text = "Не уверены, что выбрать? Оставьте настройки по умолчанию."
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            ByMeBottomNav(
                currentTab = BottomNavTab.Settings,
                onTabSelected = { tab ->
                    when (tab) {
                        BottomNavTab.Home -> onNavigateToHome()
                        BottomNavTab.Settings -> { /* already here */ }
                        BottomNavTab.Account -> onNavigateToAccount()
                    }
                }
            )
        }
    }
}
