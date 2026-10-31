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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import kotlinx.coroutines.launch

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
    val devices by AccountRepository.userDevices.collectAsState()

    val isActive = subscription?.let { it.isActive && !it.knownExpiryReached(System.currentTimeMillis()) } == true
    val remainingDays = subscription?.daysRemainingAt(System.currentTimeMillis())
    val maxDevices = subscription?.maxDevices?.takeIf { it > 0 }
    val activeDevicesCount = devices.size

    var showAccountDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deletingAccount by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.BgCanvas)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ByMeTopBar(
                title = "Аккаунт",
                onBackClick = onBackClick,
                onMoreClick = null
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
                            .background(Color(0xFF192C3D))
                            .border(1.dp, Color(0xFF294A41), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        PersonProfileIcon(color = Color(0xFF79C7C6), size = 25.dp)
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
                            text = currentUser?.email?.takeIf { it.isNotBlank() && it != "user@bymevpn.app" } ?: "Локальная подписка",
                            color = Color(0xFF8CA5A2),
                            fontSize = 11.5.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isActive) Color(0xFF193743) else Color(0xFF2B2424))
                            .padding(horizontal = 9.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = if (isActive) "Активен" else "Без подписки",
                            color = if (isActive) Color(0xFF8CDECA) else Color(0xFFE3A29A),
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
                            .background(Color(0xFF1A2D40))
                            .border(1.dp, Color(0xFF34566A), RoundedCornerShape(21.dp))
                            .padding(21.dp)
                    ) {
                        Text(
                            text = "ПОДПИСКА BYMEVPN",
                            color = Color(0xFF8FBBCA),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = remainingDays?.toString() ?: "—",
                                color = AppColors.TextPrimary,
                                fontSize = 64.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-3).sp
                            )
                            Text(
                                text = if (remainingDays == null) "" else " дн.",
                                color = Color(0xFF80D3C6),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 10.dp, start = 4.dp)
                            )
                        }

                        Text(
                            text = if (remainingDays == null) "Срок доступа не подтверждён" else "до окончания доступа",
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
                                text = subscription?.planName?.takeIf { it.isNotBlank() } ?: "Не указан",
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
                            .background(Color(0xFF1A2D40))
                            .border(1.dp, Color(0xFF34566A), RoundedCornerShape(21.dp))
                            .padding(21.dp)
                    ) {
                        Text(
                            text = "ВАШ ДОСТУП",
                            color = Color(0xFF8FBBCA),
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
                            text = "3 дня бесплатно",
                            onClick = onNavigateToTrial,
                            height = 50.dp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Посмотреть тарифы",
                            color = Color(0xFF90CFCE),
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
                        subtitle = if (devices.isNotEmpty()) {
                            maxDevices?.let { "$activeDevicesCount из $it" } ?: "$activeDevicesCount устройств"
                        } else "Нет подтверждённого списка устройств",
                        icon = { DevicesMultiIcon(color = Color(0xFF7AC9CB), size = 20.dp) },
                        trailing = { ChevronRightIcon(color = Color(0xFF71898A), size = 18.dp) },
                        onClick = onNavigateToDevices,
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
                        subtitle = "Имя, почта и автопродление",
                        icon = { PersonProfileIcon(color = Color(0xFF7AC9CB), size = 20.dp) },
                        trailing = { ChevronRightIcon(color = Color(0xFF71898A), size = 18.dp) },
                        onClick = { showAccountDialog = true },
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
                containerColor = Color(0xFF121C2B),
                titleContentColor = AppColors.TextPrimary,
                textContentColor = Color(0xFF8CA5A2),
                title = { Text("Настройки аккаунта") },
                text = {
                    Column {
                        AccountInfoLine("Имя", currentUser?.displayName.orEmpty().ifBlank { "Не указано" })
                        Spacer(modifier = Modifier.height(10.dp))
                        AccountInfoLine(
                            "Почта",
                            currentUser?.email?.takeIf { it.isNotBlank() && it != "user@bymevpn.app" } ?: "Не указана"
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFF2B4050))
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Автопродление", color = Color(0xFFEAF1EF), fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                                Text("Статус, подтверждённый сервером", color = Color(0xFF8CA5A2), fontSize = 11.sp)
                            }
                            val renewLabel = when (subscription?.autoRenew) {
                                true -> "Включено"
                                false -> "Отключено"
                                null -> "Нет данных"
                            }
                            val renewColor = when (subscription?.autoRenew) {
                                true -> Color(0xFF8CDECA)
                                false -> Color(0xFFE3B26A)
                                null -> Color(0xFF8CA5A2)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF182A3B))
                                    .padding(horizontal = 9.dp, vertical = 5.dp)
                            ) {
                                Text(renewLabel, color = renewColor, fontSize = 10.5.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "Переключатель появится, когда сервер начнёт возвращать управление автопродлением. Сейчас приложение показывает только подтверждённый статус.",
                            color = Color(0xFF8CA5A2),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        AccountInfoLine("Версия приложения", "1.0.0")
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Удалить аккаунт и данные",
                            color = Color(0xFFFF8A8A),
                            fontSize = 12.sp,
                            modifier = Modifier.clickable {
                                showAccountDialog = false
                                showDeleteDialog = true
                            }.padding(vertical = 10.dp)
                        )
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
                        Text("Закрыть", color = Color(0xFF8CDECA))
                    }
                }
            )
        }
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { if (!deletingAccount) showDeleteDialog = false },
                title = { Text("Удалить аккаунт навсегда?") },
                text = { Text("Это действие удалит аккаунт на сервере и приведёт к потере доступа к подписке. Отменить удаление невозможно.") },
                confirmButton = {
                    TextButton(enabled = !deletingAccount, onClick = {
                        deletingAccount = true
                        scope.launch {
                            val result = AccountRepository.deleteAccount(context)
                            deletingAccount = false
                            if (result.isSuccess) {
                                showDeleteDialog = false
                                onLogOut()
                            } else {
                                Toast.makeText(context, "Удаление через приложение пока недоступно. Напишите в @ByMeVPNSupportBot.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }) { Text("Удалить навсегда", color = Color(0xFFFF7373)) }
                },
                dismissButton = {
                    TextButton(enabled = !deletingAccount, onClick = { showDeleteDialog = false }) { Text("Отмена") }
                }
            )
        }
    }
}

@Composable
private fun AccountInfoLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(label, color = Color(0xFF8CA5A2), fontSize = 12.5.sp)
        Text(
            text = value,
            color = Color(0xFFEAF1EF),
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        )
    }
}
