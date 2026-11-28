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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
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
import com.example.bymevpn.BuildConfig
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
import com.example.bymevpn.data.AccountRepository
import com.example.bymevpn.data.api.ByMeApiClient
import com.example.bymevpn.data.LocaleManager
import com.example.bymevpn.data.subscription.SubscriptionManager
import com.example.bymevpn.data.subscription.SubscriptionPresentationPolicy
import com.example.bymevpn.data.subscription.TrialActivationPolicy
import com.example.bymevpn.theme.AppColors
import com.example.bymevpn.theme.AppGradients
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope

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
    val lastSubscriptionRefreshError by subManager.lastRefreshError.collectAsState()
    val devices by AccountRepository.userDevices.collectAsState()
    val deviceListUnavailable by AccountRepository.deviceListUnavailable.collectAsState()

    val nowMs = remember(subscription) { System.currentTimeMillis() }
    val isActive = subscription?.let { it.isActive && !it.knownExpiryReached(nowMs) } == true
    val subscriptionUnknown = SubscriptionPresentationPolicy.isUnknown(subscription)
    val accessConfirmationRequired = SubscriptionPresentationPolicy.requiresAccessConfirmation(
        subscription,
        subManager.canConnect()
    )
    val remainingDays = subscription?.daysRemainingAt(nowMs)
    val maxDevices = subscription?.maxDevices?.takeIf { it > 0 }
    val activeDevicesCount = devices.size
    val effectiveDevicesText = maxDevices?.let {
        t("Устройств: $activeDevicesCount из $it", "Devices: $activeDevicesCount of $it")
    } ?: t("Устройств сейчас: $activeDevicesCount", "Devices shown: $activeDevicesCount")

    var showAccountDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var deletingAccount by remember { mutableStateOf(false) }
    var refreshingAccount by remember { mutableStateOf(false) }
    var resettingPassword by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val accountSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dismissAccountSheet: (() -> Unit) -> Unit = { afterDismiss ->
        scope.launch {
            accountSheetState.hide()
            showAccountDialog = false
            afterDismiss()
        }
    }

    fun refreshAccountData() {
        if (refreshingAccount) return
        refreshingAccount = true
        scope.launch {
            try {
                val result = coroutineScope {
                    val profileRefresh = launch { AccountRepository.refreshProfile(context) }
                    val devicesRefresh = launch { AccountRepository.refreshDevices(context) }
                    val statusResult = subManager.refreshStatus(forceBackend = true)
                    if (statusResult.getOrNull()?.isUsable == true) {
                        runCatching {
                            com.example.bymevpn.vpn.VpnManager.getInstance(context).loadServers()
                        }
                    }
                    profileRefresh.join()
                    devicesRefresh.join()
                    statusResult
                }
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
        coroutineScope {
            val profileRefresh = launch { AccountRepository.refreshProfile(context) }
            val devicesRefresh = launch { AccountRepository.refreshDevices(context) }
            val status = subManager.refreshStatus(forceBackend = true).getOrNull()
            if (status?.isUsable == true) {
                runCatching {
                    com.example.bymevpn.vpn.VpnManager.getInstance(context).loadServers()
                }
            }
            profileRefresh.join()
            devicesRefresh.join()
        }
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
                    .padding(horizontal = 20.dp)
            ) {
                // Profile Minimal Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(AppColors.BgSurfaceAccent)
                            .border(1.dp, AppColors.BorderAccent, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        PersonProfileIcon(color = AppColors.SkyBlue, size = 24.dp)
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
                            color = AppColors.TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isActive) AppColors.BadgeGreenBg else AppColors.BadgeRedBg)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = when {
                                accessConfirmationRequired -> t("Проверка доступа", "Checking access")
                                isActive -> t("Активен", "Active")
                                subscriptionUnknown -> t("Проверяем", "Checking")
                                else -> t("Без подписки", "No subscription")
                            },
                            color = if (isActive && !accessConfirmationRequired) AppColors.BadgeGreenText else AppColors.BadgeRedText,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Plan Card: Active vs Checking vs Inactive
                if (isActive && !accessConfirmationRequired) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(AppGradients.ActivePlanCard)
                            .border(1.dp, AppColors.BorderAccent, RoundedCornerShape(20.dp))
                            .padding(20.dp)
                    ) {
                        Text(
                            text = t("Подписка ByMeVPN", "ByMeVPN subscription"),
                            color = AppColors.SkyBlue,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row {
                            Text(
                                text = remainingDays?.toString() ?: "—",
                                color = AppColors.TextPrimary,
                                fontSize = 54.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-2).sp,
                                modifier = Modifier.alignByBaseline()
                            )
                            if (remainingDays != null) {
                                Text(
                                    text = t(" дн.", " days"),
                                    color = AppColors.BrandMint,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.alignByBaseline().padding(start = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = if (remainingDays == null) t("Срок доступа не подтверждён", "Access expiry not confirmed") else t("до окончания доступа", "until access expires"),
                            color = AppColors.TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(AppColors.BorderDefault))
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = t("Ваш тариф", "Your plan"),
                                color = AppColors.TextSecondary,
                                fontSize = 12.sp
                            )
                            Text(
                                text = subscription?.planName?.takeIf { it.isNotBlank() } ?: t("Не указан", "Not specified"),
                                color = AppColors.TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        if (BuildConfig.EXTERNAL_CHECKOUT_ENABLED) {
                            ByMeSecondaryButton(
                                text = t("Продлить подписку", "Renew subscription"),
                                onClick = onNavigateToPricing
                            )
                        } else {
                            ByMeSecondaryButton(
                                text = if (refreshingAccount) t("Обновляем…", "Refreshing…") else t("Обновить статус", "Refresh status"),
                                onClick = { refreshAccountData() },
                                enabled = !refreshingAccount
                            )
                        }
                    }
                } else if (subscriptionUnknown || accessConfirmationRequired) {
                    SubscriptionRecoveryCard(
                        isRu = isRu,
                        isChecking = isCheckingSubscription || refreshingAccount,
                        accessConfirmationRequired = accessConfirmationRequired,
                        status = subscription,
                        lastError = lastSubscriptionRefreshError,
                        onRetry = { refreshAccountData() },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    val offerTrial = TrialActivationPolicy.shouldOfferTrial(subscription)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(AppGradients.AccentCard)
                            .border(1.dp, AppColors.BorderAccent, RoundedCornerShape(20.dp))
                            .padding(20.dp)
                    ) {
                        Text(
                            text = if (subscription?.trialUnavailableReason == "trial_already_used")
                                t("Пробный период использован", "Free trial already used")
                            else t("Подписка не активна", "Subscription inactive"),
                            color = AppColors.TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = when (subscription?.trialUnavailableReason) {
                                "trial_activation_pending" -> t("Активация проверяется. Обновите данные позже.", "Activation is being checked. Refresh your account later.")
                                "eligibility_check_unavailable", "device_confirmation_required", "subscription_check_unavailable" -> t("Не удалось проверить доступность пробного периода.", "Could not check trial availability.")
                                else -> if (BuildConfig.EXTERNAL_CHECKOUT_ENABLED) {
                                    t("Выберите тариф, чтобы подключиться к ByMeVPN.", "Choose a plan to connect to ByMeVPN.")
                                } else {
                                    t("Активной подписки пока нет. Активируйте пробный период на главной или обновите статус.", "No active subscription yet. Activate your trial on Home or refresh status.")
                                }
                            },
                            color = AppColors.TextSecondary,
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        when {
                            offerTrial -> ByMePrimaryButton(
                                text = t("Активировать на главной", "Activate on Home"),
                                onClick = onNavigateToHome,
                                height = 50.dp
                            )
                            BuildConfig.EXTERNAL_CHECKOUT_ENABLED -> ByMePrimaryButton(
                                text = t("Посмотреть тарифы", "View plans"),
                                onClick = onNavigateToPricing,
                                height = 50.dp
                            )
                            else -> ByMePrimaryButton(
                                text = if (refreshingAccount) t("Обновляем…", "Refreshing…") else t("Обновить статус", "Refresh status"),
                                onClick = { refreshAccountData() },
                                enabled = !refreshingAccount,
                                height = 50.dp,
                                showArrow = false
                            )
                        }

                        if (offerTrial && BuildConfig.EXTERNAL_CHECKOUT_ENABLED) Text(
                            text = t("Посмотреть тарифы", "View plans"),
                            color = AppColors.SkyBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onNavigateToPricing)
                                .padding(top = 14.dp, bottom = 4.dp)
                        )
                    }
                }

                // Section: Управление
                Text(
                    text = t("Управление", "Management"),
                    color = AppColors.TextKicker,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
                )

                ByMeCardGroup {
                    ByMeSettingRow(
                        title = t("Мои устройства", "My devices"),
                        subtitle = if (deviceListUnavailable) {
                            t("Список устройств недоступен", "Device list unavailable")
                        } else if (devices.isNotEmpty()) {
                            effectiveDevicesText
                        } else maxDevices?.let { t("0 из $it", "0 of $it") } ?: t("Лимит уточняется", "Checking limit"),
                        icon = { DevicesMultiIcon(color = AppColors.SkyBlue, size = 20.dp) },
                        trailing = { ChevronRightIcon(color = AppColors.TextMuted, size = 18.dp) },
                        onClick = onNavigateToDevices,
                        showBottomDivider = BuildConfig.EXTERNAL_CHECKOUT_ENABLED
                    )
                    if (BuildConfig.EXTERNAL_CHECKOUT_ENABLED) {
                        ByMeSettingRow(
                            title = t("Тарифы и оплата", "Plans and payment"),
                            subtitle = t("Выбрать или продлить тариф", "Choose or renew a plan"),
                            icon = { RefreshIcon(color = AppColors.SkyBlue, size = 20.dp) },
                            trailing = { ChevronRightIcon(color = AppColors.TextMuted, size = 18.dp) },
                            onClick = onNavigateToPricing
                        )
                        ByMeSettingRow(
                            title = t("Автопродление", "Auto-renewal"),
                            subtitle = t("Статус и управление", "Status and control"),
                            icon = { RefreshIcon(color = AppColors.SkyBlue, size = 20.dp) },
                            trailing = { ChevronRightIcon(color = AppColors.TextMuted, size = 18.dp) },
                            onClick = onNavigateToRenewal,
                            showBottomDivider = false
                        )
                    }
                }

                // Section: Аккаунт
                Text(
                    text = t("Аккаунт", "Account"),
                    color = AppColors.TextKicker,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
                )

                ByMeCardGroup {
                    ByMeSettingRow(
                        title = t("Аккаунт и безопасность", "Account and security"),
                        subtitle = t("Профиль, выход и удаление", "Profile, sign out and deletion"),
                        icon = { PersonProfileIcon(color = AppColors.SkyBlue, size = 20.dp) },
                        trailing = { ChevronRightIcon(color = AppColors.TextMuted, size = 18.dp) },
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
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(t("Аккаунт и безопасность", "Account and security"), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text(
                            t("Профиль и управление доступом", "Profile details and account control"),
                            color = AppColors.TextSecondary,
                            fontSize = 13.sp
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(AppColors.CardBg)
                            .border(1.dp, AppColors.CardBorder, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(t("Профиль", "Profile"), color = AppColors.SkyBlue, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                        AccountInfoLine(t("Имя", "Name"), currentUser?.displayName.orEmpty().ifBlank { t("Не указано", "Not set") })
                        AccountInfoLine(
                            "Email",
                            currentUser?.email?.takeIf { it.isNotBlank() && it != "user@bymevpn.app" }
                                ?: t("Не указан", "Not set")
                        )
                    }

                    ByMeCardGroup {
                        if (currentUser?.isGoogle == false && currentUser?.email?.contains("@") == true && currentUser?.email != "user@bymevpn.app") {
                            ByMeSettingRow(
                                title = t("Сменить пароль", "Change password"),
                                subtitle = t("Получить ссылку на почту", "Receive a reset link by email"),
                                icon = { PersonProfileIcon(color = AppColors.SkyBlue, size = 20.dp) },
                                trailing = { ChevronRightIcon(color = AppColors.TextMuted, size = 18.dp) },
                                onClick = {
                                    if (!resettingPassword) {
                                        val email = currentUser?.email.orEmpty()
                                        resettingPassword = true
                                        scope.launch {
                                            try {
                                                ByMeApiClient(context).requestPasswordReset(email)
                                                Toast.makeText(context, t("Если пароль доступен для этого аккаунта, ссылка придёт на почту.", "If password reset is available, a link will be sent to your email."), Toast.LENGTH_LONG).show()
                                            } catch (e: kotlinx.coroutines.CancellationException) {
                                                throw e
                                            } catch (e: Exception) {
                                                Toast.makeText(context, t("Не удалось отправить запрос. Попробуйте позже.", "Could not send the request. Try again later."), Toast.LENGTH_LONG).show()
                                            } finally {
                                                resettingPassword = false
                                            }
                                        }
                                    }
                                }
                            )
                        }
                        ByMeSettingRow(
                            title = t("Помощь и поддержка", "Help and support"),
                            subtitle = t("Связаться с нами", "Contact our team"),
                            icon = { QuestionHelpIcon(color = AppColors.SkyBlue, size = 20.dp) },
                            trailing = { ChevronRightIcon(color = AppColors.TextMuted, size = 18.dp) },
                            onClick = { dismissAccountSheet(onNavigateToSupport) },
                            showBottomDivider = false
                        )
                    }

                    ByMeSecondaryButton(
                        text = if (refreshingAccount) t("Обновляем…", "Refreshing…") else t("Обновить данные", "Refresh account"),
                        onClick = { refreshAccountData() },
                        enabled = !refreshingAccount
                    )

                    TextButton(onClick = { dismissAccountSheet(onLogOut) }) {
                        Text(t("Выйти", "Sign out"), color = AppColors.StatusError)
                    }
                    TextButton(onClick = { showDeleteAccountDialog = true }) {
                        Text(t("Удалить аккаунт", "Delete account"), color = AppColors.StatusError)
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
        if (showDeleteAccountDialog) {
            AlertDialog(
                onDismissRequest = { if (!deletingAccount) showDeleteAccountDialog = false },
                containerColor = AppColors.BgSurfaceElevated,
                titleContentColor = AppColors.TextPrimary,
                textContentColor = AppColors.TextSecondary,
                title = { Text(t("Удалить аккаунт?", "Delete account?")) },
                text = { Text(t(
                    "Аккаунт и доступ к VPN будут удалены. Оставшийся оплаченный срок не сохраняется. Это действие нельзя отменить.",
                    "Your account and VPN access will be deleted. Remaining paid time will be lost. This cannot be undone."
                )) },
                confirmButton = {
                    TextButton(enabled = !deletingAccount, onClick = {
                        deletingAccount = true
                        scope.launch {
                            val result = AccountRepository.deleteAccount(context)
                            deletingAccount = false
                            if (result.isSuccess) {
                                showDeleteAccountDialog = false
                                dismissAccountSheet {}
                            } else {
                                val status = (result.exceptionOrNull() as? com.example.bymevpn.data.api.ApiError)?.httpStatusCode
                                val message = when (status) {
                                    403 -> t("Удаление этого аккаунта в приложении недоступно. Обратитесь в поддержку.",
                                        "This account cannot be deleted in the app. Contact support.")
                                    409 -> t("Операция с подпиской ещё выполняется. Попробуйте позже.",
                                        "A subscription operation is still in progress. Try again later.")
                                    else -> t("Не удалось подтвердить удаление. Попробуйте войти снова или обратитесь в поддержку.",
                                        "Could not confirm deletion. Try signing in again or contact support.")
                                }
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }) { Text(t("Удалить", "Delete"), color = AppColors.StatusError) }
                },
                dismissButton = {
                    TextButton(enabled = !deletingAccount, onClick = { showDeleteAccountDialog = false }) {
                        Text(t("Отмена", "Cancel"), color = AppColors.TextSecondary)
                    }
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
        Text(label, color = AppColors.TextSecondary, fontSize = 12.5.sp)
        Text(
            text = value,
            color = AppColors.TextPrimary,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        )
    }
}
