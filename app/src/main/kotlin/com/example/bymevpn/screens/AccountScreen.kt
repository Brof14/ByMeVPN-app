package com.example.bymevpn.screens

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bymevpn.components.BottomNavTab
import com.example.bymevpn.components.ByMeBottomNav
import com.example.bymevpn.components.ByMeCardGroup
import com.example.bymevpn.components.ByMePrimaryButton
import com.example.bymevpn.components.ByMeSecondaryButton
import com.example.bymevpn.components.ByMeSettingRow
import com.example.bymevpn.components.ByMeTopBar
import com.example.bymevpn.components.ChevronRightIcon
import com.example.bymevpn.components.DevicesMultiIcon
import com.example.bymevpn.components.PersonProfileIcon
import com.example.bymevpn.components.QuestionHelpIcon
import com.example.bymevpn.components.RefreshIcon
import com.example.bymevpn.components.SettingsGearIcon
import com.example.bymevpn.data.AccountRepository
import com.example.bymevpn.data.subscription.SubscriptionManager
import com.example.bymevpn.theme.AppColors

@Composable
fun AccountScreen(
    onBackClick: () -> Unit,
    onNavigateToHome: () -> Unit = onBackClick,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToDevices: () -> Unit = {},
    onNavigateToRenewal: () -> Unit = {},
    onNavigateToSupport: () -> Unit = {},
    onNavigateToTrial: () -> Unit = {},
    onNavigateToPricing: () -> Unit = {},
    onLogOut: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val currentUser by AccountRepository.currentUser.collectAsState()
    val subManager = remember { SubscriptionManager.getInstance(context) }
    val subscription by subManager.subscriptionStatus.collectAsState()

    val isActive = subscription?.isActive == true
    val remainingDays = subscription?.daysRemaining ?: 0
    val maxDevices = 2

    var showAccountDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.BgCanvas)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ByMeTopBar(
                title = "Аккаунт",
                onBackClick = onBackClick,
                onMoreClick = { /* More actions */ }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 26.dp)
            ) {
                // Profile Minimal Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(51.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF1A2D2C))
                            .border(1.dp, Color(0xFF294A41), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        PersonProfileIcon(color = Color(0xFF65CAB0), size = 25.dp)
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentUser?.displayName ?: "Ваш аккаунт",
                            color = AppColors.TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentUser?.email ?: "Профиль ByMeVPN",
                            color = Color(0xFF8CA5A2),
                            fontSize = 11.5.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isActive) Color(0xFF1A3B2C) else Color(0xFF2B2424))
                            .padding(horizontal = 9.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = if (isActive) "Активен" else "Без подписки",
                            color = if (isActive) Color(0xFF65E3B1) else Color(0xFFE3A29A),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Plan Card: Active (10_account_active) vs Inactive (09_account_no_plan)
                if (isActive) {
                    // Active Plan Card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(21.dp))
                            .background(Color(0xFF19312B))
                            .border(1.dp, Color(0xFF2A5449), RoundedCornerShape(21.dp))
                            .padding(21.dp)
                    ) {
                        Text(
                            text = "ПОДПИСКА BYMEVPN",
                            color = Color(0xFF68B8A7),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$remainingDays",
                                color = AppColors.TextPrimary,
                                fontSize = 64.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-3).sp
                            )
                            Text(
                                text = " дня",
                                color = Color(0xFF6EDBB0),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 10.dp, start = 4.dp)
                            )
                        }

                        Text(
                            text = "до окончания доступа",
                            color = Color(0xFF9AB1A7),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF305248)))
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ваш тариф",
                                color = Color(0xFF99B3A8),
                                fontSize = 12.sp
                            )
                            Text(
                                text = "$maxDevices устройства · 1 месяц",
                                color = Color(0xFFE5F3EC),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        ByMeSecondaryButton(
                            text = "Продлить подписку",
                            onClick = onNavigateToPricing
                        )
                    }
                } else {
                    // Inactive Plan Card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(21.dp))
                            .background(Color(0xFF19312B))
                            .border(1.dp, Color(0xFF2A5449), RoundedCornerShape(21.dp))
                            .padding(21.dp)
                    ) {
                        Text(
                            text = "ВАШ ДОСТУП",
                            color = Color(0xFF68B8A7),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Подписка не активна",
                            color = AppColors.TextPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Оформите подписку, чтобы подключаться к серверам ByMeVPN.",
                            color = Color(0xFF96B0A6),
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        ByMePrimaryButton(
                            text = "Начать за 1 ₽",
                            onClick = onNavigateToTrial,
                            height = 50.dp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Посмотреть тарифы",
                            color = Color(0xFF8CD0B8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onNavigateToPricing)
                                .padding(vertical = 4.dp)
                        )
                    }
                }

                // Section: Управление
                Text(
                    text = "Управление",
                    color = Color(0xFF8BA3A4),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 22.dp, bottom = 10.dp, start = 2.dp)
                )

                ByMeCardGroup {
                    ByMeSettingRow(
                        title = if (isActive) "Мои устройства" else "Устройства",
                        subtitle = if (isActive) "1 из $maxDevices подключено" else "Доступны после активации",
                        icon = { DevicesMultiIcon(color = Color(0xFF62CBB0), size = 20.dp) },
                        trailing = { ChevronRightIcon(color = Color(0xFF71898A), size = 18.dp) },
                        onClick = onNavigateToDevices
                    )
                    ByMeSettingRow(
                        title = "Автопродление",
                        subtitle = if (isActive) "Включено" else "Нет активной подписки",
                        icon = { RefreshIcon(color = Color(0xFF62CBB0), size = 20.dp) },
                        trailing = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (isActive) {
                                    Text(
                                        text = "Включено",
                                        color = Color(0xFF40D5AD),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                ChevronRightIcon(color = Color(0xFF71898A), size = 18.dp)
                            }
                        },
                        onClick = onNavigateToRenewal,
                        showBottomDivider = false
                    )
                }

                // Section: Аккаунт
                Text(
                    text = "Аккаунт",
                    color = Color(0xFF8BA3A4),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 22.dp, bottom = 10.dp, start = 2.dp)
                )

                ByMeCardGroup {
                    ByMeSettingRow(
                        title = "Настройки аккаунта",
                        subtitle = null,
                        icon = { PersonProfileIcon(color = Color(0xFF62CBB0), size = 20.dp) },
                        trailing = { ChevronRightIcon(color = Color(0xFF71898A), size = 18.dp) },
                        onClick = { showAccountDialog = true }
                    )
                    ByMeSettingRow(
                        title = "Помощь",
                        subtitle = null,
                        icon = { QuestionHelpIcon(color = Color(0xFF62CBB0), size = 20.dp) },
                        trailing = { ChevronRightIcon(color = Color(0xFF71898A), size = 18.dp) },
                        onClick = onNavigateToSupport,
                        showBottomDivider = false
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            ByMeBottomNav(
                currentTab = BottomNavTab.Account,
                onTabSelected = { tab ->
                    when (tab) {
                        BottomNavTab.Home -> onNavigateToHome()
                        BottomNavTab.Settings -> onNavigateToSettings()
                        BottomNavTab.Account -> { /* already here */ }
                    }
                }
            )
        }

        if (showAccountDialog) {
            AlertDialog(
                onDismissRequest = { showAccountDialog = false },
                containerColor = Color(0xFF111A1D),
                titleContentColor = AppColors.TextPrimary,
                textContentColor = Color(0xFF8CA5A2),
                title = { Text("Настройки аккаунта") },
                text = {
                    Column {
                        Text("Пользователь: ${currentUser?.displayName ?: "Пользователь"}")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Почта: ${currentUser?.email ?: "Не указана"}")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("ID: ${currentUser?.id ?: "local"}")
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showAccountDialog = false
                        onLogOut()
                    }) {
                        Text("Выйти из аккаунта", color = Color(0xFFFF5353))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAccountDialog = false }) {
                        Text("Закрыть", color = Color(0xFF65E3B1))
                    }
                }
            )
        }
    }
}
