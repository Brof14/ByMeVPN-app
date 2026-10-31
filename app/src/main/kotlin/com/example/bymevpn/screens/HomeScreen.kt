package com.example.bymevpn.screens

import android.app.Activity
import android.content.Context
import android.net.VpnService
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.bymevpn.components.PowerIcon
import com.example.bymevpn.components.QuestionHelpIcon
import com.example.bymevpn.components.RefreshIcon
import com.example.bymevpn.components.ServerSelectionSheet
import com.example.bymevpn.components.ShieldLogo
import com.example.bymevpn.data.api.ServerNode
import com.example.bymevpn.data.LocaleManager
import com.example.bymevpn.data.subscription.SubscriptionManager
import com.example.bymevpn.data.update.AppUpdateManager
import com.example.bymevpn.data.payment.YooKassaPaymentHandler
import com.example.bymevpn.theme.AppColors
import com.example.bymevpn.theme.AppGradients
import com.example.bymevpn.vpn.VpnConnectionState
import com.example.bymevpn.vpn.VpnManager
import kotlinx.coroutines.launch

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

    val subManager = remember { SubscriptionManager.getInstance(context) }
    val subscription by subManager.subscriptionStatus.collectAsState()

    val hasPlan = subscription?.let { it.isActive && !it.knownExpiryReached(System.currentTimeMillis()) } == true
    val remainingDays = subscription?.daysRemainingAt(System.currentTimeMillis())

    var showServerSheet by remember { mutableStateOf(false) }
    var isAutoServerSelected by remember { mutableStateOf(true) }
    var showFaqDialog by remember { mutableStateOf(false) }
    val updateInfo by AppUpdateManager.updateInfo.collectAsState()

    LaunchedEffect(Unit) {
        AppUpdateManager.checkForUpdates()
        YooKassaPaymentHandler.checkAndCompletePendingPayment(context)
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
                val prepareIntent = VpnService.prepare(context)
                if (prepareIntent != null) {
                    vpnPrepareLauncher.launch(prepareIntent)
                } else {
                    VpnManager.startVpn(context, currentServer)
                }
            }
        }
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
                            .border(1.dp, Color(0xFF3CDFAC), RoundedCornerShape(12.dp))
                            .clickable {
                                AppUpdateManager.openUpdateDownload(context, updateInfo!!.downloadUrl)
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isRu) "Доступно обновление v${updateInfo!!.latestVersion}" else "Update v${updateInfo!!.latestVersion} available",
                                color = Color(0xFF3CDFAC),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isRu) "Нажмите, чтобы скачать и обновить приложение" else "Tap to download and update the app",
                                color = Color(0xFFA1C7BD),
                                fontSize = 10.5.sp
                            )
                        }
                        Text(
                            text = if (isRu) "Обновить >" else "Update >",
                            color = Color(0xFF3CDFAC),
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
                if (!hasPlan) {
                    // MOCKUP 03_home_no_plan
                    HomeNoPlanContent(
                        onTrialClick = onNavigateToTrial,
                        onPlansClick = onNavigateToPricing,
                        hasServers = availableServers.isNotEmpty(),
                        onServerSelectClick = { showServerSheet = true }
                    )
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
                        remainingDays = remainingDays
                    )
                } else {
                    // MOCKUP 04_home_off (also handles connecting)
                    HomeDisconnectedContent(
                        isConnecting = vpnState == VpnConnectionState.CONNECTING,
                        onPowerClick = { toggleVpn() },
                        onServerSelectClick = { showServerSheet = true },
                        onSubscriptionClick = onNavigateToAccount,
                        currentServer = currentServer,
                        remainingDays = remainingDays
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
                onSelectAuto = {
                    isAutoServerSelected = true
                    if (availableServers.isNotEmpty()) {
                        vpnManager.selectServer(availableServers.first())
                    }
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
    onPlansClick: () -> Unit,
    hasServers: Boolean,
    onServerSelectClick: () -> Unit
) {
    val isRu = homeIsRussian()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Center emblem with glowing halo
        Box(
            modifier = Modifier.size(190.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(176.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            0.0f to Color(0x1324D3A3),
                            0.67f to Color.Transparent
                        )
                    )
                    .border(1.dp, Color(0x214CB299), CircleShape)
            )
            ShieldLogo(size = 104.dp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "BYMEVPN",
            color = Color(0xFF40CAA6),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = if (isRu) "Свободный интернет.\nОдно касание." else "Open internet.\nOne tap.",
            color = AppColors.TextPrimary,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 36.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (isRu) "Подключайтесь к привычным сервисам с ByMeVPN — без сложных настроек." else "Connect to the services you use with ByMeVPN.",
            color = Color(0xFF91A4A5),
            fontSize = 13.sp,
            lineHeight = 19.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 14.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Mini perks
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                CheckmarkIcon(color = Color(0xFF38D5A5), size = 15.dp)
                Text(
                    text = if (isRu) "Простое подключение" else "Easy connection",
                    color = Color(0xFF89A99E),
                    fontSize = 11.sp
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                CheckmarkIcon(color = Color(0xFF38D5A5), size = 15.dp)
                Text(
                    text = if (isRu) "Прозрачные условия" else "Clear terms",
                    color = Color(0xFF89A99E),
                    fontSize = 11.sp
                )
            }
        }

        if (hasServers) {
            Text(
                text = if (isRu) "Посмотреть серверы  ›" else "View servers  ›",
                color = AppColors.BrandGreen,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onServerSelectClick).padding(12.dp)
            )
        }
        Spacer(modifier = Modifier.weight(1f))

        // Action dock: Trial card + All plans link
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(21.dp))
                    .background(AppGradients.AccentCard)
                    .border(1.dp, Color(0xFF285A4C), RoundedCornerShape(21.dp))
                    .padding(19.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "01",
                        color = Color(0xFF3BCB9D),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (isRu) "Для новых пользователей" else "For new users",
                        color = Color(0xFFA0C2AE),
                        fontSize = 10.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = if (isRu) "3 дня " else "3 days ",
                        color = AppColors.TextPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        text = if (isRu) "бесплатно" else "free",
                        color = Color(0xFF43E0AC),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-1).sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isRu) "Попробуйте сервис перед покупкой." else "Try the service before buying.",
                    color = Color(0xFFA1B4AD),
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                ByMePrimaryButton(
                    text = if (isRu) "Попробовать бесплатно" else "Try for free",
                    onClick = onTrialClick,
                    height = 50.dp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (isRu) "После пробного периода можно выбрать тариф." else "Choose a plan after your trial.",
                    color = Color(0xFF8DABA5),
                    fontSize = 10.5.sp,
                    lineHeight = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onPlansClick)
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isRu) "Посмотреть все тарифы" else "See all plans",
                    color = Color(0xFFB9DFD3),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(5.dp))
                ChevronRightIcon(color = Color(0xFFB9DFD3), size = 16.dp)
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
    remainingDays: Long?
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

        Box(modifier = Modifier.size(235.dp).clickable(onClickLabel = if (isRu) "Включить VPN" else "Connect VPN", onClick = onPowerClick), contentAlignment = Alignment.Center) {
            ShieldLogo(size = 205.dp, isActive = true, glowAlpha = if (isConnecting) 0.9f else 0.35f)
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

        Spacer(modifier = Modifier.weight(1f))

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
                                    if (proto.contains("Reality", ignoreCase = true)) Color(0x2B3CDFAC)
                                    else if (proto.contains("Hysteria", ignoreCase = true)) Color(0x2B9D7BFF)
                                    else Color(0x2B4DAFFF)
                                )
                                .border(
                                    1.dp,
                                    if (proto.contains("Reality", ignoreCase = true)) Color(0x593CDFAC)
                                    else if (proto.contains("Hysteria", ignoreCase = true)) Color(0x599D7BFF)
                                    else Color(0x594DAFFF),
                                    RoundedCornerShape(5.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = proto,
                                color = if (proto.contains("Reality", ignoreCase = true)) Color(0xFF3CDFAC)
                                        else if (proto.contains("Hysteria", ignoreCase = true)) Color(0xFFC4B0FF)
                                        else Color(0xFF78C6FF),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = if (isRu) "Выбрано автоматически" else "Selected automatically",
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
    remainingDays: Long?
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

        Box(modifier = Modifier.size(235.dp).clickable(onClickLabel = if (isRu) "Выключить VPN" else "Disconnect VPN", onClick = onPowerClick), contentAlignment = Alignment.Center) {
            ShieldLogo(size = 205.dp, isActive = true, glowAlpha = 1f)
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

        Spacer(modifier = Modifier.weight(1f))

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
                                    if (proto.contains("Reality", ignoreCase = true)) Color(0x2B3CDFAC)
                                    else if (proto.contains("Hysteria", ignoreCase = true)) Color(0x2B9D7BFF)
                                    else Color(0x2B4DAFFF)
                                )
                                .border(
                                    1.dp,
                                    if (proto.contains("Reality", ignoreCase = true)) Color(0x593CDFAC)
                                    else if (proto.contains("Hysteria", ignoreCase = true)) Color(0x599D7BFF)
                                    else Color(0x594DAFFF),
                                    RoundedCornerShape(5.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = proto,
                                color = if (proto.contains("Reality", ignoreCase = true)) Color(0xFF3CDFAC)
                                        else if (proto.contains("Hysteria", ignoreCase = true)) Color(0xFFC4B0FF)
                                        else Color(0xFF78C6FF),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = if (isRu) "Подключено" else "Connected",
                            color = Color(0xFF3CDFAC),
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

        Box(modifier = Modifier.size(235.dp).clickable(onClickLabel = if (isRu) "Повторить подключение" else "Retry connection", onClick = onRetry), contentAlignment = Alignment.Center) {
            ShieldLogo(size = 205.dp, isActive = false, glowAlpha = 0.2f)
        }

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
                                    if (proto.contains("Reality", ignoreCase = true)) Color(0x2B3CDFAC)
                                    else if (proto.contains("Hysteria", ignoreCase = true)) Color(0x2B9D7BFF)
                                    else Color(0x2B4DAFFF)
                                )
                                .border(
                                    1.dp,
                                    if (proto.contains("Reality", ignoreCase = true)) Color(0x593CDFAC)
                                    else if (proto.contains("Hysteria", ignoreCase = true)) Color(0x599D7BFF)
                                    else Color(0x594DAFFF),
                                    RoundedCornerShape(5.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = proto,
                                color = if (proto.contains("Reality", ignoreCase = true)) Color(0xFF3CDFAC)
                                        else if (proto.contains("Hysteria", ignoreCase = true)) Color(0xFFC4B0FF)
                                        else Color(0xFF78C6FF),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = if (isRu) "Выбрано автоматически" else "Selected automatically",
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
