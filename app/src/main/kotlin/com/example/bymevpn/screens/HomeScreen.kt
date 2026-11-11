package com.example.bymevpn.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bymevpn.components.BottomNavTab
import com.example.bymevpn.components.ByMeBottomNav
import com.example.bymevpn.components.ByMePrimaryButton
import com.example.bymevpn.components.ByMeTopBar
import com.example.bymevpn.components.CheckmarkIcon
import com.example.bymevpn.components.ChevronRightIcon
import com.example.bymevpn.components.CountryFlagView
import com.example.bymevpn.components.FaqDialog
import com.example.bymevpn.components.VpnDisclosureDialog
import com.example.bymevpn.components.PrivacyPolicyDialog
import com.example.bymevpn.components.TrialActivatedDialog
import com.example.bymevpn.data.storage.SecureTokenStorage
import com.example.bymevpn.data.api.ByMeApiClient
import com.example.bymevpn.components.PowerIcon
import com.example.bymevpn.components.QuestionHelpIcon
import com.example.bymevpn.components.RefreshIcon
import com.example.bymevpn.components.ServerSelectionSheet
import com.example.bymevpn.components.ShieldLogo
import com.example.bymevpn.components.VpnPowerButton
import com.example.bymevpn.data.api.ServerNode
import com.example.bymevpn.data.LocaleManager
import com.example.bymevpn.data.subscription.SubscriptionManager
import com.example.bymevpn.data.subscription.TrialActivationPolicy
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
    val connectedSeconds by vpnManager.connectedSeconds.collectAsState()
    val serversError by vpnManager.serversError.collectAsState()
    val isLoadingServers by vpnManager.isLoadingServers.collectAsState()

    val subManager = remember { SubscriptionManager.getInstance(context) }
    val subscription by subManager.subscriptionStatus.collectAsState()

    val hasPlan = subscription?.let { it.isActive && !it.knownExpiryReached(System.currentTimeMillis()) } == true
    val remainingDays = subscription?.daysRemainingAt(System.currentTimeMillis())

    var showServerSheet by remember { mutableStateOf(false) }
    var isAutoServerSelected by remember { mutableStateOf(true) }
    var showFaqDialog by remember { mutableStateOf(false) }
    val secureStorage = remember { SecureTokenStorage.getInstance(context) }
    var isActivatingTrial by remember { mutableStateOf(false) }
    var isPreparingTrialServers by remember { mutableStateOf(false) }
    var trialActivationError by remember { mutableStateOf<String?>(null) }
    var showTrialSuccessDialog by remember { mutableStateOf(false) }
    var trialServersReady by remember { mutableStateOf(false) }
    var vpnSuccessPulseKey by remember { mutableStateOf(0) }
    var previousVpnState by remember { mutableStateOf(vpnState) }
    var showVpnDisclosure by remember { mutableStateOf(false) }
    var showPrivacyPolicy by remember { mutableStateOf(false) }
    val updateInfo by AppUpdateManager.updateInfo.collectAsState()
    var isInstallingUpdate by remember { mutableStateOf(false) }

    LaunchedEffect(vpnState) {
        if (vpnState == VpnConnectionState.CONNECTED && previousVpnState != VpnConnectionState.CONNECTED) {
            vpnSuccessPulseKey++
        }
        previousVpnState = vpnState
    }

    // Fetch stale/missing plan prices before the user opens the pricing screen.
    LaunchedEffect(Unit) {
        runCatching { ByMeApiClient(context).warmPlanCatalog() }
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

    val startUpdateInstall: (com.example.bymevpn.data.update.AppUpdateInfo) -> Unit = { info ->
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

    // Servers must be ready before the user taps "connect", not after it.
    // A temporary API failure is retried with a small backoff instead of leaving
    // the screen empty until the user finds the refresh button.
    LaunchedEffect(hasPlan, availableServers.isEmpty()) {
        if (!hasPlan || availableServers.isNotEmpty()) return@LaunchedEffect
        var attempt = 0
        while (attempt < 3 && vpnManager.availableServers.value.isEmpty()) {
            vpnManager.loadServers()
            if (vpnManager.availableServers.value.isNotEmpty()) break
            attempt++
            if (attempt < 3) kotlinx.coroutines.delay(if (attempt == 1) 8_000L else 30_000L)
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
                val result = subManager.activateTrial()
                result.onSuccess {
                    // Keep the home screen in a calm preparation state while the newly
                    // granted device bootstrap propagates to the server list endpoint.
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

    // VPN Permission Launcher
    val vpnPrepareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            VpnManager.startVpn(context, currentServer)
        } else {
            Toast.makeText(context, if (isRu) "Требуется разрешение для создания VPN-туннеля" else "VPN permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    fun prepareAndStartVpn() {
        val prepareIntent = VpnService.prepare(context)
        if (prepareIntent != null) {
            vpnPrepareLauncher.launch(prepareIntent)
        } else {
            VpnManager.startVpn(context, currentServer)
        }
    }

    fun toggleVpn() {
        if (!hasPlan) {
            onNavigateToTrial()
            return
        }

        when (vpnState) {
            VpnConnectionState.CONNECTED -> {
                VpnManager.stopVpn(context)
            }
            VpnConnectionState.CONNECTING -> {
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
            // TopBar (compact brand ByMe + VPN, right help & more buttons)
            ByMeTopBar(
                title = null,
                onHelpClick = { showFaqDialog = true },
                onMoreClick = null
            )

            if (updateInfo != null) {
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF163128))
                            .border(1.dp, AppColors.BrandMint, RoundedCornerShape(12.dp))
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
                            .padding(horizontal = 14.dp, vertical = 10.dp),
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
                                    else -> if (isRu) "Нажмите, чтобы скачать и установить" else "Tap to download and install"
                                },
                                color = Color(0xFFA1C7BD),
                                fontSize = 10.5.sp
                            )
                        }
                        Text(
                            text = when {
                                isInstallingUpdate -> if (isRu) "Загрузка…" else "Downloading…"
                                else -> if (isRu) "Обновить >" else "Update >"
                            },
                            color = AppColors.BrandMint,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Dynamic Center Content based on State:
            // State 1: 03_home_no_plan
            // State 2: 14_connection_error
            // State 3: 05_home_on
            // State 4: 04_home_off
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (!hasPlan || isActivatingTrial) {
                    // MOCKUP 03_home_no_plan
                    HomeNoPlanContent(
                        onTrialClick = { activateTrialFromHome() },
                        onChoosePlanClick = onNavigateToPricing,
                        trialAvailable = isActivatingTrial || TrialActivationPolicy.shouldOfferTrial(subscription),
                        isActivatingTrial = isActivatingTrial,
                        isPreparingTrialServers = isPreparingTrialServers,
                        trialError = trialActivationError
                    )
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .clickable(onClick = onNavigateToPricing)
                            .padding(horizontal = 26.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (homeIsRussian()) "Посмотреть все тарифы" else "See all plans",
                            color = Color(0xFFB9DFD3),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        ChevronRightIcon(color = Color(0xFFB9DFD3), size = 16.dp)
                    }
                } else if (vpnState == VpnConnectionState.ERROR) {
                    val vpnError by vpnManager.errorMessage.collectAsState()
                    // MOCKUP 14_connection_error
                    HomeConnectionErrorContent(
                        onRetry = { toggleVpn() },
                        onSupportClick = onNavigateToSupport,
                        onServerSelectClick = { showServerSheet = true },
                        currentServer = currentServer,
                        errorMessage = vpnError
                    )
                } else if (vpnState == VpnConnectionState.CONNECTED) {
                    // MOCKUP 05_home_on
                    HomeConnectedContent(
                        connectedSeconds = connectedSeconds,
                        onPowerClick = { toggleVpn() },
                        onServerSelectClick = { showServerSheet = true },
                        onSubscriptionClick = onNavigateToAccount,
                        currentServer = currentServer,
                        remainingDays = remainingDays,
                        serversError = serversError,
                        isLoadingServers = isLoadingServers,
                        onRefreshServers = { refreshServers() },
                        successPulseKey = vpnSuccessPulseKey
                    )
                } else {
                    // MOCKUP 04_home_off (also handles connecting)
                    HomeDisconnectedContent(
                        isConnecting = vpnState == VpnConnectionState.CONNECTING,
                        onPowerClick = { toggleVpn() },
                        onServerSelectClick = { showServerSheet = true },
                        onSubscriptionClick = onNavigateToAccount,
                        currentServer = currentServer,
                        remainingDays = remainingDays,
                        serversError = serversError,
                        isLoadingServers = isLoadingServers,
                        onRefreshServers = { refreshServers() }
                    )
                }
            }

            // Bottom Navigation Bar (Главная active)
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

        // Server Selection Bottom Sheet
        if (showServerSheet) {
            ServerSelectionSheet(
                onDismissRequest = { showServerSheet = false },
                servers = availableServers,
                selectedServer = currentServer,
                isAutoSelected = isAutoServerSelected,
                isLoading = isLoadingServers,
                error = serversError,
                onRefresh = { scope.launch { vpnManager.loadServers() } },
                onSelectAuto = {
                    isAutoServerSelected = true
                    // "Automatic" must measure latency, not silently take the first row.
                    scope.launch { vpnManager.autoSelectFastestServer() }
                },
                onSelectServer = { server ->
                    isAutoServerSelected = false
                    vpnManager.selectServer(server)
                }
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

/**
 * 03_home_no_plan: State when user has no active subscription.
 */
@Composable
private fun homeIsRussian(): Boolean {
    val language by LocaleManager.currentLanguage.collectAsState()
    return LocaleManager.isRussian(language = language)
}

@Composable
private fun HomeNoPlanContent(
    onTrialClick: () -> Unit,
    onChoosePlanClick: () -> Unit,
    trialAvailable: Boolean,
    isActivatingTrial: Boolean,
    isPreparingTrialServers: Boolean,
    trialError: String?
) {
    val isRu = homeIsRussian()
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().padding(bottom = 56.dp),
        contentAlignment = Alignment.Center
    ) {
        val compact = maxHeight < 530.dp
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 26.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ShieldLogo(size = if (compact) 105.dp else 145.dp)
            Spacer(Modifier.height(if (compact) 8.dp else 14.dp))
            Text(
                text = if (isRu) "VPN для ваших устройств" else "VPN for your devices",
                color = AppColors.TextPrimary,
                fontSize = if (compact) 22.sp else 25.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (isRu) "Подключайтесь, когда вам нужен VPN." else "Connect whenever you need a VPN.",
                color = Color(0xFF91A4A5), fontSize = 13.sp,
                lineHeight = 18.sp, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(if (compact) 14.dp else 22.dp))
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(21.dp))
                    .background(AppGradients.AccentCard)
                    .border(1.dp, Color(0xFF285A4C), RoundedCornerShape(21.dp))
                    .padding(if (compact) 16.dp else 19.dp)
            ) {
                Text(
                    text = if (trialAvailable) {
                        if (isRu) "3 дня бесплатно" else "3 days free"
                    } else {
                        if (isRu) "Пробный период" else "Free trial"
                    },
                    color = AppColors.TextPrimary, fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (trialAvailable) {
                        if (isRu) "Попробуйте сервис перед покупкой." else "Try the service before buying."
                    } else {
                        if (isRu) "Выберите удобный вариант доступа." else "Choose the access plan that works for you."
                    },
                    color = Color(0xFFA1B4AD), fontSize = 12.sp
                )
                Spacer(Modifier.height(14.dp))
                ByMePrimaryButton(
                    text = when {
                        isPreparingTrialServers -> if (isRu) "Готовим серверы…" else "Preparing servers…"
                        isActivatingTrial -> if (isRu) "Подключаем пробный период…" else "Activating your trial…"
                        trialAvailable -> if (isRu) "Активировать бесплатно" else "Activate free trial"
                        else -> if (isRu) "Посмотреть тарифы" else "View plans"
                    },
                    onClick = if (trialAvailable) onTrialClick else onChoosePlanClick,
                    enabled = !isActivatingTrial,
                    height = 50.dp
                )
                if (!trialError.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(trialError, color = Color(0xFFE8BC84), fontSize = 11.sp, lineHeight = 15.sp)
                }
            }
        }
    }
}

/**
 * 04_home_off: State when subscription is active, but VPN is disconnected.
 */
@Composable
private fun HomeDisconnectedContent(
    isConnecting: Boolean,
    onPowerClick: () -> Unit,
    onServerSelectClick: () -> Unit,
    onSubscriptionClick: () -> Unit,
    currentServer: ServerNode?,
    remainingDays: Long?,
    serversError: String?,
    isLoadingServers: Boolean,
    onRefreshServers: () -> Unit
) {
    val isRu = homeIsRussian()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 25.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = if (isRu) "ВАШЕ ПОДКЛЮЧЕНИЕ" else "YOUR CONNECTION",
            color = Color(0xFF78999A),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(26.dp))

        Box(modifier = Modifier.weight(1f, fill = false).fillMaxWidth(), contentAlignment = Alignment.Center) {
            VpnPowerButton(connected = false, connecting = isConnecting, isRussian = isRu, onClick = onPowerClick)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = if (isConnecting) { if (isRu) "Подключение..." else "Connecting..." } else { if (isRu) "VPN выключен" else "VPN is off" },
            color = AppColors.TextPrimary,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isConnecting) { if (isRu) "Устанавливаем туннель" else "Establishing tunnel" } else { if (isRu) "Нажмите, чтобы подключиться" else "Tap to connect" },
            color = Color(0xFF859B9D),
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Home Bottom Dock: Server Summary + Access Strip
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Server summary card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF111B1E))
                    .border(1.dp, Color(0xFF263E41), RoundedCornerShape(20.dp))
                    .clickable(onClick = if (currentServer == null) onRefreshServers else onServerSelectClick)
                    .padding(horizontal = 19.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (isRu) "МЕСТОПОЛОЖЕНИЕ" else "LOCATION",
                        color = Color(0xFF698589),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CountryFlagView(
                            countryCode = currentServer?.countryCode.orEmpty(),
                            size = 24.dp
                        )
                        Text(
                            text = currentServer?.let { if (it.displayName.isNotBlank()) it.displayName else it.name } ?: if (isRu) "Сервер не выбран" else "No server selected",
                            color = AppColors.TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(start = 34.dp)
                    ) {
                        val proto = currentServer?.protocol.orEmpty()
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(
                                    if (proto.isBlank()) Color.Transparent
                                    else if (proto.contains("Reality", ignoreCase = true)) Color(0x2B3CDFAC)
                                    else if (proto.contains("Hysteria", ignoreCase = true)) Color(0x2B9D7BFF)
                                    else Color(0x2B4DAFFF)
                                )
                                .border(
                                    1.dp,
                                    if (proto.isBlank()) Color.Transparent
                                    else if (proto.contains("Reality", ignoreCase = true)) Color(0x593CDFAC)
                                    else if (proto.contains("Hysteria", ignoreCase = true)) Color(0x599D7BFF)
                                    else Color(0x594DAFFF),
                                    RoundedCornerShape(5.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = proto,
                                color = if (proto.contains("Reality", ignoreCase = true)) AppColors.BrandMint
                                        else if (proto.contains("Hysteria", ignoreCase = true)) Color(0xFFC4B0FF)
                                        else Color(0xFF78C6FF),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = when {
                                currentServer == null -> serversError ?: if (isRu) "Нажмите, чтобы обновить" else "Tap to refresh"
                                isRu -> "Текущий сервер"
                                else -> "Current server"
                            },
                            color = Color(0xFF81999B),
                            fontSize = 11.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1B3030)),
                    contentAlignment = Alignment.Center
                ) {
                    ChevronRightIcon(color = Color(0xFF9DD5CB), size = 18.dp)
                }
            }

            // Subscription access strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(17.dp))
                    .background(Color(0xFF101B1C))
                    .border(1.dp, Color(0xFF243739), RoundedCornerShape(17.dp))
                    .clickable(onClick = onSubscriptionClick)
                    .padding(horizontal = 17.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF162B27)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "⏱", fontSize = 14.sp)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isRu) "Подписка активна" else "Subscription active",
                        color = Color(0xFFEDF5F2),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = remainingDays?.let { if (isRu) "Осталось $it дней" else "$it days remaining" } ?: if (isRu) "Срок подписки не подтверждён" else "Subscription duration not confirmed",
                        color = Color(0xFF8AA1A1),
                        fontSize = 11.sp
                    )
                }

                ChevronRightIcon(color = Color(0xFF73898A), size = 17.dp)
            }
        }
    }
}

/**
 * 05_home_on: State when VPN is connected.
 */
@Composable
private fun HomeConnectedContent(
    connectedSeconds: Long,
    onPowerClick: () -> Unit,
    onServerSelectClick: () -> Unit,
    onSubscriptionClick: () -> Unit,
    currentServer: ServerNode?,
    remainingDays: Long?,
    serversError: String?,
    isLoadingServers: Boolean,
    onRefreshServers: () -> Unit,
    successPulseKey: Int
) {
    val isRu = homeIsRussian()
    val hours = connectedSeconds / 3600
    val minutes = (connectedSeconds % 3600) / 60
    val seconds = connectedSeconds % 60
    val timerText = String.format("%02d:%02d:%02d", hours, minutes, seconds)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 25.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = if (isRu) "ВАШЕ ПОДКЛЮЧЕНИЕ" else "YOUR CONNECTION",
            color = Color(0xFF78999A),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(26.dp))

        Box(modifier = Modifier.weight(1f, fill = false).fillMaxWidth(), contentAlignment = Alignment.Center) {
            VpnPowerButton(
                connected = true,
                connecting = false,
                isRussian = isRu,
                onClick = onPowerClick,
                successPulseKey = successPulseKey
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CheckmarkIcon(color = Color(0xFF3CDCAD), size = 22.dp)
            Text(
            text = if (isRu) "VPN запущен" else "VPN started",
                color = AppColors.TextPrimary,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (isRu) "Соединение с сервером проверено" else "Server connection checked",
            color = Color(0xFF859B9D),
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = timerText,
            color = Color(0xFF6FABA1),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Home Bottom Dock
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Server summary card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF111B1E))
                    .border(1.dp, Color(0xFF263E41), RoundedCornerShape(20.dp))
                    .clickable(onClick = onServerSelectClick)
                    .padding(horizontal = 19.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (isRu) "МЕСТОПОЛОЖЕНИЕ" else "LOCATION",
                        color = Color(0xFF698589),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CountryFlagView(
                            countryCode = currentServer?.countryCode.orEmpty(),
                            size = 24.dp
                        )
                        Text(
                            text = currentServer?.let { if (it.displayName.isNotBlank()) it.displayName else it.name } ?: if (isRu) "Сервер не выбран" else "No server selected",
                            color = AppColors.TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(start = 34.dp)
                    ) {
                        val proto = currentServer?.protocol.orEmpty()
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(
                                    if (proto.isBlank()) Color.Transparent
                                    else if (proto.contains("Reality", ignoreCase = true)) Color(0x2B3CDFAC)
                                    else if (proto.contains("Hysteria", ignoreCase = true)) Color(0x2B9D7BFF)
                                    else Color(0x2B4DAFFF)
                                )
                                .border(
                                    1.dp,
                                    if (proto.isBlank()) Color.Transparent
                                    else if (proto.contains("Reality", ignoreCase = true)) Color(0x593CDFAC)
                                    else if (proto.contains("Hysteria", ignoreCase = true)) Color(0x599D7BFF)
                                    else Color(0x594DAFFF),
                                    RoundedCornerShape(5.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = proto,
                                color = if (proto.contains("Reality", ignoreCase = true)) AppColors.BrandMint
                                        else if (proto.contains("Hysteria", ignoreCase = true)) Color(0xFFC4B0FF)
                                        else Color(0xFF78C6FF),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = if (isRu) "Подключено" else "Connected",
                            color = AppColors.BrandMint,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1B3030)),
                    contentAlignment = Alignment.Center
                ) {
                    ChevronRightIcon(color = Color(0xFF9DD5CB), size = 18.dp)
                }
            }

            // Subscription access strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(17.dp))
                    .background(Color(0xFF101B1C))
                    .border(1.dp, Color(0xFF243739), RoundedCornerShape(17.dp))
                    .clickable(onClick = onSubscriptionClick)
                    .padding(horizontal = 17.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF162B27)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "⏱", fontSize = 14.sp)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isRu) "Подписка активна" else "Subscription active",
                        color = Color(0xFFEDF5F2),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = remainingDays?.let { if (isRu) "Осталось $it дней" else "$it days remaining" } ?: if (isRu) "Срок подписки не подтверждён" else "Subscription duration not confirmed",
                        color = Color(0xFF8AA1A1),
                        fontSize = 11.sp
                    )
                }

                ChevronRightIcon(color = Color(0xFF73898A), size = 17.dp)
            }
        }
    }
}

/**
 * Compact status/retry strip shown while the server list is missing or being refreshed.
 */
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
            .clip(RoundedCornerShape(15.dp))
            .background(Color(0xFF152420))
            .border(1.dp, Color(0xFF2A4A42), RoundedCornerShape(15.dp))
            .clickable(enabled = !isLoading, onClick = onRefresh)
            .padding(horizontal = 15.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(Color(0xFF1D3731)),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = AppColors.BrandMint,
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp
                )
            } else {
                RefreshIcon(color = AppColors.BrandMint, size = 14.dp)
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isRu) "Список серверов" else "Server list",
                color = Color(0xFFEDF5F2),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = when {
                    isLoading -> if (isRu) "Получаем серверы…" else "Loading servers…"
                    !error.isNullOrBlank() -> error
                    else -> if (isRu) "Нажмите, чтобы обновить" else "Tap to refresh"
                },
                color = Color(0xFF8AA1A1),
                fontSize = 10.5.sp,
                maxLines = 2
            )
        }

        Text(
            text = if (isLoading) "" else if (isRu) "Обновить" else "Refresh",
            color = AppColors.BrandMint,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * 14_connection_error: State when VPN connection fails.
 */
@Composable
private fun HomeConnectionErrorContent(
    onRetry: () -> Unit,
    onSupportClick: () -> Unit,
    onServerSelectClick: () -> Unit,
    currentServer: ServerNode?,
    errorMessage: String? = null
) {
    val isRu = homeIsRussian()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 25.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = if (isRu) "ВАШЕ ПОДКЛЮЧЕНИЕ" else "YOUR CONNECTION",
            color = Color(0xFF78999A),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(26.dp))

        VpnPowerButton(connected = false, connecting = false, isRussian = isRu, onClick = onRetry)

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = if (isRu) "Не удалось подключиться" else "Could not connect",
            color = AppColors.TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = errorMessage?.takeIf { it.isNotBlank() } ?: if (isRu) "Проверьте интернет и попробуйте ещё раз" else "Check your internet connection and try again",
            color = Color(0xFF859B9D),
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Retry button matching mockup 14_connection_error
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(13.dp))
                .background(Color(0xFF19312B))
                .border(1.dp, Color(0xFF3A715D), RoundedCornerShape(13.dp))
                .clickable(onClick = onRetry)
                .padding(horizontal = 22.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            RefreshIcon(color = Color(0xFFB6E2D4), size = 17.dp)
            Text(
                text = if (isRu) "Повторить" else "Try again",
                color = Color(0xFFB6E2D4),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Bottom Dock
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Selected server card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF111B1E))
                    .border(1.dp, Color(0xFF263E41), RoundedCornerShape(20.dp))
                    .clickable(onClick = onServerSelectClick)
                    .padding(horizontal = 19.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (isRu) "ВЫБРАННЫЙ СЕРВЕР" else "SELECTED SERVER",
                        color = Color(0xFF698589),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CountryFlagView(
                            countryCode = currentServer?.countryCode.orEmpty(),
                            size = 24.dp
                        )
                        Text(
                            text = currentServer?.let { if (it.displayName.isNotBlank()) it.displayName else it.name } ?: if (isRu) "Сервер не выбран" else "No server selected",
                            color = AppColors.TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(start = 34.dp)
                    ) {
                        val proto = currentServer?.protocol.orEmpty()
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(
                                    if (proto.isBlank()) Color.Transparent
                                    else if (proto.contains("Reality", ignoreCase = true)) Color(0x2B3CDFAC)
                                    else if (proto.contains("Hysteria", ignoreCase = true)) Color(0x2B9D7BFF)
                                    else Color(0x2B4DAFFF)
                                )
                                .border(
                                    1.dp,
                                    if (proto.isBlank()) Color.Transparent
                                    else if (proto.contains("Reality", ignoreCase = true)) Color(0x593CDFAC)
                                    else if (proto.contains("Hysteria", ignoreCase = true)) Color(0x599D7BFF)
                                    else Color(0x594DAFFF),
                                    RoundedCornerShape(5.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = proto,
                                color = if (proto.contains("Reality", ignoreCase = true)) AppColors.BrandMint
                                        else if (proto.contains("Hysteria", ignoreCase = true)) Color(0xFFC4B0FF)
                                        else Color(0xFF78C6FF),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = if (isRu) "Текущий сервер" else "Current server",
                            color = Color(0xFF81999B),
                            fontSize = 11.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1B3030)),
                    contentAlignment = Alignment.Center
                ) {
                    ChevronRightIcon(color = Color(0xFF9DD5CB), size = 18.dp)
                }
            }

            // Support help card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(17.dp))
                    .background(Color(0xFF101B1C))
                    .border(1.dp, Color(0xFF243739), RoundedCornerShape(17.dp))
                    .clickable(onClick = onSupportClick)
                    .padding(horizontal = 17.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuestionHelpIcon(color = Color(0xFF7CC9B2), size = 20.dp)

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isRu) "Не получается подключиться?" else "Having trouble connecting?",
                        color = Color(0xFFEDF5F2),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isRu) "Напишите нам — поможем разобраться" else "Contact us for help",
                        color = Color(0xFF8AA1A1),
                        fontSize = 11.sp
                    )
                }

                ChevronRightIcon(color = Color(0xFF73898A), size = 17.dp)
            }
        }
    }
}
