package com.example.bymevpn.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bymevpn.components.BottomNavTab
import com.example.bymevpn.components.ByMeBottomNav
import com.example.bymevpn.components.ByMeCardGroup
import com.example.bymevpn.components.ByMeSettingRow
import com.example.bymevpn.components.ByMeToggle
import com.example.bymevpn.components.ByMeTopBar
import com.example.bymevpn.components.ChevronRightIcon
import com.example.bymevpn.components.GlobeIcon
import com.example.bymevpn.components.LockIcon
import com.example.bymevpn.components.SmartphoneIcon
import com.example.bymevpn.data.settings.SettingsRepository
import com.example.bymevpn.theme.AppColors

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

    val killSwitchEnabled by settingsRepo.killSwitchEnabled.collectAsState(initial = false)
    val dnsMode by settingsRepo.dnsMode.collectAsState(initial = "auto")

    var localKillSwitch by remember { mutableStateOf(killSwitchEnabled) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.BgCanvas)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ByMeTopBar(
                title = "Дополнительно",
                onBackClick = onBackClick,
                onMoreClick = { /* More actions */ }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 26.dp)
            ) {
                Text(
                    text = "Эти параметры необязательно изменять. Они нужны для особых сценариев подключения.",
                    color = Color(0xFF8EA4A2),
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )

                // Section: Защита
                Text(
                    text = "Защита",
                    color = Color(0xFF8BA3A4),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 10.dp, start = 2.dp)
                )

                ByMeCardGroup {
                    ByMeSettingRow(
                        title = "Защита при обрыве VPN",
                        subtitle = "Блокирует интернет, если VPN неожиданно отключится",
                        icon = { LockIcon(color = Color(0xFF62CBB0), size = 20.dp) },
                        trailing = {
                            ByMeToggle(
                                checked = localKillSwitch,
                                onCheckedChange = { checked ->
                                    localKillSwitch = checked
                                    settingsRepo.setKillSwitchEnabled(checked)
                                    val msg = if (checked) "Kill Switch активирован" else "Kill Switch отключен"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    )
                    ByMeSettingRow(
                        title = "Приложения без VPN",
                        subtitle = "Выберите исключения из подключения",
                        icon = { SmartphoneIcon(color = Color(0xFF62CBB0), size = 20.dp) },
                        trailing = { ChevronRightIcon(color = Color(0xFF71898A), size = 18.dp) },
                        onClick = {
                            Toast.makeText(context, "Исключения настроены по умолчанию (российские сервисы и банки)", Toast.LENGTH_SHORT).show()
                        },
                        showBottomDivider = false
                    )
                }

                // Section: Соединение
                Text(
                    text = "Соединение",
                    color = Color(0xFF8BA3A4),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 22.dp, bottom = 10.dp, start = 2.dp)
                )

                ByMeCardGroup {
                    ByMeSettingRow(
                        title = "DNS-сервер",
                        subtitle = "Автоматически · рекомендуется",
                        icon = { GlobeIcon(color = Color(0xFF62CBB0), size = 20.dp) },
                        trailing = { ChevronRightIcon(color = Color(0xFF71898A), size = 18.dp) },
                        onClick = {
                            Toast.makeText(context, "Используется защищенный Cloudflare/Google DNS", Toast.LENGTH_SHORT).show()
                        },
                        showBottomDivider = false
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // DNS explainer box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(15.dp))
                        .background(Color(0xFF102420))
                        .border(1.dp, Color(0xFF21463B), RoundedCornerShape(15.dp))
                        .padding(17.dp)
                ) {
                    Text(
                        text = "ЧТО ТАКОЕ DNS?",
                        color = Color(0xFF698589),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(7.dp))
                    Text(
                        text = "Сервис, который помогает устройству находить сайты. Для большинства пользователей менять его не нужно.",
                        color = Color(0xFF9DB6AC),
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }

                // Section: Язык
                Text(
                    text = "Язык",
                    color = Color(0xFF8BA3A4),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 22.dp, bottom = 10.dp, start = 2.dp)
                )

                ByMeCardGroup {
                    ByMeSettingRow(
                        title = "Язык приложения",
                        subtitle = "Русский",
                        icon = { GlobeIcon(color = Color(0xFF62CBB0), size = 20.dp) },
                        trailing = { ChevronRightIcon(color = Color(0xFF71898A), size = 18.dp) },
                        onClick = {
                            Toast.makeText(context, "Текущий язык: Русский", Toast.LENGTH_SHORT).show()
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
