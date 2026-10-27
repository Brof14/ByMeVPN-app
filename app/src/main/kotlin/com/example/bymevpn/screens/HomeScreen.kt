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
import com.example.bymevpn.data.subscription.SubscriptionManager
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
    val scope = rememberCoroutineScope()

    val vpnManager = remember { VpnManager.getInstance(context) }
    val vpnState by vpnManager.state.collectAsState()
    val currentServer by vpnManager.currentServer.collectAsState()
    val availableServers by vpnManager.availableServers.collectAsState()
    val connectedSeconds by vpnManager.connectedSeconds.collectAsState()

    val subManager = remember { SubscriptionManager.getInstance(context) }
    val subscription by subManager.subscriptionStatus.collectAsState()

    val hasPlan = subscription?.isActive == true
    val remainingDays = subscription?.daysRemaining ?: 0

    var showServerSheet by remember { mutableStateOf(false) }
    var isAutoServerSelected by remember { mutableStateOf(true) }
    var showFaqDialog by remember { mutableStateOf(false) }

    // VPN Permission Launcher
    val vpnPrepareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            VpnManager.startVpn(context, currentServer)
        } else {
            Toast.makeText(context, "Требуется разрешение для создания VPN-туннеля", Toast.LENGTH_SHORT).show()
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
                onMoreClick = { /* More actions menu */ }
            )

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
                        onPlansClick = onNavigateToPricing
                    )
                } else if (vpnState == VpnConnectionState.ERROR) {
                    // MOCKUP 14_connection_error
                    HomeConnectionErrorContent(
                        onRetry = { toggleVpn() },
                        onSupportClick = onNavigateToSupport,
                        onServerSelectClick = { showServerSheet = true },
                        currentServer = currentServer
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
private fun HomeNoPlanContent(
    onTrialClick: () -> Unit,
    onPlansClick: () -> Unit
) {
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
            text = "Свободный интернет.\nОдно касание.",
            color = AppColors.TextPrimary,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 36.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Подключайтесь к привычным сервисам с ByMeVPN — без сложных настроек.",
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
                    text = "Простое подключение",
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
                    text = "Прозрачные условия",
                    color = Color(0xFF89A99E),
                    fontSize = 11.sp
                )
            }
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
                        text = "Для новых пользователей",
                        color = Color(0xFFA0C2AE),
                        fontSize = 10.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "3 дня за ",
                        color = AppColors.TextPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        text = "1 ₽",
                        color = Color(0xFF43E0AC),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-1).sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Попробуйте сервис перед покупкой.",
                    color = Color(0xFFA1B4AD),
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                ByMePrimaryButton(
                    text = "Попробовать за 1 ₽",
                    onClick = onTrialClick,
                    height = 50.dp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Далее — 89 ₽ за 30 дней при автопродлении. Условия и отмена — до оплаты.",
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
                    text = "Посмотреть все тарифы",
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
    remainingDays: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 25.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "ВАШЕ ПОДКЛЮЧЕНИЕ",
            color = Color(0xFF78999A),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(26.dp))

        // Giant Power Control (Off State)
        Box(
            modifier = Modifier
                .size(235.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        0.0f to Color(0xFF102421),
                        0.54f to Color(0xFF0F1A1A),
                        0.70f to Color.Transparent
                    )
                )
                .border(1.dp, Color(0xFF223739), CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onPowerClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(195.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0D181A))
                    .border(2.dp, Color(0xFF2A4141), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(134.dp)
                        .clip(CircleShape)
                        .background(AppGradients.PowerButtonInactive),
                    contentAlignment = Alignment.Center
                ) {
                    PowerIcon(
                        color = if (isConnecting) Color(0xFF40E4B2) else Color(0xFF7E9B9C),
                        size = 45.dp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = if (isConnecting) "Подключение..." else "VPN выключен",
            color = AppColors.TextPrimary,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isConnecting) "Устанавливаем защищенный туннель" else "Нажмите, чтобы подключиться",
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
                        text = "МЕСТОПОЛОЖЕНИЕ",
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
                            countryCode = currentServer?.countryCode ?: "DE",
                            size = 24.dp
                        )
                        Text(
                            text = currentServer?.name ?: "Германия",
                            color = AppColors.TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Выбрано автоматически",
                        color = Color(0xFF81999B),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(start = 34.dp)
                    )
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
                        text = "Подписка активна",
                        color = Color(0xFFEDF5F2),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Осталось $remainingDays дней · 2 устройства",
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
    remainingDays: Int
) {
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
            text = "ВАШЕ ПОДКЛЮЧЕНИЕ",
            color = Color(0xFF78999A),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(26.dp))

        // Giant Power Control (On State with Mint Glow Ring)
        Box(
            modifier = Modifier
                .size(235.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        0.0f to Color(0xFF123C34),
                        0.54f to Color(0xFF102620),
                        0.70f to Color.Transparent
                    )
                )
                .border(1.dp, Color(0xFF285A4C), CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onPowerClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(195.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0D181A))
                    .border(3.dp, Color(0xFF39DFA8), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(134.dp)
                        .clip(CircleShape)
                        .background(AppGradients.PowerButtonActive),
                    contentAlignment = Alignment.Center
                ) {
                    PowerIcon(color = Color(0xFF40E4B2), size = 45.dp)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CheckmarkIcon(color = Color(0xFF3CDCAD), size = 22.dp)
            Text(
                text = "VPN подключён",
                color = AppColors.TextPrimary,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Соединение защищено",
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
                        text = "МЕСТОПОЛОЖЕНИЕ",
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
                            countryCode = currentServer?.countryCode ?: "DE",
                            size = 24.dp
                        )
                        Text(
                            text = currentServer?.name ?: "Германия",
                            color = AppColors.TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Сервер 1 · Подключено",
                        color = Color(0xFF81999B),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(start = 34.dp)
                    )
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
                        text = "Подписка активна",
                        color = Color(0xFFEDF5F2),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Осталось $remainingDays дней · 2 устройства",
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
    currentServer: ServerNode?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 25.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "ВАШЕ ПОДКЛЮЧЕНИЕ",
            color = Color(0xFF78999A),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(26.dp))

        // Power Control in Off State
        Box(
            modifier = Modifier
                .size(235.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        0.0f to Color(0xFF102421),
                        0.54f to Color(0xFF0F1A1A),
                        0.70f to Color.Transparent
                    )
                )
                .border(1.dp, Color(0xFF223739), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(195.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0D181A))
                    .border(2.dp, Color(0xFF2A4141), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(134.dp)
                        .clip(CircleShape)
                        .background(AppGradients.PowerButtonInactive),
                    contentAlignment = Alignment.Center
                ) {
                    PowerIcon(color = Color(0xFF7E9B9C), size = 45.dp)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Не удалось подключиться",
            color = AppColors.TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Проверьте интернет и попробуйте ещё раз",
            color = Color(0xFF859B9D),
            fontSize = 13.sp
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
                text = "Повторить",
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
                        text = "ВЫБРАННЫЙ СЕРВЕР",
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
                            countryCode = currentServer?.countryCode ?: "DE",
                            size = 24.dp
                        )
                        Text(
                            text = currentServer?.name ?: "Германия",
                            color = AppColors.TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Автоматический выбор",
                        color = Color(0xFF81999B),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(start = 34.dp)
                    )
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
                        text = "Не получается подключиться?",
                        color = Color(0xFFEDF5F2),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Напишите нам — поможем разобраться",
                        color = Color(0xFF8AA1A1),
                        fontSize = 11.sp
                    )
                }

                ChevronRightIcon(color = Color(0xFF73898A), size = 17.dp)
            }
        }
    }
}
