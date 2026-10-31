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

    val language by com.example.bymevpn.data.LocaleManager.currentLanguage.collectAsState()
    val isRu = com.example.bymevpn.data.LocaleManager.isRussian(language = language)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.BgCanvas)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ByMeTopBar(
                title = if (isRu) "Дополнительно" else "More settings",
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
                    text = if (isRu) "Эти параметры необязательно изменять. Они нужны для особых сценариев подключения." else "You can change these options when needed.",
                    color = Color(0xFF8EA4A2),
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )

                // Section: Защита
                Text(
                    text = if (isRu) "Защита" else "Protection",
                    color = Color(0xFF8BA3A4),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 10.dp, start = 2.dp)
                )

                ByMeCardGroup {
                    ByMeSettingRow(
                        title = if (isRu) "Защита при обрыве VPN" else "Protection if VPN disconnects",
                        subtitle = if (isRu) "Включите постоянный VPN и блокировку в настройках Android" else "Enable always-on VPN and blocking in Android settings",
                        icon = { LockIcon(color = Color(0xFF7AC9CB), size = 20.dp) },
                        trailing = { ChevronRightIcon(color = Color(0xFF71898A), size = 18.dp) },
                        onClick = { context.startActivity(android.content.Intent(android.provider.Settings.ACTION_VPN_SETTINGS)) },
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
