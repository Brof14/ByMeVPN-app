package com.example.bymevpn.screens

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
import com.example.bymevpn.components.PowerIcon
import com.example.bymevpn.components.SettingsGearIcon
import com.example.bymevpn.data.AppLanguage
import com.example.bymevpn.data.LocaleManager
import com.example.bymevpn.data.settings.AppSettings
import com.example.bymevpn.data.settings.SettingsRepository
import com.example.bymevpn.theme.AppColors
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onNavigateToHome: () -> Unit = onBackClick,
    onNavigateToAccount: () -> Unit = {},
    onNavigateToAdvancedSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val settingsRepo = remember { SettingsRepository.getInstance(context) }

    val bypassRuSites by settingsRepo.bypassRussianSites.collectAsState(initial = true)
    val settings by settingsRepo.settingsFlow.collectAsState(initial = AppSettings())

    val language by LocaleManager.currentLanguage.collectAsState()
    val isRu = LocaleManager.isRussian(language = language)
    val scope = rememberCoroutineScope()
    var showLanguage by remember { mutableStateOf(false) }

    if (showLanguage) {
        Dialog(
            onDismissRequest = { showLanguage = false },
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
                    text = if (isRu) "Язык приложения" else "App language",
                    color = AppColors.TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isRu) "Выберите удобный язык интерфейса." else "Choose the language you prefer.",
                    color = AppColors.TextSecondary,
                    fontSize = 13.sp
                )

                AppLanguage.entries.forEach { choice ->
                    val selected = language == choice
                    val detail = when (choice) {
                        AppLanguage.SYSTEM -> if (isRu) "Следовать настройкам телефона" else "Follow your phone settings"
                        AppLanguage.RUSSIAN -> if (isRu) "Полностью на русском" else "Russian interface"
                        AppLanguage.ENGLISH -> if (isRu) "Полностью на английском" else "English interface"
                    }
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
                                LocaleManager.setLanguage(choice)
                                scope.launch { settingsRepo.setLanguage(choice.code) }
                                showLanguage = false
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
                                text = if (isRu) choice.titleRu else choice.titleEn,
                                color = AppColors.TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(detail, color = AppColors.TextSecondary, fontSize = 11.5.sp)
                        }
                    }
                }

                Text(
                    text = if (isRu) "Изменение применяется сразу." else "The change takes effect immediately.",
                    color = AppColors.TextMuted,
                    fontSize = 11.5.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
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
                title = if (isRu) "Настройки" else "Settings",
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
                    text = if (isRu) "Подключение" else "Connection",
                    color = AppColors.TextKicker,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                ByMeCardGroup {
                    ByMeSettingRow(
                        title = if (isRu) "Автопереподключение" else "Auto-reconnect",
                        subtitle = if (isRu) "Восстанавливать VPN после потери сети" else "Restore VPN after network drops",
                        icon = { PowerIcon(color = AppColors.SkyBlue, size = 20.dp) },
                        trailing = {
                            ByMeToggle(
                                checked = settings.autoReconnect,
                                onCheckedChange = { checked ->
                                    scope.launch { settingsRepo.updateAutoReconnect(checked) }
                                }
                            )
                        }
                    )
                    ByMeSettingRow(
                        title = if (isRu) "Российские сайты напрямую" else "Russian sites directly",
                        subtitle = if (isRu) "Локальные сайты открываются без VPN" else "Russian sites bypass the VPN tunnel",
                        icon = { GlobeIcon(color = AppColors.SkyBlue, size = 20.dp) },
                        trailing = {
                            ByMeToggle(
                                checked = bypassRuSites,
                                onCheckedChange = { checked ->
                                    settingsRepo.setBypassRussianSites(checked)
                                    Toast.makeText(
                                        context,
                                        if (isRu) "Применится при следующем подключении" else "Applied next time you connect",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        },
                        showBottomDivider = false
                    )
                }

                Text(
                    text = if (isRu) "Язык приложения" else "App language",
                    color = AppColors.TextKicker,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
                )
                ByMeCardGroup {
                    ByMeSettingRow(
                        title = if (isRu) "Язык" else "Language",
                        subtitle = if (isRu) language.titleRu else language.titleEn,
                        icon = { GlobeIcon(color = AppColors.SkyBlue, size = 20.dp) },
                        trailing = { ChevronRightIcon(color = AppColors.TextMuted, size = 18.dp) },
                        onClick = { showLanguage = true },
                        showBottomDivider = false
                    )
                }

                Text(
                    text = if (isRu) "Дополнительно" else "Advanced",
                    color = AppColors.TextKicker,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
                )
                ByMeCardGroup {
                    ByMeSettingRow(
                        title = if (isRu) "Расширенные настройки" else "Advanced settings",
                        subtitle = if (isRu) "DNS, исключения приложений, уведомления" else "DNS, split tunneling, notifications",
                        icon = { SettingsGearIcon(color = AppColors.SkyBlue, size = 20.dp) },
                        trailing = { ChevronRightIcon(color = AppColors.TextMuted, size = 18.dp) },
                        onClick = onNavigateToAdvancedSettings,
                        showBottomDivider = false
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
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
