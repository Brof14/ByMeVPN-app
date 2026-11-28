package com.example.bymevpn.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.bymevpn.BuildConfig
import com.example.bymevpn.components.BottomNavTab
import com.example.bymevpn.components.ByMeBottomNav
import com.example.bymevpn.components.ByMePrimaryButton
import com.example.bymevpn.components.ByMeTopBar
import com.example.bymevpn.components.CheckmarkIcon
import com.example.bymevpn.components.ChevronRightIcon
import com.example.bymevpn.components.ClockVectorIcon
import com.example.bymevpn.components.CountryFlagView
import com.example.bymevpn.components.FaqDialog
import com.example.bymevpn.components.PrivacyPolicyDialog
import com.example.bymevpn.components.QuestionHelpIcon
import com.example.bymevpn.components.RefreshIcon
import com.example.bymevpn.components.ServerSelectionSheet
import com.example.bymevpn.components.ShieldLogo
import com.example.bymevpn.components.TrialActivatedDialog
import com.example.bymevpn.components.VpnDisclosureDialog
import com.example.bymevpn.components.VpnPowerButton
import com.example.bymevpn.data.AccountRepository
import com.example.bymevpn.data.LocaleManager
import com.example.bymevpn.data.api.ByMeApiClient
import com.example.bymevpn.data.api.ServerNode
import com.example.bymevpn.data.storage.SecureTokenStorage
import com.example.bymevpn.data.subscription.SubscriptionManager
import com.example.bymevpn.data.subscription.SubscriptionPresentationPolicy
import com.example.bymevpn.data.subscription.TrialActivationPolicy
import com.example.bymevpn.data.update.AppUpdateInfo
import com.example.bymevpn.data.update.AppUpdateManager
import com.example.bymevpn.theme.AppColors
import com.example.bymevpn.theme.AppGradients
import com.example.bymevpn.vpn.VpnConnectionState
import com.example.bymevpn.vpn.VpnManager
import com.example.bymevpn.vpn.xray.XrayVpnEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun HomeScreen(
    onNavigateToAccount: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToTrial: () -> Unit = {},
    onNavigateToPricing: () -> Unit = {},
    onNavigateToSupport: () -> Unit = {},
    onRestoreViaTelegram: () -> Unit = {},
    onLogOut: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isRu = homeIsRussian()
    val scope = rememberCoroutineScope()

    val vpnManager = remember { VpnManager.getInstance(context) }
    val vpnState by vpnManager.state.collectAsState()
    val currentServer by vpnManager.currentServer.collectAsState()
    val availableServers by vpnManager.availableServers.collectAsState()
    val serversError by vpnManager.serversError.collectAsState()
    val isLoadingServers by vpnManager.isLoadingServers.collectAsState()
    val isAutoServerSelected by vpnManager.isAutomaticServerSelected.collectAsState()

    val subManager = remember { SubscriptionManager.getInstance(context) }
    val subscription by subManager.subscriptionStatus.collectAsState()
    val isCheckingSubscription by subManager.isSyncing.collectAsState()
    val lastSubscriptionRefreshError by subManager.lastRefreshError.collectAsState()
    val currentUser by AccountRepository.currentUser.collectAsState()
    val subscriptionNowMs = remember(subscription) { System.currentTimeMillis() }
    val subscriptionUnknown = SubscriptionPresentationPolicy.isUnknown(subscription)
    val hasPlan = subscription?.let { it.isActive && !it.knownExpiryReached(subscriptionNowMs) } == true
    val isTrialPlan = subscription?.status == "trial"
    val accessConfirmationRequired = SubscriptionPresentationPolicy.requiresAccessConfirmation(
        subscription,
        subManager.canConnect()
    )
    val offerTelegramRecovery = subscription?.status == "none" &&
        (currentUser?.isGoogle == true || !currentUser?.email.isNullOrBlank())

    val remainingDays = subscription?.daysRemainingAt(subscriptionNowMs)
    val vpnError by vpnManager.errorMessage.collectAsState()

    LaunchedEffect(currentUser?.id) {
        if (subscription?.isUsable != true || !subManager.canConnect()) {
            subManager.refreshStatus(forceBackend = true)
        }
    }

    var showServerSheet by remember { mutableStateOf(false) }
    var showFaqDialog by remember { mutableStateOf(false) }
    val secureStorage = remember { SecureTokenStorage.getInstance(context) }
    var isActivatingTrial by remember { mutableStateOf(false) }
    var isPreparingTrialServers by remember { mutableStateOf(false) }
    var trialActivationError by remember { mutableStateOf<String?>(null) }
    var showTrialSuccessDialog by remember { mutableStateOf(false) }
    var trialServersReady by remember { mutableStateOf(false) }
    var vpnSuccessPulseKey by remember { mutableIntStateOf(0) }
    var previousVpnState by remember { mutableStateOf(vpnState) }
    var showVpnDisclosure by remember { mutableStateOf(false) }
    var showPrivacyPolicy by remember { mutableStateOf(false) }
    var notificationPermissionPrompted by remember {
        mutableStateOf(secureStorage.isNotificationPermissionPrompted())
    }
    val updateInfo by AppUpdateManager.updateInfo.collectAsState()
    var isInstallingUpdate by remember { mutableStateOf(false) }

    LaunchedEffect(vpnState) {
        if (vpnState == VpnConnectionState.CONNECTED && previousVpnState != VpnConnectionState.CONNECTED) {
            vpnSuccessPulseKey++
        }
        previousVpnState = vpnState
    }

    // Fetch stale/missing plan prices only when external checkout is enabled.
    LaunchedEffect(Unit) {
        if (BuildConfig.EXTERNAL_CHECKOUT_ENABLED) {
            runCatching { ByMeApiClient(context).warmPlanCatalog() }
        }
    }

    // Initialize the native core while the home screen is idle so a cold first
    // connection does not pay the asset-copy and library setup cost after the tap.
    LaunchedEffect(hasPlan) {
        if (hasPlan) {
            withContext(Dispatchers.IO) {
                runCatching { XrayVpnEngine.getInstance(context).initCoreEnv() }
            }
        }
    }

    val startUpdateInstall: (AppUpdateInfo) -> Unit = { info ->
        scope.launch {
            isInstallingUpdate = true
            try {
                AppUpdateManager.downloadAndInstall(context, info)
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    if (isRu) "Не удалось установить обновление. Попробуйте позже." else "Could not install the update. Please try again.",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                isInstallingUpdate = false
            }
        }
    }
    val installPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        val permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()
        val pendingUpdate = updateInfo
        if (permissionGranted && pendingUpdate != null) {
            startUpdateInstall(pendingUpdate)
        } else if (pendingUpdate != null) {
            Toast.makeText(
                context,
                if (isRu) "Разрешите установку обновлений для ByMeVPN." else "Allow ByMeVPN to install updates.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    LaunchedEffect(subscription?.status) {
        if (subscription?.isUndetermined != true) return@LaunchedEffect
        for (backoffMs in listOf(1_500L, 3_500L, 7_500L)) {
            delay(backoffMs)
            val fresh = subManager.refreshStatus(forceBackend = true).getOrNull()
            if (fresh != null && !fresh.isUndetermined) {
                if (fresh.isUsable) vpnManager.loadServers()
                break
            }
        }
    }

    LaunchedEffect(hasPlan, availableServers.isEmpty()) {
        if (!hasPlan || availableServers.isNotEmpty()) return@LaunchedEffect
        var attempt = 0
        while (attempt < 3 && vpnManager.availableServers.value.isEmpty()) {
            vpnManager.loadServers()
            if (vpnManager.availableServers.value.isNotEmpty()) break
            attempt++
            if (attempt < 3) delay(if (attempt == 1) 8_000L else 30_000L)
        }
    }

    fun refreshServers() {
        scope.launch { vpnManager.loadServers() }
    }

    fun activateTrialFromHome() {
        if (isActivatingTrial || !TrialActivationPolicy.shouldOfferTrial(subscription)) return
        isActivatingTrial = true
        trialActivationError = null
        scope.launch {
            try {
                val fresh = subManager.refreshStatus(forceBackend = true).getOrThrow()
                if (!TrialActivationPolicy.shouldOfferTrial(fresh)) return@launch
                val result = subManager.activateTrial()
                result.onSuccess {
                    isPreparingTrialServers = true
                    var loaded = false
                    for (attempt in 0..1) {
                        val serverResult = runCatching { vpnManager.loadServers() }.getOrNull()
                        if (serverResult?.getOrNull()?.isNotEmpty() == true) {
                            loaded = true
                            break
                        }
                        if (attempt == 0) delay(750)
                    }
                    trialServersReady = loaded
                    showTrialSuccessDialog = true
                }.onFailure { error ->
                    trialActivationError = com.example.bymevpn.data.api.AccountActionErrorMessages.activateTrial(error, isRu)
                }
            } catch (error: Exception) {
                trialActivationError = com.example.bymevpn.data.api.AccountActionErrorMessages.activateTrial(error, isRu)
            } finally {
                isPreparingTrialServers = false
                isActivatingTrial = false
            }
        }
    }

    val vpnPrepareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            VpnManager.startVpn(context)
        } else {
            Toast.makeText(
                context,
                if (isRu) "Требуется разрешение для создания VPN-туннеля" else "VPN permission is required",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun launchVpnPrepareOrStart() {
        val prepareIntent = VpnService.prepare(context)
        if (prepareIntent != null) {
            vpnPrepareLauncher.launch(prepareIntent)
        } else {
            VpnManager.startVpn(context)
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        launchVpnPrepareOrStart()
    }

    fun prepareAndStartVpn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !notificationPermissionPrompted &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionPrompted = true
            secureStorage.setNotificationPermissionPrompted(true)
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            launchVpnPrepareOrStart()
        }
    }

    fun toggleVpn() {
        if (!hasPlan) {
            onNavigateToTrial()
            return
        }

        when (vpnState) {
            VpnConnectionState.CONNECTED, VpnConnectionState.CONNECTING -> {
                VpnManager.stopVpn(context)
            }
            else -> {
                if (!secureStorage.isVpnDisclosureAccepted()) showVpnDisclosure = true
                else prepareAndStartVpn()
            }
        }
    }

    if (showVpnDisclosure) {
        VpnDisclosureDialog(
            isRussian = isRu,
            onAccept = {
                secureStorage.setVpnDisclosureAccepted(true)
                showVpnDisclosure = false
                prepareAndStartVpn()
            },
            onDecline = { showVpnDisclosure = false },
            onOpenPrivacyPolicy = { showPrivacyPolicy = true }
        )
    }
    if (showPrivacyPolicy) {
        PrivacyPolicyDialog(isRussian = isRu, onDismiss = { showPrivacyPolicy = false })
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.BgCanvas)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ByMeTopBar(
                title = null,
                onHelpClick = { showFaqDialog = true },
                onMoreClick = null
            )

            if (BuildConfig.EXTERNAL_CHECKOUT_ENABLED && updateInfo != null) {
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(AppColors.BgSurfaceElevated)
                            .border(1.dp, AppColors.BorderActiveGreen, RoundedCornerShape(16.dp))
                            .clickable(enabled = !isInstallingUpdate) {
                                val info = updateInfo ?: return@clickable
                                val needsInstallPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                                    !context.packageManager.canRequestPackageInstalls()
                                if (needsInstallPermission) {
                                    installPermissionLauncher.launch(
                                        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                        }
                                    )
                                } else {
                                    startUpdateInstall(info)
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isRu) "Доступно обновление v${updateInfo!!.latestVersion}" else "Update v${updateInfo!!.latestVersion} available",
                                color = AppColors.BrandMint,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = when {
                                    isInstallingUpdate -> if (isRu) "Загружаем обновление…" else "Downloading update…"
                                    else -> if (isRu) "Нажмите для установки" else "Tap to download and install"
                                },
                                color = AppColors.TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Text(
                            text = when {
                                isInstallingUpdate -> if (isRu) "Загрузка…" else "Downloading…"
                                else -> if (isRu) "Обновить >" else "Update >"
                            },
                            color = AppColors.SkyBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (!hasPlan || isActivatingTrial || subscriptionUnknown || accessConfirmationRequired) {
                    HomeNoPlanContent(
                        onTrialClick = { activateTrialFromHome() },
                        onChoosePlanClick = onNavigateToPricing,
                        onRestoreViaTelegram = onRestoreViaTelegram,
                        onRetrySubscription = {
                            scope.launch {
                                val refreshed = subManager.refreshStatus(forceBackend = true).getOrNull()
                                if (refreshed?.isUsable == true) vpnManager.loadServers()
                            }
                        },
                        subscriptionUnknown = subscriptionUnknown,
                        accessConfirmationRequired = accessConfirmationRequired,
                        subscriptionStatus = subscription,
                        lastRefreshError = lastSubscriptionRefreshError,
                        offerTelegramRecovery = offerTelegramRecovery,
                        isCheckingSubscription = isCheckingSubscription,
                        trialAvailable = isActivatingTrial || TrialActivationPolicy.shouldOfferTrial(subscription),
                        trialUnavailableReason = subscription?.trialUnavailableReason,
                        isActivatingTrial = isActivatingTrial,
                        isPreparingTrialServers = isPreparingTrialServers,
                        trialError = trialActivationError
                    )
                    if (BuildConfig.EXTERNAL_CHECKOUT_ENABLED && !subscriptionUnknown && !accessConfirmationRequired) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .clickable(onClick = onNavigateToPricing)
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isRu) "Посмотреть все тарифы" else "See all plans",
                                color = AppColors.SkyBlue,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            ChevronRightIcon(color = AppColors.SkyBlue, size = 16.dp)
                        }
                    }
                } else if (vpnState == VpnConnectionState.ERROR) {
                    HomeConnectionErrorContent(
                        onRetry = { toggleVpn() },
                        onSupportClick = onNavigateToSupport,
                        onServerSelectClick = { showServerSheet = true },
                        currentServer = currentServer,
                        errorMessage = vpnError
                    )
                } else if (vpnState == VpnConnectionState.CONNECTED) {
                    HomeConnectedContent(
                        vpnManager = vpnManager,
                        onPowerClick = { toggleVpn() },
                        onServerSelectClick = { showServerSheet = true },
                        onSubscriptionClick = onNavigateToAccount,
                        currentServer = currentServer,
                        remainingDays = remainingDays,
                        isTrial = isTrialPlan,
                        serversError = serversError,
                        isLoadingServers = isLoadingServers,
                        onRefreshServers = { refreshServers() },
                        successPulseKey = vpnSuccessPulseKey
                    )
                } else {
                    HomeDisconnectedContent(
                        isConnecting = vpnState == VpnConnectionState.CONNECTING,
                        connectingHint = if (vpnState == VpnConnectionState.CONNECTING) vpnError else null,
                        onPowerClick = { toggleVpn() },
                        onServerSelectClick = { showServerSheet = true },
                        onSubscriptionClick = onNavigateToAccount,
                        currentServer = currentServer,
                        remainingDays = remainingDays,
                        isTrial = isTrialPlan,
                        serversError = serversError,
                        isLoadingServers = isLoadingServers,
                        onRefreshServers = { refreshServers() }
                    )
                }
            }

            ByMeBottomNav(
                currentTab = BottomNavTab.Home,
                onTabSelected = { tab ->
                    when (tab) {
                        BottomNavTab.Home -> { /* already here */ }
                        BottomNavTab.Settings -> onNavigateToSettings()
                        BottomNavTab.Account -> onNavigateToAccount()
                    }
                }
            )
        }

        if (showServerSheet) {
            ServerSelectionSheet(
                onDismissRequest = { showServerSheet = false },
                servers = availableServers,
                selectedServer = currentServer,
                isAutoSelected = isAutoServerSelected,
                isLoading = isLoadingServers,
                error = serversError,
                onRefresh = { scope.launch { vpnManager.loadServers() } },
                onRefreshPing = { scope.launch { vpnManager.refreshAllPings(force = true) } },
                onSelectAuto = { scope.launch { vpnManager.autoSelectFastestServer() } },
                onSelectServer = { server -> vpnManager.selectServer(server) }
            )
        }

        if (showFaqDialog) {
            FaqDialog(onDismiss = { showFaqDialog = false })
        }
        if (showTrialSuccessDialog) {
            TrialActivatedDialog(
                isRussian = isRu,
                serversReady = trialServersReady,
                onDismiss = { showTrialSuccessDialog = false }
            )
        }
    }
}

@Composable
private fun homeIsRussian(): Boolean {
    val language by LocaleManager.currentLanguage.collectAsState()
    return LocaleManager.isRussian(language = language)
}

@Composable
private fun HomeNoPlanContent(
    onTrialClick: () -> Unit,
    onChoosePlanClick: () -> Unit,
    onRestoreViaTelegram: () -> Unit,
    onRetrySubscription: () -> Unit,
    subscriptionUnknown: Boolean,
    accessConfirmationRequired: Boolean,
    subscriptionStatus: com.example.bymevpn.data.api.SubscriptionStatus?,
    lastRefreshError: com.example.bymevpn.data.subscription.SubscriptionRefreshError?,
    offerTelegramRecovery: Boolean,
    isCheckingSubscription: Boolean,
    trialAvailable: Boolean,
    trialUnavailableReason: String?,
    isActivatingTrial: Boolean,
    isPreparingTrialServers: Boolean,
    trialError: String?
) {
    val isRu = homeIsRussian()
    val hasCheckout = BuildConfig.EXTERNAL_CHECKOUT_ENABLED
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = if (hasCheckout) 56.dp else 16.dp),
        contentAlignment = Alignment.Center
    ) {
        val compact = maxHeight < 530.dp
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = if (compact) 8.dp else 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (subscriptionUnknown || accessConfirmationRequired) {
                SubscriptionRecoveryCard(
                    isRu = isRu,
                    isChecking = isCheckingSubscription,
                    accessConfirmationRequired = accessConfirmationRequired,
                    status = subscriptionStatus,
                    lastError = lastRefreshError,
                    onRetry = onRetrySubscription
                )
            } else {
                ShieldLogo(size = if (compact) 84.dp else 104.dp)
                Spacer(Modifier.height(if (compact) 16.dp else 24.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(AppGradients.AccentCard)
                        .border(1.dp, AppColors.BorderAccent, RoundedCornerShape(20.dp))
                        .padding(if (compact) 16.dp else 20.dp)
                ) {
                    Text(
                        text = when {
                            trialAvailable -> if (isRu) "3 дня бесплатно" else "3 days free"
                            trialUnavailableReason == "trial_already_used" -> if (isRu) "Пробный период использован" else "Free trial already used"
                            trialUnavailableReason == "trial_activation_pending" -> if (isRu) "Проверяем активацию" else "Checking activation"
                            else -> if (isRu) "Доступ не активен" else "Access inactive"
                        },
                        color = AppColors.TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = when {
                            trialAvailable -> if (isRu) "Полный доступ без привязки карты и автосписаний." else "Full access with no card or auto-charges."
                            trialUnavailableReason == "trial_already_used" -> if (hasCheckout) {
                                if (isRu) "Выберите тариф, чтобы продолжить пользоваться VPN." else "Choose a plan to continue using VPN."
                            } else {
                                if (isRu) "Обновите статус или войдите под другим аккаунтом." else "Refresh your status or sign in with another account."
                            }
                            trialUnavailableReason == "trial_activation_pending" -> if (isRu) "Обновите статус через некоторое время." else "Refresh your status in a little while."
                            trialUnavailableReason in setOf("eligibility_check_unavailable", "device_confirmation_required", "subscription_check_unavailable") -> if (isRu) "Не удалось проверить пробный период. Попробуйте ещё раз." else "Could not check trial availability. Please try again."
                            else -> if (hasCheckout) {
                                if (isRu) "Выберите подходящий тариф." else "Choose a plan that works for you."
                            } else {
                                if (isRu) "Войдите в аккаунт с подпиской и обновите статус." else "Sign in with an active account and refresh status."
                            }
                        },
                        color = AppColors.TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    if (trialAvailable) {
                        Spacer(Modifier.height(12.dp))
                        HomeTrialPerkRow(if (isRu) "Все серверы и протоколы без ограничений" else "All servers and protocols unlocked")
                        Spacer(Modifier.height(8.dp))
                        HomeTrialPerkRow(if (isRu) "Без карты и автоматических списаний" else "No card required, no auto-renewal")
                    }
                    Spacer(Modifier.height(16.dp))
                    val shouldRetry = trialUnavailableReason in setOf(
                        "trial_activation_pending",
                        "eligibility_check_unavailable",
                        "device_confirmation_required",
                        "subscription_check_unavailable"
                    ) || (!trialAvailable && !hasCheckout)
                    ByMePrimaryButton(
                        text = when {
                            isPreparingTrialServers -> if (isRu) "Готовим серверы…" else "Preparing servers…"
                            isActivatingTrial -> if (isRu) "Подключаем…" else "Activating…"
                            trialAvailable -> if (isRu) "Активировать бесплатно" else "Activate free trial"
                            shouldRetry -> if (isRu) "Обновить статус" else "Refresh status"
                            else -> if (isRu) "Посмотреть тарифы" else "View plans"
                        },
                        onClick = when {
                            trialAvailable -> onTrialClick
                            shouldRetry -> onRetrySubscription
                            else -> onChoosePlanClick
                        },
                        enabled = !isActivatingTrial && !isCheckingSubscription,
                        height = 52.dp
                    )
                    if (offerTelegramRecovery) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = if (isRu) "Подписка в Telegram? Войти через Telegram"
                            else "Subscribed in Telegram? Sign in with Telegram",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onRestoreViaTelegram)
                                .padding(vertical = 4.dp),
                            textAlign = TextAlign.Center,
                            color = AppColors.SkyBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (!trialError.isNullOrBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(trialError, color = AppColors.StatusWarning, fontSize = 11.5.sp, lineHeight = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeTrialPerkRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CheckmarkIcon(color = AppColors.BrandMint, size = 14.dp)
        Text(
            text = text,
            color = AppColors.TextSoft,
            fontSize = 12.5.sp,
            lineHeight = 17.sp
        )
    }
}

@Composable
private fun HomeDisconnectedContent(
    isConnecting: Boolean,
    connectingHint: String?,
    onPowerClick: () -> Unit,
    onServerSelectClick: () -> Unit,
    onSubscriptionClick: () -> Unit,
    currentServer: ServerNode?,
    remainingDays: Long?,
    isTrial: Boolean,
    serversError: String?,
    isLoadingServers: Boolean,
    onRefreshServers: () -> Unit
) {
    val isRu = homeIsRussian()
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val buttonSize = when {
            maxHeight < 520.dp -> 188.dp
            maxHeight < 600.dp -> 216.dp
            else -> 244.dp
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = maxHeight)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.6f, fill = true))

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                VpnPowerButton(
                    connected = false,
                    connecting = isConnecting,
                    isRussian = isRu,
                    onClick = onPowerClick,
                    maxSize = buttonSize
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = if (isConnecting) {
                    if (isRu) "Подключение…" else "Connecting…"
                } else {
                    if (isRu) "VPN выключен" else "VPN is off"
                },
                color = AppColors.TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isConnecting) {
                    connectingHint?.takeIf { it.isNotBlank() }
                        ?: if (isRu) "Устанавливаем защищённый туннель" else "Establishing secure tunnel"
                } else {
                    if (isRu) "Нажмите, чтобы подключиться" else "Tap to connect"
                },
                color = AppColors.TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            if (currentServer == null && (isLoadingServers || !serversError.isNullOrBlank())) {
                Spacer(modifier = Modifier.height(16.dp))
                ServerLoadStatusRow(
                    isRu = isRu,
                    error = serversError,
                    isLoading = isLoadingServers,
                    onRefresh = onRefreshServers
                )
            }

            Spacer(modifier = Modifier.weight(1f, fill = true))
            Spacer(modifier = Modifier.height(16.dp))

            HomeBottomDock(
                isRu = isRu,
                isConnected = false,
                currentServer = currentServer,
                remainingDays = remainingDays,
                isTrial = isTrial,
                serversError = serversError,
                onServerSelectClick = onServerSelectClick,
                onRefreshServers = onRefreshServers,
                onSubscriptionClick = onSubscriptionClick
            )
        }
    }
}

@Composable
private fun HomeConnectedContent(
    vpnManager: VpnManager,
    onPowerClick: () -> Unit,
    onServerSelectClick: () -> Unit,
    onSubscriptionClick: () -> Unit,
    currentServer: ServerNode?,
    remainingDays: Long?,
    isTrial: Boolean,
    serversError: String?,
    isLoadingServers: Boolean,
    onRefreshServers: () -> Unit,
    successPulseKey: Int
) {
    val isRu = homeIsRussian()
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val buttonSize = when {
            maxHeight < 520.dp -> 188.dp
            maxHeight < 600.dp -> 216.dp
            else -> 244.dp
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = maxHeight)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.6f, fill = true))

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                VpnPowerButton(
                    connected = true,
                    connecting = false,
                    isRussian = isRu,
                    onClick = onPowerClick,
                    successPulseKey = successPulseKey,
                    maxSize = buttonSize
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CheckmarkIcon(color = AppColors.BrandMintBright, size = 22.dp)
                Text(
                    text = if (isRu) "VPN подключён" else "VPN connected",
                    color = AppColors.TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            ConnectedTimerText(vpnManager = vpnManager)

            Spacer(modifier = Modifier.weight(1f, fill = true))
            Spacer(modifier = Modifier.height(16.dp))

            HomeBottomDock(
                isRu = isRu,
                isConnected = true,
                currentServer = currentServer,
                remainingDays = remainingDays,
                isTrial = isTrial,
                serversError = serversError,
                onServerSelectClick = onServerSelectClick,
                onRefreshServers = onRefreshServers,
                onSubscriptionClick = onSubscriptionClick
            )
        }
    }
}

/**
 * Isolated timer observer so 1-second timer updates only recompose
 * the elapsed-time label rather than the entire HomeScreen hierarchy.
 */
@Composable
private fun ConnectedTimerText(vpnManager: VpnManager) {
    val connectedSeconds by vpnManager.connectedSeconds.collectAsState()
    val hours = connectedSeconds / 3600
    val minutes = (connectedSeconds % 3600) / 60
    val seconds = connectedSeconds % 60
    val timerText = String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)

    Text(
        text = timerText,
        color = AppColors.SkyBlue,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 2.sp
    )
}

@Composable
private fun HomeBottomDock(
    isRu: Boolean,
    isConnected: Boolean,
    currentServer: ServerNode?,
    remainingDays: Long?,
    isTrial: Boolean,
    serversError: String?,
    onServerSelectClick: () -> Unit,
    onRefreshServers: () -> Unit,
    onSubscriptionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(AppColors.BgSurface)
                .border(
                    1.dp,
                    if (isConnected) AppColors.BorderActiveGreen else AppColors.BorderDefault,
                    RoundedCornerShape(20.dp)
                )
                .clickable(onClick = if (currentServer == null) onRefreshServers else onServerSelectClick)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CountryFlagView(
                        countryCode = currentServer?.countryCode.orEmpty(),
                        fallbackFlag = currentServer?.flag.orEmpty(),
                        size = 24.dp
                    )
                    Text(
                        text = currentServer?.let { if (it.displayName.isNotBlank()) it.displayName else it.name }
                            ?: if (isRu) "Сервер не выбран" else "No server selected",
                        color = AppColors.TextPrimary,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(start = 34.dp)
                ) {
                    val proto = currentServer?.protocolLabel.orEmpty()
                    if (proto.isNotBlank()) {
                        ProtocolChip(proto = proto)
                    }
                    if (LocalDensity.current.fontScale < 1.3f || currentServer == null) {
                        val pingText = currentServer?.pingMs?.takeIf { it > 0 }?.let {
                            if (isRu) "$it мс" else "$it ms"
                        }
                        Text(
                            text = when {
                                currentServer == null -> serversError ?: if (isRu) "Нажмите, чтобы обновить" else "Tap to refresh"
                                isConnected -> pingText ?: if (isRu) "Подключено" else "Connected"
                                pingText != null -> pingText
                                isRu -> "Выбранный сервер"
                                else -> "Selected server"
                            },
                            color = if (isConnected) AppColors.BrandMint else AppColors.TextSecondary,
                            fontSize = 11.5.sp,
                            fontWeight = if (isConnected) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AppColors.BgSurfaceAccent)
                    .border(1.dp, AppColors.BorderDefault, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                ChevronRightIcon(color = AppColors.SkyBlue, size = 18.dp)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(AppColors.BgSurface)
                .border(
                    1.dp,
                    if (isTrial) AppColors.BorderAccent else AppColors.BorderDefault,
                    RoundedCornerShape(16.dp)
                )
                .clickable(onClick = onSubscriptionClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(AppColors.BgSurfaceAccent)
                    .border(1.dp, AppColors.BorderDefault, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                ClockVectorIcon(
                    color = if (isTrial) AppColors.BrandMint else AppColors.SkyBlue,
                    size = 16.dp
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        isTrial -> if (isRu) "Пробный период" else "Free trial"
                        isRu -> "Подписка активна"
                        else -> "Subscription active"
                    },
                    color = if (isTrial) AppColors.BrandMint else AppColors.TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = remainingDays?.let { if (isRu) "Осталось $it дн." else "$it days remaining" }
                        ?: if (isRu) "Срок доступа не подтверждён" else "Duration not confirmed",
                    color = AppColors.TextSecondary,
                    fontSize = 11.5.sp
                )
            }

            ChevronRightIcon(color = AppColors.TextMuted, size = 17.dp)
        }
    }
}

@Composable
private fun ProtocolChip(proto: String) {
    val isReality = proto.contains("Reality", ignoreCase = true)
    val isHysteria = proto.contains("Hysteria", ignoreCase = true)
    val bg = when {
        isReality -> Color(0x222EE9B8)
        isHysteria -> Color(0x227C6BFF)
        else -> Color(0x220077FF)
    }
    val stroke = when {
        isReality -> Color(0x552EE9B8)
        isHysteria -> Color(0x557C6BFF)
        else -> Color(0x5500D8FF)
    }
    val textColor = when {
        isReality -> AppColors.BrandMint
        isHysteria -> Color(0xFFB5A8FF)
        else -> AppColors.SkyBlue
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(1.dp, stroke, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = proto,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ServerLoadStatusRow(
    isRu: Boolean,
    error: String?,
    isLoading: Boolean,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.BgSurface)
            .border(1.dp, AppColors.BorderDefault, RoundedCornerShape(16.dp))
            .clickable(enabled = !isLoading, onClick = onRefresh)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(AppColors.BgSurfaceAccent),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = AppColors.SkyBlue,
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp
                )
            } else {
                RefreshIcon(color = AppColors.SkyBlue, size = 14.dp)
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isRu) "Список серверов" else "Server list",
                color = AppColors.TextPrimary,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = when {
                    isLoading -> if (isRu) "Получаем серверы…" else "Loading servers…"
                    !error.isNullOrBlank() -> error
                    else -> if (isRu) "Нажмите, чтобы обновить" else "Tap to refresh"
                },
                color = AppColors.TextSecondary,
                fontSize = 11.sp,
                maxLines = 2
            )
        }

        Text(
            text = if (isLoading) "" else if (isRu) "Обновить" else "Refresh",
            color = AppColors.SkyBlue,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun HomeConnectionErrorContent(
    onRetry: () -> Unit,
    onSupportClick: () -> Unit,
    onServerSelectClick: () -> Unit,
    currentServer: ServerNode?,
    errorMessage: String? = null
) {
    val isRu = homeIsRussian()
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val buttonSize = when {
            maxHeight < 520.dp -> 172.dp
            maxHeight < 600.dp -> 200.dp
            else -> 228.dp
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = maxHeight)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.5f, fill = true))

            VpnPowerButton(
                connected = false,
                connecting = false,
                isRussian = isRu,
                onClick = onRetry,
                maxSize = buttonSize
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = if (isRu) "Не удалось подключиться" else "Could not connect",
                color = AppColors.TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = errorMessage?.takeIf { it.isNotBlank() }
                    ?: if (isRu) "Проверьте интернет и попробуйте ещё раз" else "Check your internet connection and try again",
                color = AppColors.TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(AppColors.SecondaryButtonBg)
                    .border(1.dp, AppColors.BorderSecondaryButton, RoundedCornerShape(14.dp))
                    .clickable(onClick = onRetry)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RefreshIcon(color = AppColors.SkyBlue, size = 16.dp)
                Text(
                    text = if (isRu) "Повторить" else "Try again",
                    color = AppColors.SecondaryButtonText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.weight(1f, fill = true))
            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(AppColors.BgSurface)
                        .border(1.dp, AppColors.BorderDefault, RoundedCornerShape(20.dp))
                        .clickable(onClick = onServerSelectClick)
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CountryFlagView(
                                countryCode = currentServer?.countryCode.orEmpty(),
                                fallbackFlag = currentServer?.flag.orEmpty(),
                                size = 24.dp
                            )
                            Text(
                                text = currentServer?.let { if (it.displayName.isNotBlank()) it.displayName else it.name }
                                    ?: if (isRu) "Сервер не выбран" else "No server selected",
                                color = AppColors.TextPrimary,
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(start = 34.dp)
                        ) {
                            val proto = currentServer?.protocolLabel.orEmpty()
                            if (proto.isNotBlank()) {
                                ProtocolChip(proto = proto)
                            }
                            if (LocalDensity.current.fontScale < 1.3f) {
                                Text(
                                    text = if (isRu) "Текущий сервер" else "Current server",
                                    color = AppColors.TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AppColors.BgSurfaceAccent)
                            .border(1.dp, AppColors.BorderDefault, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        ChevronRightIcon(color = AppColors.SkyBlue, size = 18.dp)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(AppColors.BgSurface)
                        .border(1.dp, AppColors.BorderDefault, RoundedCornerShape(16.dp))
                        .clickable(onClick = onSupportClick)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuestionHelpIcon(color = AppColors.SkyBlue, size = 20.dp)

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isRu) "Не получается подключиться?" else "Having trouble connecting?",
                            color = AppColors.TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isRu) "Напишите нам — поможем разобраться" else "Contact us for help",
                            color = AppColors.TextSecondary,
                            fontSize = 11.5.sp
                        )
                    }

                    ChevronRightIcon(color = AppColors.TextMuted, size = 17.dp)
                }
            }
        }
    }
}
