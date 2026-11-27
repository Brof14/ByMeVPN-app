package com.example.bymevpn.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bymevpn.components.ShieldLogo
import com.example.bymevpn.theme.AppColors
import com.example.bymevpn.theme.AppGradients

@Composable
internal fun AuthBrandPanel(
    isRu: Boolean,
    isSignUp: Boolean,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(AppGradients.AccentCard)
            .border(1.dp, AppColors.BorderAccent.copy(alpha = 0.8f), RoundedCornerShape(24.dp))
            .padding(horizontal = 18.dp, vertical = if (compact) 16.dp else 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 76.dp else 88.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(AppColors.BgSurfaceAccent.copy(alpha = 0.9f))
                .border(1.dp, AppColors.BorderAccent, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            ShieldLogo(size = if (compact) 58.dp else 68.dp, isActive = true)
        }

        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "ByMe",
                color = AppColors.TextPrimary,
                fontSize = 29.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1.1).sp
            )
            Text(
                text = "VPN",
                color = AppColors.SkyBlue,
                fontSize = 29.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1.1).sp
            )
        }

        Spacer(Modifier.height(5.dp))

        Text(
            text = when {
                isRu && isSignUp -> "Ваш доступ к интернету — под вашим контролем"
                isRu -> "Безопасное подключение начинается здесь"
                isSignUp -> "Your connection, protected and in your control"
                else -> "Your secure connection starts here"
            },
            color = AppColors.TextSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AuthFeatureChip(
                modifier = Modifier.weight(1f),
                title = if (isRu) "Защита" else "Private",
                subtitle = if (isRu) "трафика" else "access"
            )
            AuthFeatureChip(
                modifier = Modifier.weight(1f),
                title = if (isRu) "До 10" else "Up to 10",
                subtitle = if (isRu) "устройств" else "devices"
            )
            AuthFeatureChip(
                modifier = Modifier.weight(1f),
                title = if (isRu) "Серверы" else "Servers",
                subtitle = if (isRu) "по миру" else "worldwide"
            )
        }
    }
}

@Composable
private fun AuthFeatureChip(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.BgSurface.copy(alpha = 0.72f))
            .border(1.dp, AppColors.BorderDefault, RoundedCornerShape(14.dp))
            .padding(horizontal = 5.dp, vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = title,
            color = AppColors.TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Text(
            text = subtitle,
            color = AppColors.TextMuted,
            fontSize = 9.sp,
            maxLines = 1
        )
    }
}
