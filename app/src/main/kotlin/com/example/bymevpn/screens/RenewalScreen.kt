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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.bymevpn.components.ByMeCardGroup
import com.example.bymevpn.components.ByMeInfoBox
import com.example.bymevpn.components.ByMeSettingRow
import com.example.bymevpn.components.ByMeTopBar
import com.example.bymevpn.components.ChevronRightIcon
import com.example.bymevpn.components.CreditCardIcon
import com.example.bymevpn.theme.AppColors

@Composable
fun RenewalScreen(
    onBackClick: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var isAutoRenewActive by remember { mutableStateOf(true) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.BgCanvas)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ByMeTopBar(
                title = "Автопродление",
                onBackClick = onBackClick,
                onMoreClick = { /* More actions */ }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 26.dp, vertical = 12.dp)
            ) {
                // Main Status Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(21.dp))
                        .background(Color(0xFF17352B))
                        .border(1.dp, Color(0xFF2B6D54), RoundedCornerShape(21.dp))
                        .padding(21.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ТЕКУЩИЙ СТАТУС",
                            color = Color(0xFF68B8A7),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isAutoRenewActive) Color(0xFF1A3B2C) else Color(0xFF2B2424))
                                .padding(horizontal = 9.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isAutoRenewActive) "Включено" else "Отключено",
                                color = if (isAutoRenewActive) Color(0xFF65E3B1) else Color(0xFFE3A29A),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = if (isAutoRenewActive) "Доступ продлевается\nавтоматически" else "Автопродление отключено",
                        color = AppColors.TextPrimary,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 29.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (isAutoRenewActive) "Не нужно каждый раз оплачивать подписку вручную." else "Подписка действует до окончания оплаченного срока.",
                        color = Color(0xFF94B6A8),
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Следующее списание",
                    color = Color(0xFF8BA3A4),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 10.dp, start = 2.dp)
                )

                ByMeCardGroup {
                    ByMeSettingRow(
                        title = "89 ₽ · каждые 30 дней",
                        subtitle = "Точная дата определяется активной подпиской",
                        icon = { CreditCardIcon(color = Color(0xFF62CBB0), size = 20.dp) },
                        trailing = { ChevronRightIcon(color = Color(0xFF71898A), size = 18.dp) },
                        showBottomDivider = false
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Cancel Box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(19.dp))
                        .background(Color(0xFF111A1D))
                        .border(1.dp, Color(0xFF2C3F3E), RoundedCornerShape(19.dp))
                        .padding(22.dp)
                ) {
                    Text(
                        text = if (isAutoRenewActive) "Хотите отключить?" else "Автопродление отключено",
                        color = AppColors.TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Автопродление можно отменить в любой момент. Доступ сохранится до окончания уже оплаченного периода.",
                        color = Color(0xFF91A6A4),
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                    Spacer(modifier = Modifier.height(18.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(Color(0xFF142022))
                            .border(1.dp, Color(0xFF627371), RoundedCornerShape(13.dp))
                            .clickable {
                                isAutoRenewActive = !isAutoRenewActive
                                val msg = if (isAutoRenewActive) "Автопродление возобновлено" else "Автопродление отключено"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isAutoRenewActive) "Отключить автопродление" else "Включить автопродление",
                            color = Color(0xFFE0ECE9),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                ByMeInfoBox(
                    text = "В интерфейсе должны отображаться только подтверждённые сервером сумма, дата и статус."
                )

                Spacer(modifier = Modifier.height(24.dp))
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
