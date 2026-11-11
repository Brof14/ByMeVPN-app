package com.example.bymevpn.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Short branded overlay while the first app frame and cached session are prepared. */
@Composable
fun AppStartupOverlay() {
    val transition = rememberInfiniteTransition(label = "startup-mark")
    val scale by transition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(tween(850), RepeatMode.Reverse),
        label = "startup-mark-pulse"
    )

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0B0F12)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(116.dp).border(1.dp, Color(0x5537D6B0), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                ShieldLogo(modifier = Modifier.scale(scale), size = 72.dp)
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "ByMeVPN",
                color = Color(0xFFECF4F5),
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(18.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color(0xFF70D4C1),
                strokeWidth = 2.dp
            )
        }
    }
}
