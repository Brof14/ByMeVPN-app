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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.drawscope.Stroke
import android.content.Intent
import android.net.Uri
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
import com.example.bymevpn.components.GoogleLogoIcon
import com.example.bymevpn.components.ShieldLogo
import com.example.bymevpn.components.TelegramLogoIcon
import com.example.bymevpn.components.WebsiteLogoIcon
import com.example.bymevpn.components.YouTubeLogoIcon
import com.example.bymevpn.data.AccountRepository
import com.example.bymevpn.data.AppLanguage
import com.example.bymevpn.data.LocaleManager
import com.example.bymevpn.vpn.AVAILABLE_SERVERS
import com.example.bymevpn.vpn.VpnManager
import com.example.bymevpn.vpn.VpnServer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Main ByMeVPN connection dashboard matching the reference design 1:1,
 * wired to real native Android VpnService tunnel.
 */
@Composable
fun HomeScreen(
    userEmail: String = "mama.nikfjdj@gmail.com",
    isGoogleAuth: Boolean = true,
    onLogOut: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val currentLanguage by LocaleManager.currentLanguage.collectAsState()
    val isRu = LocaleManager.isRussian(context)

    val currentUserAccount by AccountRepository.currentUser.collectAsState()
    val displayEmail = currentUserAccount?.email ?: userEmail
    val displayGoogle = currentUserAccount?.isGoogle ?: isGoogleAuth

    val vpnSessionState by VpnManager.sessionState.collectAsState()
    val isConnected = vpnSessionState.isConnected
    val isConnecting = vpnSessionState.isConnecting
    val durationSeconds = vpnSessionState.durationSeconds
    val selectedServer = vpnSessionState.selectedServer

    var showServerPicker by remember { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }
    var currentNavTab by remember { mutableStateOf("servers") }

    // Launcher for official Android VpnService permission dialog
    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // User granted VPN permission in Android system prompt
            VpnManager.startVpn(context, selectedServer)
        } else {
            Toast.makeText(context, "VPN access permission is required to connect", Toast.LENGTH_SHORT).show()
        }
    }

    // Launcher for Android 13+ Notification permission
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* result handled */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Show error toast if any
    LaunchedEffect(vpnSessionState.errorMessage) {
        vpnSessionState.errorMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }

    // Breathing pulse for outer aura when connected
    val infiniteTransition = rememberInfiniteTransition(label = "halo_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // Expand wave animation during turn-on
    val waveScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_scale"
    )
    val waveAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_alpha"
    )

    // Smooth transition for glow brightness
    val glowIntensity by animateFloatAsState(
        targetValue = if (isConnected && !isConnecting) 1.0f else if (isConnecting) 0.8f else 0.0f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "glow_intensity"
    )

    // Button press scale feedback
    var isPressed by remember { mutableStateOf(false) }
    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else if (isConnecting) 1.03f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "button_scale"
    )

    val timerTextColor by animateColorAsState(
        targetValue = if (isConnected) Color(0xFFF1F5F9) else Color(0xFF64748B),
        animationSpec = tween(400),
        label = "timer_color"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF060B17),
        bottomBar = {
            // Bottom Navigation Bar matching reference design
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF060B17))
                    .border(width = 1.dp, color = Color(0xFF111D33))
                    .navigationBarsPadding()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Servers Tab
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { currentNavTab = "servers" }
                        .padding(horizontal = 28.dp, vertical = 6.dp)
                        .testTag("nav_servers")
                ) {
                    GlobeIcon(
                        color = if (currentNavTab == "servers") Color(0xFF26E875) else Color(0xFF64748B),
                        size = 26.dp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isRu) "Серверы" else "Servers",
                        color = if (currentNavTab == "servers") Color(0xFF26E875) else Color(0xFF64748B),
                        fontSize = 12.sp,
                        fontWeight = if (currentNavTab == "servers") FontWeight.Bold else FontWeight.Medium
                    )
                }

                // Account Tab
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            currentNavTab = "account"
                            showAccountDialog = true
                        }
                        .padding(horizontal = 28.dp, vertical = 6.dp)
                        .testTag("nav_account")
                ) {
                    AccountIcon(
                        color = if (currentNavTab == "account") Color(0xFF26E875) else Color(0xFF64748B),
                        size = 26.dp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isRu) "Аккаунт" else "Account",
                        color = if (currentNavTab == "account") Color(0xFF26E875) else Color(0xFF64748B),
                        fontSize = 12.sp,
                        fontWeight = if (currentNavTab == "account") FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF060B17),
                            Color(0xFF091224),
                            Color(0xFF060B17)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 500.dp)
                    .align(Alignment.Center)
                    .padding(horizontal = 22.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Top Header Brand: ByMe (white) + VPN (neon green)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 4.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ByMe",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "VPN",
                        color = Color(0xFF26E875),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    )
                }

                Spacer(modifier = Modifier.weight(0.12f))

                // Central Shield Power Button
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .scale(buttonScale)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                isPressed = true
                                coroutineScope.launch {
                                    delay(100)
                                    isPressed = false

                                    if (isConnected || isConnecting) {
                                        // Stop real VPN service
                                        VpnManager.stopVpn(context)
                                    } else {
                                        // Check if Android requires user permission to establish VPN
                                        val prepareIntent = VpnManager.getVpnPrepareIntent(context)
                                        if (prepareIntent != null) {
                                            // Launch native system dialog
                                            vpnPermissionLauncher.launch(prepareIntent)
                                        } else {
                                            // Permission already approved, start real VPN
                                            VpnManager.startVpn(context, selectedServer)
                                        }
                                    }
                                }
                            }
                        )
                        .testTag("shield_power_button"),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer expanding pulse wave during connection
                    if (isConnected || isConnecting) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val r = size.width * 0.48f * (if (isConnecting) waveScale else 1.0f)
                            drawCircle(
                                color = Color(0xFF00C4FF).copy(
                                    alpha = if (isConnecting) waveAlpha else 0.15f * pulseAlpha
                                ),
                                radius = r,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }

                    // Shield Logo with responsive active/dimmed glow
                    ShieldLogo(
                        size = 190.dp,
                        isActive = isConnected || isConnecting,
                        pulseAlpha = pulseAlpha,
                        glowAlpha = glowIntensity
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Connection Duration Timer directly below the shield logo
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isConnected && !isConnecting) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF26E875))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        Text(
                            text = when {
                                isConnecting -> "CONNECTING..."
                                isConnected -> formatDuration(durationSeconds)
                                else -> "00:00:00"
                            },
                            color = if (isConnecting) Color(0xFF00D4FF) else timerTextColor,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.2.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = when {
                            isConnecting -> if (isRu) "Запрос авторизации туннеля..." else "Requesting tunnel authorization..."
                            isConnected -> if (isRu) "Подключено и защищено (AES-256)" else "Connected & Secure (AES-256)"
                            else -> if (isRu) "Нажмите на логотип для подключения" else "Tap logo to connect"
                        },
                        color = when {
                            isConnecting -> Color(0xFF00D4FF)
                            isConnected -> Color(0xFF26E875).copy(alpha = 0.9f)
                            else -> Color(0xFF94A3B8)
                        },
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.weight(0.18f))

                // Server Selection Card matching reference image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF0C162A))
                        .border(1.2.dp, Color(0xFF192C4E), RoundedCornerShape(18.dp))
                        .clickable { showServerPicker = true }
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                        .testTag("server_card")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Country Flag
                        CountryFlagView(
                            countryCode = selectedServer.countryCode,
                            size = 30.dp
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        // Country • City & IP / Ping
                        Column(modifier = Modifier.weight(1f)) {
                            val serverCountryName = if (isRu) {
                                when (selectedServer.countryCode) {
                                    "NL" -> "Нидерланды"
                                    "DE" -> "Германия"
                                    else -> selectedServer.country
                                }
                            } else {
                                selectedServer.country
                            }
                            val serverCityName = if (isRu) {
                                when (selectedServer.city) {
                                    "Amsterdam" -> "Амстердам"
                                    "Frankfurt" -> "Франкфурт"
                                    else -> selectedServer.city
                                }
                            } else {
                                selectedServer.city
                            }

                            Text(
                                text = "$serverCountryName • $serverCityName",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "IP: ${selectedServer.ip}",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.5.sp
                                )
                                Text(
                                    text = "${selectedServer.ping} ms",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // "Change Server" Button matching reference image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF165FE8), // Electric blue
                                    Color(0xFF00AFD6), // Cyan
                                    Color(0xFF26E875)  // Neon lime
                                )
                            )
                        )
                        .clickable { showServerPicker = true }
                        .testTag("change_server_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        ChangeServerIcon(color = Color.White, size = 18.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isRu) "Выбрать сервер" else "Change Server",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Server Selection Modal Dialog
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

                    Spacer(modifier = Modifier.height(16.dp))

                    AVAILABLE_SERVERS.forEach { server ->
                        val isSelected = server == selectedServer
                        val sCountry = if (isRu) {
                            when (server.countryCode) {
                                "NL" -> "Нидерланды"
                                "DE" -> "Германия"
                                else -> server.country
                            }
                        } else server.country

                        val sCity = if (isRu) {
                            when (server.city) {
                                "Amsterdam" -> "Амстердам"
                                "Frankfurt" -> "Франкфурт"
                                else -> server.city
                            }
                        } else server.city

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) Color(0xFF162744) else Color(0xFF091222))
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFF26E875) else Color(0xFF152642),
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    VpnManager.selectServer(server)
                                    showServerPicker = false
                                    if (isConnected) {
                                        // Reconnect to new server
                                        VpnManager.startVpn(context, server)
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CountryFlagView(countryCode = server.countryCode, size = 26.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "$sCountry • $sCity",
                                        color = Color.White,
                                        fontSize = 14.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                    Text(
                                        text = "IP: ${server.ip}",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.5.sp
                                    )
                                }
                            }

                            Text(
                                text = "${server.ping} ms",
                                color = if (server.ping < 30) Color(0xFF26E875) else Color(0xFF00D4FF),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // Account Modal Dialog
    if (showAccountDialog) {
        Dialog(onDismissRequest = {
            showAccountDialog = false
            currentNavTab = "servers"
        }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF0C162A))
                    .border(1.2.dp, Color(0xFF1E355B), RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isRu) "Профиль аккаунта" else "Account Profile",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "✕",
                            color = Color(0xFF94A3B8),
                            fontSize = 18.sp,
                            modifier = Modifier
                                .clickable {
                                    showAccountDialog = false
                                    currentNavTab = "servers"
                                }
                                .padding(4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // User avatar & email
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF142442)),
                        contentAlignment = Alignment.Center
                    ) {
                        AccountIcon(color = Color(0xFF26E875), size = 36.dp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = displayEmail,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (displayGoogle) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF122038))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            GoogleLogoIcon(size = 13.dp)
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isRu) "Аккаунт Google подключен" else "Google Account Connected",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Subscription & Plan details from database
                    val userAcc = currentUserAccount
                    val hasSub = userAcc?.hasSubscription ?: true
                    val planName = userAcc?.planName ?: "ByMeVPN Pro Unlimited"
                    val daysLeft = userAcc?.daysRemaining ?: 28
                    val hoursLeft = userAcc?.hoursRemaining ?: 14
                    val expDate = userAcc?.expirationDate ?: "15.10.2026"

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF070E1C))
                            .border(1.dp, Color(0xFF152542), RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (isRu) "Подписка" else "Subscription", color = Color(0xFF94A3B8), fontSize = 13.sp)
                            Text(
                                text = planName,
                                color = if (hasSub) Color(0xFF26E875) else Color(0xFFFFB74D),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (isRu) "Осталось времени" else "Time Remaining", color = Color(0xFF94A3B8), fontSize = 13.sp)
                            Text(
                                text = if (hasSub) {
                                    if (isRu) "$daysLeft дн. $hoursLeft ч. (до $expDate)" else "$daysLeft d $hoursLeft h (until $expDate)"
                                } else {
                                    if (isRu) "Истекла (Купить на сайте)" else "Expired (Renew on site)"
                                },
                                color = if (hasSub) Color.White else Color(0xFFFF6B6B),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (isRu) "Статус VPN" else "VPN Status", color = Color(0xFF94A3B8), fontSize = 13.sp)
                            Text(
                                text = if (isConnected) {
                                    if (isRu) "Защищено (Активно)" else "Protected (Active)"
                                } else {
                                    if (isRu) "Отключено" else "Disconnected"
                                },
                                color = if (isConnected) Color(0xFF26E875) else Color(0xFFFF6B6B),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Language Selector (Russian, English, System default)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF070E1C))
                            .border(1.dp, Color(0xFF152542), RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = if (isRu) "Язык приложения" else "App Language",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AppLanguage.values().forEach { lang ->
                                val isSelected = currentLanguage == lang
                                val label = when (lang) {
                                    AppLanguage.RUSSIAN -> if (isRu) "Русский" else "Russian"
                                    AppLanguage.ENGLISH -> "English"
                                    AppLanguage.SYSTEM -> if (isRu) "Как в системе" else "System"
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) Color(0xFF1B3864) else Color(0xFF0D172B))
                                        .border(
                                            1.dp,
                                            if (isSelected) Color(0xFF00D4FF) else Color(0xFF162542),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            LocaleManager.setLanguage(lang)
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) Color(0xFF00D4FF) else Color(0xFF94A3B8),
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Official Community Links
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF070E1C))
                            .border(1.dp, Color(0xFF152542), RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = if (isRu) "Наши ресурсы и контакты" else "Official Links & Support",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Telegram link
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0E1A30))
                                .clickable {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/ByMeVPN"))
                                    context.startActivity(intent)
                                }
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TelegramLogoIcon(size = 18.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Telegram", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text("@ByMeVPN", color = Color(0xFF00D4FF), fontSize = 11.sp)
                                }
                            }
                            Text("↗", color = Color(0xFF94A3B8), fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Official Website link
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0E1A30))
                                .clickable {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://bymevpn-site.duckdns.org"))
                                    context.startActivity(intent)
                                }
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                WebsiteLogoIcon(size = 18.dp, color = Color(0xFF26E875))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(if (isRu) "Официальный сайт" else "Official Website", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text("bymevpn-site.duckdns.org", color = Color(0xFF26E875), fontSize = 11.sp)
                                }
                            }
                            Text("↗", color = Color(0xFF94A3B8), fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // YouTube channel link
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0E1A30))
                                .clickable {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/@ByMeVPN"))
                                    context.startActivity(intent)
                                }
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                YouTubeLogoIcon(size = 18.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("YouTube", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text("@ByMeVPN", color = Color(0xFFFF4D4D), fontSize = 11.sp)
                                }
                            }
                            Text("↗", color = Color(0xFF94A3B8), fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Log Out Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF2A1520))
                            .border(1.dp, Color(0xFF661E2E), RoundedCornerShape(12.dp))
                            .clickable {
                                if (isConnected) {
                                    VpnManager.stopVpn(context)
                                }
                                AccountRepository.logout(context)
                                showAccountDialog = false
                                onLogOut()
                            }
                            .testTag("logout_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isRu) "Выйти из аккаунта" else "Log Out",
                            color = Color(0xFFFF6B6B),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
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
