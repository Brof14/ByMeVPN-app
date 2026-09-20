package com.example.bymevpn.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.bymevpn.components.AccountIcon
import com.example.bymevpn.components.ChangeServerIcon
import com.example.bymevpn.components.ChatSupportIcon
import com.example.bymevpn.components.CountryFlagView
import com.example.bymevpn.components.FaqDialog
import com.example.bymevpn.components.GlobeIcon
import com.example.bymevpn.components.ShieldLogo
import com.example.bymevpn.components.TelegramLogoIcon
import com.example.bymevpn.data.AccountRepository
import com.example.bymevpn.data.LocaleManager
import com.example.bymevpn.data.PingTester
import com.example.bymevpn.data.api.ServerNode
import com.example.bymevpn.data.subscription.SubscriptionManager
import com.example.bymevpn.vpn.VpnConnectionState
import com.example.bymevpn.vpn.VpnManager
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun HomeScreen(
    onNavigateToAccount: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onLogOut: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentLanguage by LocaleManager.currentLanguage.collectAsState()
    val isRu = remember(currentLanguage) { LocaleManager.isRussian(language = currentLanguage) }
    val scope = rememberCoroutineScope()

    val vpnManager = remember { VpnManager.getInstance(context) }
    val vpnState by vpnManager.state.collectAsState()
    val currentServer by vpnManager.currentServer.collectAsState()
    val availableServers by vpnManager.availableServers.collectAsState()
    val connectedSeconds by vpnManager.connectedSeconds.collectAsState()
    val bytesIn by vpnManager.bytesIn.collectAsState()
    val bytesOut by vpnManager.bytesOut.collectAsState()
    val errorMessage by vpnManager.errorMessage.collectAsState()

    val subManager = remember { SubscriptionManager.getInstance(context) }
    val subscription by subManager.subscriptionStatus.collectAsState()

    var showServerPicker by remember { mutableStateOf(false) }
    var showTrialBanner by remember { mutableStateOf(false) }
    var showFaqDialog by remember { mutableStateOf(false) }

    var measuredPings by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var isMeasuringPing by remember { mutableStateOf(false) }

    val isConnected = vpnState == VpnConnectionState.CONNECTED
    val isConnecting = vpnState == VpnConnectionState.CONNECTING

    // Load servers from API on launch
    LaunchedEffect(Unit) {
        vpnManager.loadServers()
        subManager.refreshStatus()
    }

    // Android VpnService permission launcher
    val vpnPrepareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            VpnManager.startVpn(context)
        } else {
            Toast.makeText(context, if (isRu) "Требуется разрешение VPN" else "VPN permission required", Toast.LENGTH_SHORT).show()
        }
    }

    // Notification permission launcher
    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF060B17),
        bottomBar = {
            // Modern bottom bar with Servers, Settings, and Account
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0C162A))
                    .border(width = 1.dp, color = Color(0xFF1E355B))
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Servers Tab (Active on Home)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { /* Already on Home */ }
                ) {
                    GlobeIcon(color = Color(0xFF26E875), size = 24.dp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isRu) "Серверы" else "Servers",
                        color = Color(0xFF26E875),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Settings Tab
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onNavigateToSettings() }
                ) {
                    Text("⚙", color = Color(0xFF94A3B8), fontSize = 22.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isRu) "Настройки" else "Settings",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Account Tab
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onNavigateToAccount() }
                ) {
                    AccountIcon(color = Color(0xFF94A3B8), size = 24.dp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isRu) "Аккаунт" else "Account",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Top Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ByMeVPN Logo + App Name
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ShieldLogo(
                        size = 28.dp,
                        isActive = true,
                        glowAlpha = 1.0f
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = Color.White)) {
                                append("ByMe")
                            }
                            withStyle(SpanStyle(color = Color(0xFF26E875))) {
                                append("VPN")
                            }
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Top right buttons: Chat support bot first, then Telegram channel
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Chat support bot: https://t.me/ByMeVPNSupportBot
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0C162A))
                            .border(1.2.dp, Color(0xFF1E355B), CircleShape)
                            .clickable {
                                openExternalUrl(context, "https://t.me/ByMeVPNSupportBot")
                            }
                            .testTag("support_bot_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        ChatSupportIcon(size = 20.dp, color = Color(0xFF00D4FF))
                    }

                    // Official Telegram channel: https://t.me/ByMeVPN
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0C162A))
                            .border(1.2.dp, Color(0xFF1E355B), CircleShape)
                            .clickable {
                                openExternalUrl(context, "https://t.me/ByMeVPN")
                            }
                            .testTag("telegram_channel_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        TelegramLogoIcon(size = 19.dp, color = Color(0xFF2AABEE))
                    }
                }
            }

            // Central Interactive & Status Section (Centered and scrollable on compact screens)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // Error Banner when connection fails
            if (!errorMessage.isNullOrBlank()) {
                val isSubError = errorMessage == "SUBSCRIPTION_REQUIRED"
                val displayMsg = when {
                    isSubError -> if (isRu) "Требуется активная подписка для подключения" else "Active subscription required to connect"
                    else -> errorMessage ?: ""
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF2E0F14))
                        .border(1.2.dp, Color(0xFFFF5252), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⚠️", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isRu) "Ошибка подключения" else "Connection Error",
                                    color = Color(0xFFFF5252),
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = displayMsg,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 12.sp,
                                    maxLines = 3
                                )
                            }
                        }
                        Text(
                            text = "✕",
                            color = Color(0xFF94A3B8),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { vpnManager.clearErrorMessage() }
                                .padding(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSubError) {
                            Text(
                                text = if (isRu) "Оформить подписку →" else "Get Subscription →",
                                color = Color(0xFF00D4FF),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    vpnManager.clearErrorMessage()
                                    onNavigateToAccount()
                                }
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF162A4E))
                                    .clickable {
                                        vpnManager.clearErrorMessage()
                                        vpnManager.connect()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (isRu) "↻ Повторить" else "↻ Retry",
                                    color = Color(0xFF00D4FF),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Trial Reminder Banner if available
            if (subscription?.trialAvailable == true && (subscription?.status == "none" || subscription?.status == "expired")) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF142442))
                        .border(1.dp, Color(0xFF00D4FF), RoundedCornerShape(14.dp))
                        .clickable { onNavigateToAccount() }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🎁", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isRu) "Попробуйте 3 дня бесплатно!" else "Try 3 days for free!",
                                color = Color.White,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isRu) "Активируйте в аккаунте" else "Activate in your account",
                                color = Color(0xFF00D4FF),
                                fontSize = 11.5.sp
                            )
                        }
                    }
                    Text("→", color = Color(0xFF00D4FF), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Shield Logo as the Main Interactive Power Button
            val shieldPulseTransition = rememberInfiniteTransition(label = "shield_pulse")
            val pulseGlow by shieldPulseTransition.animateFloat(
                initialValue = 0.55f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse_glow"
            )

            val logoInteractionSource = remember { MutableInteractionSource() }

            Box(
                modifier = Modifier
                    .size(210.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = if (isConnected) {
                                listOf(
                                    Color(0xFF26E875).copy(alpha = 0.28f),
                                    Color(0xFF00D4FF).copy(alpha = 0.10f),
                                    Color.Transparent
                                )
                            } else if (isConnecting) {
                                listOf(
                                    Color(0xFF00D4FF).copy(alpha = 0.35f * pulseGlow),
                                    Color.Transparent
                                )
                            } else {
                                listOf(
                                    Color(0xFF132B4F).copy(alpha = 0.35f),
                                    Color.Transparent
                                )
                            }
                        )
                    )
                    .border(
                        width = if (isConnected) 2.dp else 1.5.dp,
                        brush = Brush.sweepGradient(
                            if (isConnected) listOf(Color(0xFF26E875), Color(0xFF00D4FF), Color(0xFF26E875))
                            else if (isConnecting) listOf(Color(0xFF00D4FF), Color(0xFF1E355B), Color(0xFF00D4FF))
                            else listOf(Color(0xFF1E355B), Color(0xFF0E1A2E), Color(0xFF1E355B))
                        ),
                        shape = CircleShape
                    )
                    .clickable(
                        interactionSource = logoInteractionSource,
                        indication = null
                    ) {
                        if (isConnected) {
                            vpnManager.disconnect()
                        } else if (isConnecting) {
                            vpnManager.disconnect()
                        } else {
                            val prepIntent = VpnManager.getVpnPrepareIntent(context)
                            if (prepIntent != null) {
                                vpnPrepareLauncher.launch(prepIntent)
                            } else {
                                VpnManager.startVpn(context)
                            }
                        }
                    }
                    .testTag("shield_logo_toggle"),
                contentAlignment = Alignment.Center
            ) {
                ShieldLogo(
                    size = 145.dp,
                    isActive = isConnected,
                    pulseAlpha = if (isConnecting) pulseGlow else 1.0f,
                    glowAlpha = if (isConnected) 1.0f else 0.45f
                )

                if (isConnecting) {
                    CircularProgressIndicator(
                        color = Color(0xFF00D4FF),
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(190.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Status indicator label (NO technical VLESS / Reality mentioned)
            val isEngineBlocked = errorMessage?.contains("VPN-движок недоступен", ignoreCase = true) == true ||
                errorMessage?.contains("VPN engine unavailable", ignoreCase = true) == true
            val isVpnUnavailable = errorMessage?.contains("недоступен", ignoreCase = true) == true ||
                errorMessage?.contains("unavailable", ignoreCase = true) == true ||
                errorMessage?.contains("not configured", ignoreCase = true) == true
            val statusColor = when {
                isConnected -> Color(0xFF26E875)
                isConnecting -> Color(0xFF00D4FF)
                vpnState == VpnConnectionState.ERROR -> Color(0xFFFF5252)
                else -> Color(0xFF94A3B8)
            }
            val statusText = when {
                isConnected -> if (isRu) "ЗАЩИТА ВКЛЮЧЕНА" else "PROTECTED"
                isConnecting -> if (isRu) "ПОДКЛЮЧЕНИЕ..." else "CONNECTING..."
                vpnState == VpnConnectionState.ERROR -> {
                    when {
                        isVpnUnavailable -> if (isRu) "VPN НЕДОСТУПЕН" else "VPN UNAVAILABLE"
                        isEngineBlocked -> if (isRu) "ДВИЖОК НЕДОСТУПЕН" else "ENGINE UNAVAILABLE"
                        else -> if (isRu) "ОШИБКА ПОДКЛЮЧЕНИЯ" else "CONNECTION ERROR"
                    }
                }
                else -> if (isRu) "ОТКЛЮЧЕНО" else "DISCONNECTED"
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = statusText,
                    color = statusColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Tap hint under the logo
            val tapHint = when {
                isConnected -> if (isRu) "Нажмите на логотип для отключения" else "Tap logo to disconnect"
                isConnecting -> if (isRu) "Устанавливаем безопасное соединение..." else "Securing connection..."
                vpnState == VpnConnectionState.ERROR -> if (isRu) "Нажмите на логотип для повторной попытки" else "Tap logo to retry"
                else -> if (isRu) "Нажмите на логотип для включения" else "Tap logo to connect"
            }
            Text(
                text = tapHint,
                color = if (isConnected) Color(0xFF86EFAC) else Color(0xFF94A3B8),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            // Connection Duration & Traffic Stats
            if (isConnected) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = formatDuration(connectedSeconds),
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "↓ ${formatBytes(bytesIn)}",
                        color = Color(0xFF26E875),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "↑ ${formatBytes(bytesOut)}",
                        color = Color(0xFF00D4FF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isRu) "Ваш IP и трафик не защищены" else "Your IP and traffic are unprotected",
                    color = Color(0xFF64748B),
                    fontSize = 12.5.sp
                )
            }

            }

            // Lower Section: Selected Server Card (Cleanly styled and positioned lower down)
            Spacer(modifier = Modifier.height(10.dp))
            val server = currentServer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF0C162A))
                    .border(1.2.dp, Color(0xFF1E355B), RoundedCornerShape(18.dp))
                    .clickable { showServerPicker = true }
                    .padding(horizontal = 16.dp, vertical = 13.dp)
                    .testTag("current_server_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = server?.flag ?: "🌐", fontSize = 26.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isRu) "Сервер подключения" else "Selected Server",
                                color = Color(0xFF64748B),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (server != null) {
                                    "${server.localizedCountry(isRu)} (${server.localizedCity(isRu)})"
                                } else {
                                    if (isRu) "Сервер не выбран" else "No server selected"
                                },
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF162A4E))
                            .border(1.dp, Color(0xFF00D4FF).copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isRu) "Сменить" else "Change",
                                color = Color(0xFF00D4FF),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "⇄",
                                color = Color(0xFF00D4FF),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }
    }

    // Server Selection Modal
    if (showServerPicker) {
        Dialog(onDismissRequest = { showServerPicker = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF0C162A))
                    .border(1.2.dp, Color(0xFF1E355B), RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isRu) "Выберите VPN сервер" else "Select VPN Server",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "✕",
                            color = Color(0xFF94A3B8),
                            fontSize = 18.sp,
                            modifier = Modifier
                                .clickable { showServerPicker = false }
                                .padding(4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Dedicated button to test real ping on demand (Zero battery waste)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF13223D))
                            .border(1.dp, Color(0xFF1E355B), RoundedCornerShape(12.dp))
                            .clickable(enabled = !isMeasuringPing) {
                                scope.launch {
                                    isMeasuringPing = true
                                    val newPings = mutableMapOf<String, Int>()
                                    availableServers.forEach { s ->
                                        val latency = PingTester.measureRealPing(s.countryCode)
                                        if (latency > 0) {
                                            newPings[s.nodeCode] = latency
                                        }
                                    }
                                    measuredPings = newPings
                                    isMeasuringPing = false
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚡", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isMeasuringPing) {
                                    if (isRu) "Измерение задержки..." else "Measuring ping..."
                                } else {
                                    if (isRu) "Измерить пинг серверов" else "Measure server ping"
                                },
                                color = if (isMeasuringPing) Color(0xFF00D4FF) else Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (isMeasuringPing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color(0xFF00D4FF),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (isRu) "Измерить" else "Measure",
                                color = Color(0xFF00D4FF),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (availableServers.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (isRu) "Список серверов загружается или недоступен" else "Servers list is loading or unavailable",
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF162A4E))
                                    .clickable {
                                        scope.launch { vpnManager.loadServers() }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = if (isRu) "Повторить попытку" else "Retry",
                                    color = Color(0xFF00D4FF),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                            items(availableServers) { node ->
                                val isSelected = currentServer?.nodeCode == node.nodeCode
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) Color(0xFF162A4E) else Color(0xFF091222))
                                        .border(
                                            1.dp,
                                            if (isSelected) Color(0xFF00D4FF) else Color(0xFF142442),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            vpnManager.selectServer(node)
                                            showServerPicker = false
                                            if (isConnected) {
                                                vpnManager.disconnect()
                                                VpnManager.startVpn(context, node)
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(text = node.flag, fontSize = 24.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "${node.localizedCountry(isRu)} (${node.localizedCity(isRu)})",
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            if (isSelected) {
                                                Text(
                                                    text = if (isRu) "Выбранный сервер" else "Selected server",
                                                    color = Color(0xFF00D4FF),
                                                    fontSize = 11.5.sp
                                                )
                                            }
                                        }
                                    }

                                    val realPing = measuredPings[node.nodeCode]
                                    if (realPing != null) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF0D1B33))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "$realPing ms",
                                                color = if (realPing < 45) Color(0xFF26E875) else Color(0xFF00D4FF),
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    } else if (isSelected) {
                                        Text(
                                            text = "✓",
                                            color = Color(0xFF00D4FF),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // FAQ Dialog Modal
    if (showFaqDialog) {
        FaqDialog(onDismiss = { showFaqDialog = false })
    }
}

private fun formatDuration(totalSecs: Long): String {
    val hours = totalSecs / 3600
    val minutes = (totalSecs % 3600) / 60
    val secs = totalSecs % 60
    return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, secs)
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(Locale.US, "%.1f GB", gb)
        mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
        kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
        else -> "$bytes B"
    }
}

private fun openExternalUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        val isRu = LocaleManager.isRussian(context)
        Toast.makeText(context, if (isRu) "Не удалось открыть ссылку" else "Unable to open link", Toast.LENGTH_SHORT).show()
    }
}

