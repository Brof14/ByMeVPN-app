package com.example.bymevpn.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.example.bymevpn.theme.AppColors
import kotlinx.coroutines.launch

private val CustomEaseOut = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

@Composable
fun VpnPowerButton(
    connected: Boolean,
    connecting: Boolean,
    isRussian: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    successPulseKey: Int = 0
) {
    val haptics = rememberByMeHaptics()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 500f),
        label = "vpn_power_press"
    )

    val progress by animateFloatAsState(
        targetValue = if (connected) 1f else 0f,
        animationSpec = tween(280, easing = CustomEaseOut),
        label = "vpn_power_state"
    )

    val ringColor by animateColorAsState(
        targetValue = if (connected) AppColors.BrandMint else Color(0xFF4A5C6F),
        animationSpec = tween(280),
        label = "vpn_power_ring"
    )

    val connectingScale = if (connecting) {
        val transition = rememberInfiniteTransition(label = "vpn_connecting")
        val scale by transition.animateFloat(
            initialValue = 0.98f,
            targetValue = 1.02f,
            animationSpec = infiniteRepeatable(
                animation = tween(650, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "vpn_connecting_pulse"
        )
        scale
    } else 1f

    val successScale = remember { Animatable(1f) }
    val successRipple = remember { Animatable(1f) }
    val successRippleAlpha = remember { Animatable(0f) }

    LaunchedEffect(successPulseKey) {
        if (successPulseKey > 0) {
            haptics.tunnelConnected()
            successRipple.snapTo(0.75f)
            successRippleAlpha.snapTo(0.70f)
            successScale.snapTo(0.88f)
            launch {
                successRipple.animateTo(1.38f, tween(650, easing = CustomEaseOut))
            }
            launch {
                successRippleAlpha.animateTo(0f, tween(650, easing = CustomEaseOut))
            }
            successScale.animateTo(1.08f, animationSpec = tween(200, easing = CustomEaseOut))
            successScale.animateTo(1f, animationSpec = tween(280, easing = CustomEaseOut))
        } else if (!connected) {
            successScale.snapTo(1f)
        }
    }

    val totalScale = connectingScale * successScale.value * pressScale

    Box(
        modifier = modifier
            .sizeIn(maxWidth = 290.dp, maxHeight = 290.dp)
            .aspectRatio(1f)
            .fillMaxWidth()
            .clip(CircleShape)
            .background(Color.Transparent)
            .border(1.5.dp, ringColor.copy(alpha = 0.25f + 0.55f * progress), CircleShape)
            .graphicsLayer {
                scaleX = totalScale
                scaleY = totalScale
            }
            .semantics {
                contentDescription = if (isRussian) {
                    if (connected) "Выключить VPN" else "Включить VPN"
                } else if (connected) "Disconnect VPN" else "Connect VPN"
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = {
                    haptics.tap()
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (successRippleAlpha.value > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = successRipple.value
                        scaleY = successRipple.value
                        alpha = successRippleAlpha.value
                    }
                    .border(2.5.dp, AppColors.BrandMint, CircleShape)
            )
        }
        Image(
            painter = painterResource(R.drawable.byme_connect_shield_transparent),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize(0.93f)
                .graphicsLayer {
                    alpha = 0.62f + 0.38f * progress
                    scaleX = 0.96f + 0.04f * progress
                    scaleY = 0.96f + 0.04f * progress
                }
        )
        if (connecting) {
            CircularProgressIndicator(
                modifier = Modifier.fillMaxSize(0.96f),
                color = AppColors.BrandMint,
                strokeWidth = 2.dp
            )
        }
    }
}
