package com.example.bymevpn.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.bymevpn.components.ByMeTopBar
import com.example.bymevpn.components.SmartphoneIcon
import com.example.bymevpn.data.AccountRepository
import com.example.bymevpn.data.subscription.SubscriptionManager
import com.example.bymevpn.theme.AppColors

@Composable
fun DevicesScreen(
    onBackClick: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val subManager = remember { SubscriptionManager.getInstance(context) }
    val subscription by subManager.subscriptionStatus.collectAsState()
    val devicesList by AccountRepository.userDevices.collectAsState()

    val effectiveMax = subscription?.maxDevices?.takeIf { it > 0 }
    val effectiveConnected = devicesList.size

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.BgCanvas)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ByMeTopBar(
                title = "Мои устройства",
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
                        text = if (devicesList.isEmpty()) "—" else "$effectiveConnected",
                        color = AppColors.TextPrimary,
                        fontSize = 60.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-3).sp
                    )
                    Text(
                        text = effectiveMax?.let { " из $it" } ?: "",
                        color = Color(0xFF6EDBB0),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 10.dp, start = 4.dp)
                    )
                }

                Text(
                    text = if (devicesList.isEmpty()) "Данные об устройствах недоступны" else "Устройств в ответе сервера",
                    color = Color(0xFF92A9A5),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )

                if (devicesList.none { it.isCurrent }) {
                    Text("Этот телефон · ${android.os.Build.MODEL}", color = AppColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("Текущее устройство. Регистрация на сервере не подтверждена.", color = Color(0xFF92A9A5), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, bottom = 18.dp))
                }

                // List of Devices
                if (devicesList.isNotEmpty()) {
                    devicesList.forEach { dev ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(89.dp)
                                .clip(RoundedCornerShape(17.dp))
                                .background(Color(0xFF12221F))
                                .border(1.dp, Color(0xFF28443C), RoundedCornerShape(17.dp))
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(37.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(Color(0xFF172A29)),
                                contentAlignment = Alignment.Center
                            ) {
                                SmartphoneIcon(color = Color(0xFF62CBB0), size = 21.dp)
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (dev.isCurrent) "Этот телефон (${dev.deviceModel})" else dev.deviceModel,
                                    color = Color(0xFFEAF1EF),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${dev.platform} · ${dev.lastActive}",
                                    color = Color(0xFF88A09F),
                                    fontSize = 11.5.sp
                                )
                            }

                            if (dev.isCurrent) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF1A3B2C))
                                        .padding(horizontal = 9.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Вы здесь",
                                        color = Color(0xFF65E3B1),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                } else {
                    Text("Список устройств не подтверждён сервером.", color = AppColors.TextPrimary, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Info Box "ДОБАВИТЬ УСТРОЙСТВО"
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(17.dp))
                        .background(Color(0xFF102420))
                        .border(1.dp, Color(0xFF21463B), RoundedCornerShape(17.dp))
                        .padding(18.dp)
                ) {
                    Text(
                        text = "ДОБАВИТЬ УСТРОЙСТВО",
                        color = Color(0xFF68B8A7),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Войдите в ByMeVPN на другом устройстве с тем же аккаунтом. Оно появится здесь после регистрации на сервере.",
                        color = Color(0xFF9DB6AC),
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
    }
}
