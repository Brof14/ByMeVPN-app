package com.example.bymevpn.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.bymevpn.components.AccountIcon
import com.example.bymevpn.components.ChatSupportIcon
import com.example.bymevpn.components.GlobeIcon
import com.example.bymevpn.components.LiveSpeedWave
import com.example.bymevpn.components.ShieldLogo
import com.example.bymevpn.components.TelegramLogoIcon
import com.example.bymevpn.data.ClipboardKeyDetector
import com.example.bymevpn.data.LocaleManager
import com.example.bymevpn.data.PingTester
import com.example.bymevpn.data.api.ServerNode
import com.example.bymevpn.data.storage.SecureTokenStorage
import com.example.bymevpn.data.subscription.SubscriptionManager
import com.example.bymevpn.theme.AppColors
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
    val haptic = LocalHapticFeedback.current
    val currentLanguage by LocaleManager.currentLanguage.collectAsState()
    val isRu = remember(currentLanguage) { LocaleManager.isRussian(language = currentLanguage) }
    val scope = rememberCoroutineScope()
    val storage = remember { SecureTokenStorage.getInstance(context) }

    val vpnManager = remember { VpnManager.getInstance(context) }
    val vpnState by vpnManager.state.collectAsState()
    val currentServer by vpnManager.currentServer.collectAsState()
    val availableServers by vpnManager.availableServers.collectAsState()
    val connectedSeconds by vpnManager.connectedSeconds.collectAsState()
    val bytesIn by vpnManager.bytesIn.collectAsState()
    val bytesOut by vpnManager.bytesOut.collectAsState()
    val downloadSpeedBps by vpnManager.downloadSpeedBps.collectAsState()
    val uploadSpeedBps by vpnManager.uploadSpeedBps.collectAsState()
    val errorMessage by vpnManager.errorMessage.collectAsState()

    val subManager = remember { SubscriptionManager.getInstance(context) }
    val subscription by subManager.subscriptionStatus.collectAsState()

    var showServerPicker by remember { mutableStateOf(false) }
    var measuredPings by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var isMeasuringPing by remember { mutableStateOf(false) }
    var serverSearchQuery by remember { mutableStateOf("") }
    var favoriteServerCodes by remember { mutableStateOf(storage.getFavoriteServers()) }
    var isIpMasked by remember { mutableStateOf(true) }

    // Clipboard key detection
    var detectedClipboardKey by remember { mutableStateOf<String?>(null) }
    var isImportingClipboard by remember { mutableStateOf(false) }

    val isConnected = vpnState == VpnConnectionState.CONNECTED
    val isConnecting = vpnState == VpnConnectionState.CONNECTING

    LaunchedEffect(Unit) {
        vpnManager.syncWithServiceState()
        vpnManager.loadServers()
        subManager.refreshStatus()
        detectedClipboardKey = ClipboardKeyDetector.detectKey(context)
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

    // Tactile confirmation when connected
    var previousConnected by remember { mutableStateOf(false) }
    LaunchedEffect(isConnected) {
        if (isConnected && !previousConnected) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        previousConnected = isConnected
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AppColors.BgCanvas,
        bottomBar = {
            // Figma Studio Bottom Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.CardBg)
                    .border(width = 1.dp, color = AppColors.CardBorder)
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // VPN Tab (Active)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { /* Already on Home */ }
                ) {
                    GlobeIcon(color = AppColors.BrandGreen, size = 24.dp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isRu) "Подключение" else "VPN",
                        color = AppColors.BrandGreen,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Settings Tab
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onNavigateToSettings() }
                ) {
                    Text("⚙", color = AppColors.TextSecondary, fontSize = 22.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isRu) "Настройки" else "Settings",
                        color = AppColors.TextSecondary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Account Tab
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onNavigateToAccount() }
                ) {
                    AccountIcon(color = AppColors.TextSecondary, size = 24.dp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isRu) "Аккаунт" else "Account",
                        color = AppColors.TextSecondary,
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
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // 1. Top Header Bar (ByMeVPN brand + Support buttons)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ShieldLogo(
                        size = 28.dp,
                        isActive = isConnected,
                        glowAlpha = if (isConnected) 1.0f else 0.4f
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = Color.White)) {
                                append("ByMe")
                            }
                            withStyle(SpanStyle(color = AppColors.BrandGreen)) {
                                append("VPN")
                            }
                        },
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Support Bot
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(AppColors.CardBg)
                            .border(1.dp, AppColors.CardBorder, CircleShape)
                            .clickable {
                                openExternalUrl(context, "https://t.me/ByMeVPNSupportBot")
                            }
                            .testTag("support_bot_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        ChatSupportIcon(size = 20.dp, color = AppColors.SkyBlue)
                    }

                    // Official Telegram Channel
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(AppColors.CardBg)
                            .border(1.dp, AppColors.CardBorder, CircleShape)
                            .clickable {
                                openExternalUrl(context, "https://t.me/ByMeVPN")
                            }
                            .testTag("telegram_channel_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        TelegramLogoIcon(size = 19.dp, color = AppColors.TelegramBlue)
                    }
                }
            }

            // Clipboard Key Import Banner (Auto-detected from Telegram)
            AnimatedVisibility(
                visible = detectedClipboardKey != null,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                detectedClipboardKey?.let { rawKey ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(AppColors.CardBgElevated)
                            .border(1.2.dp, AppColors.BrandGreen, RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
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
                                Text("📋", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (isRu) "Обнаружен ключ ByMeVPN" else "ByMeVPN link detected",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isRu) "В буфере обмена найдена ссылка из бота" else "Link from Telegram bot found in clipboard",
                                        color = AppColors.TextSecondary,
                                        fontSize = 11.5.sp
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AppColors.BrandGreen)
                                        .clickable(enabled = !isImportingClipboard) {
                                            isImportingClipboard = true
                                            scope.launch {
                                                val res = subManager.applySubscriptionUrl(rawKey)
                                                isImportingClipboard = false
                                                if (res.isSuccess) {
                                                    vpnManager.loadServers()
                                                    detectedClipboardKey = null
                                                    Toast.makeText(context, if (isRu) "Подписка успешно подключена!" else "Subscription applied!", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, res.exceptionOrNull()?.message ?: "Ошибка", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = if (isRu) "Подключить" else "Apply",
                                        color = AppColors.DarkText,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = "✕",
                                    color = AppColors.TextMuted,
                                    fontSize = 16.sp,
                                    modifier = Modifier
                                        .clickable {
                                            ClipboardKeyDetector.dismissKey(context, rawKey)
                                            detectedClipboardKey = null
                                        }
                                        .padding(4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Central Scrollable Section
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Connection Error Banner
                if (!errorMessage.isNullOrBlank()) {
                    val isSubError = errorMessage == "SUBSCRIPTION_REQUIRED"
                    val displayMsg = when {
                        isSubError -> if (isRu) "Требуется активная подписка для подключения" else "Active subscription required to connect"
                        else -> errorMessage ?: ""
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF220A0E))
                            .border(1.2.dp, AppColors.StatusError, RoundedCornerShape(16.dp))
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
                                        color = AppColors.StatusError,
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
                                color = AppColors.TextSecondary,
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
                                    color = AppColors.BrandGreen,
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
                                        .background(AppColors.CardBgElevated)
                                        .border(1.dp, AppColors.CardBorder, RoundedCornerShape(8.dp))
                                        .clickable {
                                            vpnManager.clearErrorMessage()
                                            vpnManager.connect()
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = if (isRu) "↻ Повторить" else "↻ Retry",
                                        color = AppColors.BrandGreen,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Interactive Shield Power Button with tactile haptics
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
                        .size(215.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = if (isConnected) {
                                    listOf(
                                        AppColors.BrandGreen.copy(alpha = 0.22f),
                                        AppColors.ElectricBlue.copy(alpha = 0.08f),
                                        Color.Transparent
                                    )
                                } else if (isConnecting) {
                                    listOf(
                                        AppColors.SkyBlue.copy(alpha = 0.30f * pulseGlow),
                                        Color.Transparent
                                    )
                                } else {
                                    listOf(
                                        AppColors.CardBgElevated.copy(alpha = 0.40f),
                                        Color.Transparent
                                    )
                                }
                            )
                        )
                        .border(
                            width = if (isConnected) 2.dp else 1.5.dp,
                            brush = Brush.sweepGradient(
                                if (isConnected) listOf(AppColors.BrandGreen, AppColors.SkyBlue, AppColors.BrandGreen)
                                else if (isConnecting) listOf(AppColors.SkyBlue, AppColors.CardBorder, AppColors.SkyBlue)
                                else listOf(AppColors.CardBorder, AppColors.BgCanvas, AppColors.CardBorder)
                            ),
                            shape = CircleShape
                        )
                        .clickable(
                            interactionSource = logoInteractionSource,
                            indication = null
                        ) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)

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
                        size = 150.dp,
                        isActive = isConnected,
                        pulseAlpha = if (isConnecting) pulseGlow else 1.0f,
                        glowAlpha = if (isConnected) 1.0f else 0.45f
                    )

                    if (isConnecting) {
                        CircularProgressIndicator(
                            color = AppColors.SkyBlue,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(195.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Connection Status Headline
                val statusColor = when {
                    isConnected -> AppColors.BrandGreen
                    isConnecting -> AppColors.SkyBlue
                    vpnState == VpnConnectionState.ERROR -> AppColors.StatusError
                    else -> AppColors.TextSecondary
                }
                val statusHeadline = when {
                    isConnected -> if (isRu) "VPN подключён" else "VPN Connected"
                    isConnecting -> if (isRu) "Подключение..." else "Connecting..."
                    vpnState == VpnConnectionState.ERROR -> if (isRu) "Ошибка соединения" else "Connection Failed"
                    else -> if (isRu) "VPN выключен" else "VPN Disconnected"
                }

                val statusSubtext = when {
                    isConnected -> if (isRu) "Соединение защищено • ${formatDuration(connectedSeconds)}" else "Connection secured • ${formatDuration(connectedSeconds)}"
                    isConnecting -> if (isRu) "Устанавливаем безопасный туннель..." else "Securing connection..."
                    vpnState == VpnConnectionState.ERROR -> if (isRu) "Нажмите на щит для повтора" else "Tap shield to retry"
                    else -> if (isRu) "Нажмите на щит для включения" else "Tap shield to connect"
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
                        text = statusHeadline,
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = statusSubtext,
                    color = if (isConnected) AppColors.BrandGreenGlow else AppColors.TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                // Virtual IP Privacy Inspector (Only when connected)
                if (isConnected) {
                    Spacer(modifier = Modifier.height(10.dp))
                    val activeCfg = currentServer?.let { storage.getServerConfig(it.nodeCode) }
                    val rawIp = activeCfg?.server ?: "194.164.21.84"
                    val protoName = activeCfg?.protocol?.uppercase() ?: "VLESS"
                    val secName = activeCfg?.security?.uppercase() ?: "REALITY"

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(AppColors.CardBg)
                            .border(1.dp, AppColors.CardBorder, RoundedCornerShape(10.dp))
                            .clickable { isIpMasked = !isIpMasked }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🛡️", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$protoName $secName • IP: ${if (isIpMasked) maskIp(rawIp) else rawIp}",
                            color = AppColors.TextSecondary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isIpMasked) "👁" else "🔒",
                            fontSize = 12.sp
                        )
                    }
                }

                // Live Speed Wave Telemetry Graph (Real throughput)
                if (isConnected) {
                    Spacer(modifier = Modifier.height(14.dp))
                    LiveSpeedWave(
                        bytesIn = bytesIn,
                        bytesOut = bytesOut,
                        downloadBps = downloadSpeedBps,
                        uploadBps = uploadSpeedBps,
                        isConnected = isConnected
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // 3. Lower Cards: Selected Server Card & Active Subscription Card
            Spacer(modifier = Modifier.height(8.dp))

            // Selected Server Card
            val server = currentServer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(AppColors.CardBg)
                    .border(1.2.dp, AppColors.CardBorder, RoundedCornerShape(18.dp))
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
                        Text(text = server?.flag ?: "⚡", fontSize = 26.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isRu) "Сервер подключения" else "Selected Server",
                                color = AppColors.TextMuted,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (server != null) {
                                    "${server.localizedCountry(isRu)} (${server.localizedCity(isRu)})"
                                } else {
                                    if (isRu) "Автоматически (Лучший пинг)" else "Auto (Best latency)"
                                },
                                color = Color.White,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(AppColors.CardBgElevated)
                            .border(1.dp, AppColors.CardBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isRu) "Изменить" else "Change",
                                color = AppColors.BrandGreen,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "→",
                                color = AppColors.BrandGreen,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Active Subscription Card
            val isSubActive = subscription?.status == "active" || subscription?.status == "trial"
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(AppColors.CardBg)
                    .border(1.2.dp, AppColors.CardBorder, RoundedCornerShape(18.dp))
                    .clickable { onNavigateToAccount() }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
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
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isSubActive) AppColors.BrandGreen else AppColors.StatusWarning)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isSubActive) {
                                    if (isRu) "Доступ активен" else "Subscription Active"
                                } else {
                                    if (isRu) "Нет активной подписки" else "No Active Subscription"
                                },
                                color = Color.White,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            val expDate = subscription?.expiresAt ?: "24 ноября 2026"
                            val devicesCount = "${subscription?.activeDevices ?: 2} из ${subscription?.maxDevices ?: 5}"
                            Text(
                                text = if (isSubActive) {
                                    if (isRu) "До $expDate • $devicesCount устройств" else "Until $expDate • $devicesCount devices"
                                } else {
                                    if (isRu) "Оформите подписку для безлимита" else "Get a plan for unlimited access"
                                },
                                color = AppColors.TextSecondary,
                                fontSize = 11.5.sp
                            )
                        }
                    }

                    Text(
                        text = if (isSubActive) {
                            if (isRu) "Продлить →" else "Renew →"
                        } else {
                            if (isRu) "Оформить →" else "Get Plan →"
                        },
                        color = AppColors.BrandGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    // 4. Server Selection Modal (Figma Screen 06)
    if (showServerPicker) {
        Dialog(onDismissRequest = { showServerPicker = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(AppColors.CardBg)
                    .border(1.2.dp, AppColors.CardBorder, RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isRu) "Выбор сервера" else "Select Server",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "✕",
                                color = AppColors.TextSecondary,
                                fontSize = 18.sp,
                                modifier = Modifier
                                    .clickable { showServerPicker = false }
                                    .padding(4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Search field
                    BasicTextField(
                        value = serverSearchQuery,
                        onValueChange = { serverSearchQuery = it },
                        textStyle = TextStyle(color = Color.White, fontSize = 13.5.sp),
                        cursorBrush = SolidColor(AppColors.BrandGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppColors.InputBg)
                            .border(1.dp, AppColors.InputBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        decorationBox = { innerTextField ->
                            if (serverSearchQuery.isEmpty()) {
                                Text(
                                    text = if (isRu) "🔍 Найти сервер..." else "🔍 Search server...",
                                    color = AppColors.TextMuted,
                                    fontSize = 13.sp
                                )
                            }
                            innerTextField()
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Automatic fastest selection
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (currentServer == null) AppColors.CardBgElevated else AppColors.InputBg)
                            .border(
                                1.dp,
                                if (currentServer == null) AppColors.BrandGreen else AppColors.CardBorder,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                scope.launch {
                                    val fastest = vpnManager.autoSelectFastestServer()
                                    showServerPicker = false
                                    if (isConnected && fastest != null) {
                                        vpnManager.disconnect()
                                        VpnManager.startVpn(context, fastest)
                                    }
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚡", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isRu) "Автоматически" else "Automatic",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isRu) "Рекомендуется • лучший доступный пинг" else "Recommended • lowest latency",
                                    color = AppColors.TextSecondary,
                                    fontSize = 11.5.sp
                                )
                            }
                        }

                        if (currentServer == null) {
                            Text("✓", color = AppColors.BrandGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Measure All Pings in parallel (~350ms)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppColors.CardBgElevated)
                            .border(1.dp, AppColors.CardBorder, RoundedCornerShape(12.dp))
                            .clickable(enabled = !isMeasuringPing) {
                                scope.launch {
                                    isMeasuringPing = true
                                    val targets = availableServers.mapNotNull { s ->
                                        val cfg = storage.getServerConfig(s.nodeCode)
                                        if (cfg != null) s.nodeCode to Pair(cfg.server, cfg.port) else null
                                    }
                                    if (targets.isNotEmpty()) {
                                        measuredPings = PingTester.measureAllConcurrent(targets, timeoutMs = 1500)
                                    }
                                    isMeasuringPing = false
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📶", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isMeasuringPing) {
                                    if (isRu) "Быстрое измерение всех узлов..." else "Measuring latency..."
                                } else {
                                    if (isRu) "Замерить реальный пинг (в параллель)" else "Measure real latency"
                                },
                                color = if (isMeasuringPing) AppColors.SkyBlue else Color.White,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (isMeasuringPing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = AppColors.SkyBlue,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (isRu) "Замерить" else "Test",
                                color = AppColors.BrandGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val filteredServers = remember(availableServers, serverSearchQuery) {
                        if (serverSearchQuery.isBlank()) availableServers
                        else availableServers.filter {
                            it.country.contains(serverSearchQuery, ignoreCase = true) ||
                            it.city.contains(serverSearchQuery, ignoreCase = true) ||
                            it.nodeCode.contains(serverSearchQuery, ignoreCase = true)
                        }
                    }

                    val sortedServers = remember(filteredServers, favoriteServerCodes) {
                        filteredServers.sortedWith(
                            compareByDescending<ServerNode> { favoriteServerCodes.contains(it.nodeCode) }
                                .thenBy { it.localizedCountry(isRu) }
                        )
                    }

                    if (sortedServers.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (isRu) "Серверы не найдены" else "No servers found",
                                color = AppColors.TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                            items(sortedServers) { node ->
                                val isSelected = currentServer?.nodeCode == node.nodeCode
                                val isFav = favoriteServerCodes.contains(node.nodeCode)
                                val realPing = measuredPings[node.nodeCode]

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) AppColors.CardBgElevated else AppColors.InputBg)
                                        .border(
                                            1.dp,
                                            if (isSelected) AppColors.BrandGreen else AppColors.CardBorder,
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
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
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
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "${node.localizedCountry(isRu)} (${node.localizedCity(isRu)})",
                                                    color = Color.White,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                if (isFav) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("⭐", fontSize = 11.sp)
                                                }
                                            }
                                            if (isSelected) {
                                                Text(
                                                    text = if (isRu) "Выбранный сервер" else "Selected server",
                                                    color = AppColors.BrandGreen,
                                                    fontSize = 11.5.sp
                                                )
                                            }
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Star Toggle (Favorite)
                                        Text(
                                            text = if (isFav) "⭐" else "☆",
                                            fontSize = 16.sp,
                                            modifier = Modifier
                                                .clickable {
                                                    storage.toggleFavoriteServer(node.nodeCode)
                                                    favoriteServerCodes = storage.getFavoriteServers()
                                                }
                                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                        )

                                        Spacer(modifier = Modifier.width(4.dp))

                                        if (realPing != null) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(AppColors.CardBg)
                                                    .border(1.dp, AppColors.CardBorder, RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = "$realPing ms",
                                                    color = if (realPing < 50) AppColors.BrandGreen else if (realPing < 100) AppColors.SkyBlue else AppColors.StatusWarning,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        } else if (isSelected) {
                                            Text(
                                                text = "✓",
                                                color = AppColors.BrandGreen,
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
    }
}

private fun formatDuration(totalSecs: Long): String {
    val hours = totalSecs / 3600
    val minutes = (totalSecs % 3600) / 60
    val secs = totalSecs % 60
    return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, secs)
}

private fun maskIp(ip: String): String {
    val parts = ip.split(".")
    return if (parts.size == 4) "${parts[0]}.${parts[1]}.•••.•••" else "•••.•••.•••.•••"
}

private fun openExternalUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        val isRu = LocaleManager.isRussian(context)
        Toast.makeText(context, if (isRu) "Не удалось открыть ссылку" else "Unable to open link", Toast.LENGTH_SHORT).show()
    }
}
