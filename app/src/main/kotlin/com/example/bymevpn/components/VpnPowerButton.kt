package com.example.bymevpn.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.bymevpn.R

@Composable
fun VpnPowerButton(
    connected: Boolean,
    connecting: Boolean,
    isRussian: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress by animateFloatAsState(
        targetValue = if (connected) 1f else 0f,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "vpn_power_state"
    )
    val ringColor by animateColorAsState(
        targetValue = if (connected) Color(0xFF35D6A3) else Color(0xFF536578),
        animationSpec = tween(420),
        label = "vpn_power_ring"
    )
    val connectingScale = if (connecting) {
        val transition = rememberInfiniteTransition(label = "vpn_connecting")
        val scale by transition.animateFloat(
            initialValue = 0.98f,
            targetValue = 1.02f,
            animationSpec = infiniteRepeatable(
                animation = tween(700, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "vpn_connecting_pulse"
        )
        scale
    } else 1f

    Box(
        modifier = modifier
            .size(235.dp)
            .clip(CircleShape)
            .background(Color.Transparent)
            .border(1.5.dp, ringColor.copy(alpha = 0.25f + 0.55f * progress), CircleShape)
            .graphicsLayer(scaleX = connectingScale, scaleY = connectingScale)
            .semantics {
                contentDescription = if (isRussian) {
                    if (connected) "Выключить VPN" else "Включить VPN"
                } else if (connected) "Disconnect VPN" else "Connect VPN"
            }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.byme_connect_shield_transparent),
            contentDescription = null,
            modifier = Modifier
                .size(202.dp)
                .graphicsLayer(
                    alpha = 0.62f + 0.38f * progress,
                    scaleX = 0.96f + 0.04f * progress,
                    scaleY = 0.96f + 0.04f * progress
                )
        )
        if (connecting) {
            CircularProgressIndicator(
                modifier = Modifier.size(224.dp),
                color = Color(0xFF35D6A3),
                strokeWidth = 2.dp
            )
        }
    }
}
