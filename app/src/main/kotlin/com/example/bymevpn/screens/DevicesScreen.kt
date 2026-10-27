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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bymevpn.components.BottomNavTab
import com.example.bymevpn.components.ByMeBottomNav
import com.example.bymevpn.components.ByMeTopBar
import com.example.bymevpn.components.SmartphoneIcon
import com.example.bymevpn.theme.AppColors

@Composable
fun DevicesScreen(
    onBackClick: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAccount: () -> Unit,
    connectedDevicesCount: Int = 1,
    maxDevicesCount: Int = 2,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.BgCanvas)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ByMeTopBar(
                title = "Мои устройства",
                onBackClick = onBackClick,
                onMoreClick = { /* More actions */ }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 26.dp, vertical = 12.dp)
            ) {
                // Large device count display matching mockup 15_devices
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$connectedDevicesCount",
                        color = AppColors.TextPrimary,
                        fontSize = 60.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-3).sp
                    )
                    Text(
                        text = " из $maxDevicesCount",
                        color = Color(0xFF6EDBB0),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 10.dp, start = 4.dp)
                    )
                }

                Text(
                    text = "Устройство подключено к вашей подписке",
                    color = Color(0xFF92A9A5),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 28.dp)
                )

                // Current Device Card
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
                            text = "Этот телефон",
                            color = Color(0xFFEAF1EF),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Android · Активно",
                            color = Color(0xFF88A09F),
                            fontSize = 11.5.sp
                        )
                    }

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

                Spacer(modifier = Modifier.height(24.dp))

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
                        text = "Войдите в ByMeVPN на другом устройстве с тем же аккаунтом. Оно появится здесь автоматически, если это поддерживается вашим тарифом и платформой.",
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
