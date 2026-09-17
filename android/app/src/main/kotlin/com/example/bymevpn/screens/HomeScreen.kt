package com.example.bymevpn.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bymevpn.components.GoogleLogoIcon
import com.example.bymevpn.components.GradientBackground
import com.example.bymevpn.components.ShieldLogo
import com.example.bymevpn.theme.AppColors

data class VpnServer(val country: String, val city: String, val flag: String, val ping: Int)

val DEFAULT_SERVERS = listOf(
    VpnServer("United States", "New York", "🇺🇸", 24),
    VpnServer("Germany", "Frankfurt", "🇩🇪", 18),
    VpnServer("Netherlands", "Amsterdam", "🇳🇱", 15),
    VpnServer("United Kingdom", "London", "🇬🇧", 21),
    VpnServer("Japan", "Tokyo", "🇯🇵", 85)
)

/**
 * Connected ByMeVPN Home / Dashboard screen.
 * Provides user feedback upon successful email or Google sign-in/registration.
 */
@Composable
fun HomeScreen(
    userEmail: String = "mama.nikfjdj@gmail.com",
    isGoogleAuth: Boolean = true,
    onLogOut: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isConnected by remember { mutableStateOf(true) }
    var selectedServer by remember { mutableStateOf(DEFAULT_SERVERS[0]) }
    var showServerPicker by remember { mutableStateOf(false) }

    val pulseTransition = rememberInfiniteTransition(label = "pulse_ring")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val statusColor by animateColorAsState(
        targetValue = if (isConnected) Color(0xFF26E875) else Color(0xFF94A3B8),
        animationSpec = tween(300),
        label = "status_color"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent
    ) { innerPadding ->
        GradientBackground(modifier = Modifier.padding(innerPadding)) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val scrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 500.dp)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Bar: User Badge + Log Out
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // User Profile Pill
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF0F1E38))
                                .border(1.dp, Color(0xFF1E3860), RoundedCornerShape(20.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isGoogleAuth) {
                                GoogleLogoIcon(size = 16.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = userEmail,
                                color = Color.White,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Log Out button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1A1A2E))
                                .border(1.dp, Color(0xFF332F4C), RoundedCornerShape(12.dp))
                                .clickable(onClick = onLogOut)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("logout_button")
                        ) {
                            Text(
                                text = "Log Out",
                                color = Color(0xFFFF6B6B),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Shield Logo compact
                    ShieldLogo(size = 110.dp)

                    Spacer(modifier = Modifier.height(18.dp))

                    // Status Badge
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isConnected) Color(0x2226E875) else Color(0x2264758E))
                            .border(
                                1.dp,
                                if (isConnected) Color(0x6626E875) else Color(0x4464758E),
                                RoundedCornerShape(14.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isConnected) "CONNECTED & PROTECTED" else "VPN DISCONNECTED",
                            color = statusColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(30.dp))

                    // Big Power Button
                    Box(
                        modifier = Modifier
                            .size(170.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { isConnected = !isConnected }
                            )
                            .testTag("vpn_toggle_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        // Outer glowing pulse ring
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val r = size.width / 2f
                            if (isConnected) {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFF26E875).copy(alpha = pulseAlpha * 0.4f),
                                            Color.Transparent
                                        ),
                                        center = Offset(r, r),
                                        radius = r
                                    )
                                )
                                drawCircle(
                                    color = Color(0xFF26E875).copy(alpha = pulseAlpha),
                                    radius = r - 6f,
                                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                )
                            } else {
                                drawCircle(
                                    color = Color(0xFF1E3558),
                                    radius = r - 6f,
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }
                        }

                        // Inner circular button
                        Box(
                            modifier = Modifier
                                .size(130.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isConnected) {
                                        Brush.linearGradient(
                                            colors = listOf(Color(0xFF00C4FF), Color(0xFF26E875))
                                        )
                                    } else {
                                        Brush.linearGradient(
                                            colors = listOf(Color(0xFF162542), Color(0xFF0A1324))
                                        )
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (isConnected) "STOP" else "START",
                                    color = if (isConnected) Color(0xFF031015) else Color.White,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.8.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(30.dp))

                    // Server Selection Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF0C172C))
                            .border(1.2.dp, Color(0xFF1C345C), RoundedCornerShape(16.dp))
                            .clickable { showServerPicker = !showServerPicker }
                            .padding(16.dp)
                            .testTag("server_card")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = selectedServer.flag, fontSize = 28.sp)
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "${selectedServer.country} (${selectedServer.city})",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Optimal Server • ${selectedServer.ping} ms",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 12.5.sp
                                    )
                                }
                            }

                            Text(
                                text = if (showServerPicker) "▲" else "▼",
                                color = Color(0xFF00D4FF),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Server list expandable
                    if (showServerPicker) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF091224))
                                .border(1.dp, Color(0xFF182D50), RoundedCornerShape(16.dp))
                                .padding(8.dp)
                        ) {
                            DEFAULT_SERVERS.forEach { server ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (server == selectedServer) Color(0xFF142442) else Color.Transparent)
                                        .clickable {
                                            selectedServer = server
                                            showServerPicker = false
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = server.flag, fontSize = 22.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "${server.country} - ${server.city}",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = if (server == selectedServer) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                    Text(
                                        text = "${server.ping} ms",
                                        color = Color(0xFF26E875),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Speed / Protection Metrics Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            label = "DOWNLOAD",
                            value = if (isConnected) "78.4 Mb/s" else "0.0 Mb/s",
                            valueColor = Color(0xFF00D4FF),
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            label = "UPLOAD",
                            value = if (isConnected) "34.2 Mb/s" else "0.0 Mb/s",
                            valueColor = Color(0xFF26E875),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0C172C))
            .border(1.2.dp, Color(0xFF1C345C), RoundedCornerShape(14.dp))
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = valueColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}
