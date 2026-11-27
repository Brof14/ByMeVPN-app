package com.example.bymevpn.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.bymevpn.R

/**
 * Transparent shield artwork shared by the connect control and app branding.
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
    val activeAlpha = (if (isActive) 1f else 0.75f) * pulseAlpha.coerceIn(0f, 1f)
    Image(
        painter = painterResource(id = R.drawable.byme_connect_shield_transparent),
        contentDescription = "ByMeVPN",
        contentScale = ContentScale.Fit,
        modifier = modifier
            .size(size)
            .alpha(if (isConnecting) activeAlpha * 0.9f else activeAlpha)
    )
}
