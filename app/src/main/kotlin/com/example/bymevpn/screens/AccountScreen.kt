package com.example.bymevpn.screens

import android.widget.Toast
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

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
import com.example.bymevpn.data.LocaleManager
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
    val language by LocaleManager.currentLanguage.collectAsState()
    val isRu = LocaleManager.isRussian(language = language)
    val t: (String, String) -> String = { ru, en -> if (isRu) ru else en }
    val scrollState = rememberScrollState()

    val currentUser by AccountRepository.currentUser.collectAsState()
    val subManager = remember { SubscriptionManager.getInstance(context) }
    val subscription by subManager.subscriptionStatus.collectAsState()
    val devices by AccountRepository.userDevices.collectAsState()

    val isActive = subscription?.let { it.isActive && !it.knownExpiryReached(System.currentTimeMillis()) } == true
    val remainingDays = subscription?.daysRemainingAt(System.currentTimeMillis())
    val maxDevices = subscription?.maxDevices?.takeIf { it > 0 }
    val activeDevicesCount = devices.size
    val effectiveDevicesText = maxDevices?.let { "$activeDevicesCount / $it" } ?: t("Лимит уточняется", "Limit pending")

    var showAccountDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deletingAccount by remember { mutableStateOf(false) }
    var refreshingAccount by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.BgCanvas)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ByMeTopBar(
                title = t("Аккаунт", "Account"),
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
                            text = currentUser?.displayName ?: t("Ваш аккаунт", "Your account"),
                            color = AppColors.TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentUser?.email?.takeIf { it.isNotBlank() && it != "user@bymevpn.app" } ?: t("Локальная подписка", "Local subscription"),
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
                            text = if (isActive) t("Активен", "Active") else t("Без подписки", "No subscription"),
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
                            text = t("ПОДПИСКА BYMEVPN", "BYMEVPN SUBSCRIPTION"),
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
                                text = if (remainingDays == null) "" else t(" дн.", " days"),
                                color = Color(0xFF80D3C6),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 10.dp, start = 4.dp)
                            )
                        }

                        Text(
                            text = if (remainingDays == null) t("Срок доступа не подтверждён", "Access expiry not confirmed") else t("до окончания доступа", "until access expires"),
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
                                text = t("Ваш тариф", "Your plan"),
                                color = Color(0xFF99B3A8),
                                fontSize = 12.sp
                            )
                            Text(
                                text = subscription?.planName?.takeIf { it.isNotBlank() } ?: t("Не указан", "Not specified"),
                                color = Color(0xFFE5F3EC),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        ByMeSecondaryButton(
                            text = t("Продлить подписку", "Renew subscription"),
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
                            text = t("ВАШ ДОСТУП", "YOUR ACCESS"),
                            color = Color(0xFF8FBBCA),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = t("Подписка не активна", "Subscription inactive"),
                            color = AppColors.TextPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = t("Оформите подписку, чтобы подключаться к серверам ByMeVPN.", "Choose a plan to connect to ByMeVPN servers."),
                            color = Color(0xFF96B0A6),
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        ByMePrimaryButton(
                            text = t("3 дня бесплатно", "3 days free"),
                            onClick = onNavigateToTrial,
                            height = 50.dp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = t("Посмотреть тарифы", "View plans"),
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
                    text = t("Управление", "Management"),
                    color = Color(0xFF8BA3A4),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 22.dp, bottom = 10.dp, start = 2.dp)
                )

                ByMeCardGroup {
                    ByMeSettingRow(
                        title = t("Мои устройства", "My devices"),
                        subtitle = if (devices.isNotEmpty()) {
                            effectiveDevicesText
                        } else maxDevices?.let { t("0 из $it · нет списка устройств", "0 of $it · no device list") } ?: t("Лимит уточняется сервером", "Device limit pending"),
                        icon = { DevicesMultiIcon(color = Color(0xFF7AC9CB), size = 20.dp) },
                        trailing = { ChevronRightIcon(color = Color(0xFF71898A), size = 18.dp) },
                        onClick = onNavigateToDevices,
                        showBottomDivider = false
                    )
                }

                // Section: Аккаунт
                Text(
                    text = t("Аккаунт", "Account"),
                    color = Color(0xFF8BA3A4),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 22.dp, bottom = 10.dp, start = 2.dp)
                )

                ByMeCardGroup {
                    ByMeSettingRow(
                        title = t("Настройки аккаунта", "Account settings"),
                        subtitle = t("Профиль, устройства, подписка", "Profile, devices, subscription"),
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
                containerColor = AppColors.CardBg,
                titleContentColor = AppColors.TextPrimary,
                title = { Text(t("Настройки аккаунта", "Account settings")) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AccountInfoLine(t("Имя", "Name"), currentUser?.displayName.orEmpty().ifBlank { t("Не указано", "Not specified") })
                        AccountInfoLine(
                            "Email",
                            currentUser?.email?.takeIf { it.isNotBlank() && it != "user@bymevpn.app" }
                                ?: t("Не указан", "Not specified")
                        )
                        AccountInfoLine(t("ID аккаунта", "Account ID"), currentUser?.id ?: "—")
                        TextButton(
                            enabled = currentUser?.id?.isNotBlank() == true,
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("ByMeVPN account ID", currentUser?.id.orEmpty()))
                                Toast.makeText(context, t("ID скопирован", "ID copied"), Toast.LENGTH_SHORT).show()
                            }
                        ) { Text(t("Скопировать ID", "Copy ID"), color = AppColors.BrandMint) }

                        TextButton(
                            enabled = !refreshingAccount,
                            onClick = {
                                refreshingAccount = true
                                scope.launch {
                                    try {
                                        AccountRepository.refreshProfile(context)
                                        AccountRepository.refreshDevices(context)
                                        val result = subManager.refreshStatus()
                                        Toast.makeText(
                                            context,
                                            if (result.isSuccess) t("Данные обновлены", "Account refreshed")
                                            else t("Не удалось проверить подписку", "Could not refresh subscription"),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } finally {
                                        refreshingAccount = false
                                    }
                                }
                            }
                        ) {
                            Text(
                                if (refreshingAccount) t("Обновляем…", "Refreshing…")
                                else t("Обновить данные аккаунта", "Refresh account data"),
                                color = AppColors.BrandMint
                            )
                        }

                        AccountInfoLine(t("Устройства", "Devices"), effectiveDevicesText)
                        TextButton(onClick = {
                            showAccountDialog = false
                            onNavigateToDevices()
                        }) { Text(t("Управление устройствами", "Manage devices"), color = AppColors.BrandMint) }

                        AccountInfoLine(
                            t("Подписка", "Subscription"),
                            if (isActive) t("Активна", "Active") else t("Не активна", "Inactive")
                        )
                        AccountInfoLine(
                            t("Автопродление", "Auto-renewal"),
                            when (subscription?.autoRenew) {
                                true -> t("Включено", "Enabled")
                                false -> t("Отключено", "Disabled")
                                null -> t("Не подтверждено сервером", "Not confirmed by server")
                            }
                        )
                        TextButton(onClick = {
                            showAccountDialog = false
                            onNavigateToRenewal()
                        }) { Text(t("Подробнее об автопродлении", "Auto-renewal details"), color = AppColors.BrandMint) }

                        TextButton(onClick = {
                            showAccountDialog = false
                            onNavigateToPricing()
                        }) { Text(t("Тарифы и продление", "Plans and renewal"), color = AppColors.BrandMint) }

                        TextButton(onClick = {
                            showAccountDialog = false
                            onNavigateToSupport()
                        }) { Text(t("Связаться с поддержкой", "Contact support"), color = AppColors.BrandMint) }

                        TextButton(onClick = {
                            showAccountDialog = false
                            onNavigateToSettings()
                        }) { Text(t("Настройки приложения и язык", "App settings and language"), color = AppColors.BrandMint) }

                        TextButton(onClick = {
                            showAccountDialog = false
                            showDeleteDialog = true
                        }) { Text(t("Удаление аккаунта и данных", "Account and data deletion"), color = Color(0xFFFF8A8A)) }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showAccountDialog = false
                        onLogOut()
                    }) { Text(t("Выйти", "Sign out"), color = Color(0xFFFF7373)) }
                },
                dismissButton = {
                    TextButton(onClick = { showAccountDialog = false }) {
                        Text(t("Закрыть", "Close"), color = AppColors.BrandMint)
                    }
                }
            )
        }
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { if (!deletingAccount) showDeleteDialog = false },
                title = { Text(t("Удалить аккаунт навсегда?", "Delete account permanently?")) },
                text = { Text(t("Это действие удалит аккаунт на сервере и приведёт к потере доступа к подписке. Отменить удаление невозможно.", "This will delete your account and subscription access from the server. This cannot be undone.")) },
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
                                Toast.makeText(context, t("Удаление через приложение пока недоступно. Напишите в @ByMeVPNSupportBot.", "In-app deletion is not available yet. Contact @ByMeVPNSupportBot."), Toast.LENGTH_LONG).show()
                            }
                        }
                    }) { Text(t("Удалить навсегда", "Delete permanently"), color = Color(0xFFFF7373)) }
                },
                dismissButton = {
                    TextButton(enabled = !deletingAccount, onClick = { showDeleteDialog = false }) { Text(t("Отмена", "Cancel")) }
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
