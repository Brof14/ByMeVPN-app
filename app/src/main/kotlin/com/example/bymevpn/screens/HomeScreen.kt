package com.example.bymevpn.screens

import android.app.Activity
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.bymevpn.components.AccountIcon
import com.example.bymevpn.components.ChangeServerIcon
import com.example.bymevpn.components.CountryFlagView
import com.example.bymevpn.components.GlobeIcon
import com.example.bymevpn.components.ShieldLogo
import com.example.bymevpn.data.AccountRepository
import com.example.bymevpn.data.LocaleManager
import com.example.bymevpn.data.api.ServerNode
import com.example.bymevpn.data.subscription.SubscriptionManager
import com.example.bymevpn.vpn.VpnConnectionState
import com.example.bymevpn.vpn.VpnManager
import com.example.bymevpn.vpn.WireGuardVpnManager
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
    val isRu = LocaleManager.isRussian(context)
    val scope = rememberCoroutineScope()

    val vpnManager = remember { WireGuardVpnManager.getInstance(context) }
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Error Banner when connection fails
            if (!errorMessage.isNullOrBlank()) {
                val isSubError = errorMessage == "SUBSCRIPTION_REQUIRED"
                val displayMsg = when {
                    isSubError -> if (isRu) "Требуется активная подписка для подключения" else "Active subscription required to connect"
                    else -> errorMessage ?: ""
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF2E0F14))
                        .border(1.2.dp, Color(0xFFFF5252), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
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
                            if (isSubError) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isRu) "Оформить подписку →" else "Get Subscription →",
                                    color = Color(0xFF00D4FF),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable {
                                        vpnManager.clearErrorMessage()
                                        onNavigateToAccount()
                                    }
                                )
                            }
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

            // Shield Logo Display with connection animations
            val shieldPulseTransition = rememberInfiniteTransition(label = "shield_pulse")
            val pulseGlow by shieldPulseTransition.animateFloat(
                initialValue = 0.5f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse_glow"
            )

            ShieldLogo(
                size = 175.dp,
                isActive = isConnected,
                pulseAlpha = if (isConnecting) pulseGlow else 1.0f,
                glowAlpha = if (isConnected) 1.0f else 0.4f
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Status indicator label
            val statusColor = when {
                isConnected -> Color(0xFF26E875)
                isConnecting -> Color(0xFF00D4FF)
                vpnState == VpnConnectionState.ERROR -> Color(0xFFFF5252)
                else -> Color(0xFF94A3B8)
            }
            val statusText = when {
                isConnected -> if (isRu) "ПОДКЛЮЧЕНО (WireGuard)" else "CONNECTED (WireGuard)"
                isConnecting -> if (isRu) "ПОДКЛЮЧЕНИЕ..." else "CONNECTING..."
                vpnState == VpnConnectionState.ERROR -> if (isRu) "ОШИБКА ПОДКЛЮЧЕНИЯ" else "CONNECTION ERROR"
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
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp
                )
            }

            // Connection Duration & Traffic Stats
            if (isConnected) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = formatDuration(connectedSeconds),
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "↓ ${formatBytes(bytesIn)}",
                        color = Color(0xFF26E875),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "↑ ${formatBytes(bytesOut)}",
                        color = Color(0xFF00D4FF),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isRu) "Ваш IP и трафик не защищены" else "Your IP and traffic are unprotected",
                    color = Color(0xFF64748B),
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Main Connect/Disconnect Button
            val buttonColor by animateColorAsState(
                targetValue = if (isConnected) Color(0xFFE53935) else Color(0xFF26E875),
                animationSpec = spring(),
                label = "btn_color"
            )

            Box(
                modifier = Modifier
                    .size(130.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                buttonColor.copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (isConnected) Color(0xFF1E0E14) else Color(0xFF081F17)
                    )
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(
                            listOf(buttonColor, Color(0xFF00D4FF))
                        ),
                        shape = CircleShape
                    )
                    .clickable {
                        if (isConnected) {
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
                    .testTag("vpn_toggle_button"),
                contentAlignment = Alignment.Center
            ) {
                if (isConnecting) {
                    CircularProgressIndicator(color = Color(0xFF00D4FF), modifier = Modifier.size(44.dp))
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isConnected) "STOP" else "START",
                            color = if (isConnected) Color(0xFFFF6B6B) else Color(0xFF26E875),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Selected Server Card
            val server = currentServer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF0C162A))
                    .border(1.dp, Color(0xFF1E355B), RoundedCornerShape(18.dp))
                    .clickable { showServerPicker = true }
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = server?.flag ?: "🌐", fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "${server?.country ?: "Global"} (${server?.city ?: "Auto"})",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Ping: ${server?.pingMs ?: 25} ms • Загрузка: ${server?.loadPercent ?: 30}%",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF13223D))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isRu) "Сменить" else "Change",
                            color = Color(0xFF00D4FF),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
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

                    Spacer(modifier = Modifier.height(14.dp))

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
                        LazyColumn(modifier = Modifier.fillMaxWidth().height(300.dp)) {
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
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = node.flag, fontSize = 22.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(text = "${node.country} (${node.city})", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                            Text(text = "${node.loadPercent}% load", color = Color(0xFF64748B), fontSize = 11.5.sp)
                                        }
                                    }
                                    Text(
                                        text = "${node.pingMs} ms",
                                        color = if (node.pingMs < 35) Color(0xFF26E875) else Color(0xFF00D4FF),
                                        fontSize = 12.5.sp,
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
