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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.bymevpn.data.subscription.SubscriptionPresentationPolicy
import com.example.bymevpn.theme.AppColors
import kotlinx.coroutines.launch

@Composable
@OptIn(ExperimentalMaterial3Api::class)
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
    val isCheckingSubscription by subManager.isSyncing.collectAsState()
    val devices by AccountRepository.userDevices.collectAsState()
    val deviceListUnavailable by AccountRepository.deviceListUnavailable.collectAsState()

    val isActive = subscription?.let { it.isActive && !it.knownExpiryReached(System.currentTimeMillis()) } == true
    val subscriptionUnknown = SubscriptionPresentationPolicy.isUnknown(subscription)
    val remainingDays = subscription?.daysRemainingAt(System.currentTimeMillis())
    val maxDevices = subscription?.maxDevices?.takeIf { it > 0 }
    val activeDevicesCount = devices.count { !it.isCurrent } + 1
    val effectiveDevicesText = maxDevices?.let {
        t("Устройств: $activeDevicesCount из $it", "Devices: $activeDevicesCount of $it")
    } ?: t("Устройств сейчас: $activeDevicesCount", "Devices shown: $activeDevicesCount")

    var showAccountDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deletingAccount by remember { mutableStateOf(false) }
    var refreshingAccount by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val accountSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun refreshAccountData() {
        if (refreshingAccount) return
        refreshingAccount = true
        scope.launch {
            try {
                AccountRepository.refreshProfile(context)
                AccountRepository.refreshDevices(context)
                val result = subManager.refreshStatus(forceBackend = true)
                Toast.makeText(
                    context,
                    if (result.isSuccess) t("Данные обновлены", "Account refreshed")
                    else t("Не удалось обновить данные. Попробуйте позже.", "Could not refresh account. Try again later."),
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                refreshingAccount = false
            }
        }
    }

    LaunchedEffect(Unit) {
        AccountRepository.refreshProfile(context)
        AccountRepository.refreshDevices(context)
        subManager.refreshStatus(forceBackend = true)
    }

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
                            text = currentUser?.email?.takeIf { it.isNotBlank() && it != "user@bymevpn.app" } ?: t("Аккаунт ByMeVPN", "ByMeVPN account"),
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
                            text = when {
                                isActive -> t("Активен", "Active")
                                subscriptionUnknown -> t("Проверяем", "Checking")
                                else -> t("Без подписки", "No subscription")
                            },
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
                } else if (subscriptionUnknown) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(21.dp))
                            .background(Color(0xFF1A2D40))
                            .border(1.dp, Color(0xFF34566A), RoundedCornerShape(21.dp))
                            .padding(21.dp)
                    ) {
                        Text(
                            text = t("СТАТУС ДОСТУПА", "ACCESS STATUS"),
                            color = Color(0xFF8FBBCA),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isCheckingSubscription || refreshingAccount) {
                                t("Проверяем подписку", "Checking subscription")
                            } else {
                                t("Не удалось проверить подписку", "Could not verify subscription")
                            },
                            color = AppColors.TextPrimary,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = t(
                                "Доступ пока не подтверждён. Обновите данные аккаунта; пробный период и подписка не менялись.",
                                "Access is not confirmed yet. Refresh your account; your trial and subscription have not been changed."
                            ),
                            color = Color(0xFF96B0A6),
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        ByMePrimaryButton(
                            text = if (isCheckingSubscription || refreshingAccount) {
                                t("Проверяем…", "Checking…")
                            } else {
                                t("Повторить проверку", "Retry status check")
                            },
                            onClick = { refreshAccountData() },
                            enabled = !isCheckingSubscription && !refreshingAccount,
                            height = 50.dp,
                            showArrow = false
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
                        subtitle = if (deviceListUnavailable) {
                            t("Не удалось загрузить список устройств", "Could not load your devices")
                        } else if (devices.isNotEmpty()) {
                            effectiveDevicesText
                        } else maxDevices?.let { t("0 из $it · нет списка устройств", "0 of $it · no device list") } ?: t("Сервер не передал лимит тарифа", "Server did not provide the plan limit"),
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
            ModalBottomSheet(
                onDismissRequest = { showAccountDialog = false },
                sheetState = accountSheetState,
                containerColor = AppColors.BgCanvas,
                contentColor = AppColors.TextPrimary
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 680.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 22.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(t("Настройки аккаунта", "Account settings"), fontSize = 23.sp, fontWeight = FontWeight.Bold)
                        Text(
                            t("Профиль, подписка и управление доступом", "Profile, subscription and access"),
                            color = AppColors.TextSecondary,
                            fontSize = 13.sp
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(AppColors.CardBg)
                            .border(1.dp, AppColors.CardBorder, RoundedCornerShape(18.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(t("ПРОФИЛЬ", "PROFILE"), color = AppColors.BrandMint, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        AccountInfoLine(t("Имя", "Name"), currentUser?.displayName.orEmpty().ifBlank { t("Не указано", "Not set") })
                        AccountInfoLine(
                            "Email",
                            currentUser?.email?.takeIf { it.isNotBlank() && it != "user@bymevpn.app" }
                                ?: t("Не указан", "Not set")
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = t("ID аккаунта", "Account ID"),
                                color = AppColors.TextSecondary,
                                fontSize = 12.5.sp,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                enabled = currentUser?.id?.isNotBlank() == true,
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("ByMeVPN account ID", currentUser?.id.orEmpty()))
                                    Toast.makeText(context, t("ID скопирован", "ID copied"), Toast.LENGTH_SHORT).show()
                                }
                            ) { Text(t("Скопировать", "Copy"), color = AppColors.BrandMint) }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(AppColors.CardBg)
                            .border(1.dp, AppColors.CardBorder, RoundedCornerShape(18.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(t("ПОДПИСКА", "SUBSCRIPTION"), color = AppColors.BrandMint, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        AccountInfoLine(t("Статус", "Status"), if (isActive) t("Активна", "Active") else t("Не активна", "Inactive"))
                        AccountInfoLine(t("Осталось", "Remaining"), remainingDays?.let { t("$it дн.", "$it days") } ?: "—")
                        AccountInfoLine(t("Тариф", "Plan"), subscription?.planName?.takeIf { it.isNotBlank() } ?: t("Не выбран", "Not selected"))
                        AccountInfoLine(t("Устройства", "Devices"), effectiveDevicesText)
                        subscription?.autoRenew?.let { enabled ->
                            AccountInfoLine(t("Автопродление", "Auto-renewal"), if (enabled) t("Включено", "On") else t("Отключено", "Off"))
                        }
                    }

                    ByMeCardGroup {
                        ByMeSettingRow(
                            title = t("Тарифы и оплата", "Plans and payment"),
                            subtitle = t("Выбрать или продлить тариф", "Choose or renew a plan"),
                            icon = { RefreshIcon(color = Color(0xFF7AC9CB), size = 20.dp) },
                            trailing = { ChevronRightIcon(color = Color(0xFF71898A), size = 18.dp) },
                            onClick = { showAccountDialog = false; onNavigateToPricing() }
                        )
                        ByMeSettingRow(
                            title = t("Автопродление", "Auto-renewal"),
                            subtitle = when (subscription?.autoRenew) {
                                true -> t("Включено", "On")
                                false -> t("Отключено", "Off")
                                null -> t("Управление продлением подписки", "Manage subscription renewal")
                            },
                            icon = { RefreshIcon(color = Color(0xFF7AC9CB), size = 20.dp) },
                            trailing = { ChevronRightIcon(color = Color(0xFF71898A), size = 18.dp) },
                            onClick = { showAccountDialog = false; onNavigateToRenewal() }
                        )
                        ByMeSettingRow(
                            title = t("Помощь и поддержка", "Help and support"),
                            subtitle = t("Связаться с нами", "Contact our team"),
                            icon = { QuestionHelpIcon(color = Color(0xFF7AC9CB), size = 20.dp) },
                            trailing = { ChevronRightIcon(color = Color(0xFF71898A), size = 18.dp) },
                            onClick = { showAccountDialog = false; onNavigateToSupport() },
                            showBottomDivider = false
                        )
                    }

                    ByMeSecondaryButton(
                        text = if (refreshingAccount) t("Обновляем…", "Refreshing…") else t("Обновить данные", "Refresh account"),
                        onClick = { refreshAccountData() },
                        enabled = !refreshingAccount
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TextButton(
                            onClick = { showAccountDialog = false; onLogOut() },
                            modifier = Modifier.weight(1f)
                        ) { Text(t("Выйти", "Sign out"), color = Color(0xFFE98B8B)) }
                        TextButton(
                            onClick = { showAccountDialog = false; showDeleteDialog = true },
                            modifier = Modifier.weight(1f)
                        ) { Text(t("Удалить аккаунт", "Delete account"), color = Color(0xFFE98B8B)) }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
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
                                val apiError = result.exceptionOrNull() as? com.example.bymevpn.data.api.ApiError
                                val message = when {
                                    apiError?.httpStatusCode == 501 -> t("Удаление пока недоступно. Обратитесь в поддержку.", "Account deletion is not available yet. Contact support.")
                                    apiError?.httpStatusCode == 401 -> t("Войдите в аккаунт заново и повторите попытку.", "Sign in again and try again.")
                                    else -> t("Не удалось удалить аккаунт. Проверьте соединение и попробуйте позже.", "Could not delete the account. Check your connection and try again later.")
                                }
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
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
