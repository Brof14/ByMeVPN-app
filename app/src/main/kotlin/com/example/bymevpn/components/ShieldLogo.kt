package com.example.bymevpn.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
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
    glowAlpha: Float = 1.0f
) {
    Image(
        painter = painterResource(id = R.drawable.byme_brand_logo),
        contentDescription = "ByMeVPN",
        contentScale = ContentScale.Fit,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.22f))
            .alpha((if (isActive) 1f else 0.68f) * pulseAlpha.coerceIn(0f, 1f))
    )
}
