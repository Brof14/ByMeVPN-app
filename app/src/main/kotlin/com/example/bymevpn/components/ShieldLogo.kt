package com.example.bymevpn.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.bymevpn.R

/**
 * The official ByMeVPN artwork, shared by the connect/disconnect control and branding.
 * Avoid redrawn geometry, ambient animation and extra glows.
 */
@Composable
fun ShieldLogo(
    modifier: Modifier = Modifier,
    size: Dp = 180.dp,
    isActive: Boolean = true,
    pulseAlpha: Float = 1.0f,
    glowAlpha: Float = 1.0f,
    isConnecting: Boolean = false
) {
    // Animate only while connecting; do not keep the renderer running forever.
    val scale = if (isConnecting) {
        val transition = rememberInfiniteTransition(label = "vpn-connecting")
        val animated by transition.animateFloat(
            initialValue = 0.97f,
            targetValue = 1.03f,
            animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
            label = "vpn-connect-scale"
        )
        animated
    } else 1f
    Image(
        painter = painterResource(id = R.drawable.byme_connect_shield),
        contentDescription = "ByMeVPN",
        contentScale = ContentScale.Fit,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.15f))
            .graphicsLayer {
                scaleX = if (isConnecting) scale else 1f
                scaleY = if (isConnecting) scale else 1f
            }
            .alpha((if (isActive) 1f else 0.75f) * pulseAlpha.coerceIn(0f, 1f))
    )
}
