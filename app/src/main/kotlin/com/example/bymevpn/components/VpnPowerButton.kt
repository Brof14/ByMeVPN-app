package com.example.bymevpn.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.bymevpn.R
import com.example.bymevpn.theme.AppColors
import com.example.bymevpn.theme.AppGradients

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
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 500f),
        label = "vpn_power_press"
    )

    val progress by animateFloatAsState(
        targetValue = if (connected) 1f else 0f,
        animationSpec = tween(240, easing = CustomEaseOut),
        label = "vpn_power_state"
    )

    val ringColor by animateColorAsState(
        targetValue = when {
            connected -> AppColors.BrandMintBright
            connecting -> AppColors.SkyBlue
            else -> AppColors.BorderAccent
        },
        animationSpec = tween(240),
        label = "vpn_power_ring"
    )

    val ringBrush = remember(connected, connecting) {
        if (connected || connecting) {
            AppGradients.Shield
        } else {
            Brush.linearGradient(
                listOf(
                    Color(0xFF1F4278),
                    Color(0xFF193563)
                )
            )
        }
    }

    val haloBrush = remember(connected, connecting) {
        if (connected) {
            Brush.radialGradient(
                colors = listOf(
                    Color(0x2E00D8FF),
                    Color(0x1842F59B),
                    Color.Transparent
                )
            )
        } else if (connecting) {
            Brush.radialGradient(
                colors = listOf(
                    Color(0x240077FF),
                    Color(0x1000D8FF),
                    Color.Transparent
                )
            )
        } else {
            Brush.radialGradient(
                colors = listOf(
                    Color(0x180077FF),
                    Color.Transparent
                )
            )
        }
    }

    val successScale = remember { Animatable(1f) }

    LaunchedEffect(successPulseKey) {
        if (successPulseKey > 0) {
            haptics.tunnelConnected()
            successScale.animateTo(1.03f, animationSpec = tween(140, easing = CustomEaseOut))
            successScale.animateTo(1f, animationSpec = tween(200, easing = CustomEaseOut))
        } else if (!connected) {
            successScale.snapTo(1f)
        }
    }

    val totalScale = successScale.value * pressScale

    Box(
        modifier = modifier
            .sizeIn(maxWidth = 264.dp, maxHeight = 264.dp)
            .aspectRatio(1f)
            .fillMaxWidth()
            .clip(CircleShape)
            .background(haloBrush)
            .border(
                width = if (connected || connecting) 2.dp else 1.5.dp,
                brush = ringBrush,
                shape = CircleShape
            )
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
        Image(
            painter = painterResource(R.drawable.byme_connect_shield_transparent),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize(0.93f)
                .graphicsLayer {
                    alpha = 0.68f + 0.32f * progress
                    scaleX = 0.96f + 0.04f * progress
                    scaleY = 0.96f + 0.04f * progress
                }
        )
        if (connecting) {
            CircularProgressIndicator(
                modifier = Modifier.fillMaxSize(0.97f),
                color = ringColor,
                strokeWidth = 2.5.dp
            )
        }
    }
}
