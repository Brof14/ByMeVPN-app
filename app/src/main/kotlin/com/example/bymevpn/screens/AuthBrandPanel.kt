package com.example.bymevpn.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bymevpn.components.ShieldLogo
import com.example.bymevpn.theme.AppColors

/**
 * Quiet brand header used by both authentication flows.
 * No decorative cards or feature chips: the product mark is the visual anchor.
 */
@Composable
internal fun AuthBrandPanel(
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ShieldLogo(size = if (compact) 76.dp else 88.dp, isActive = true)

        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "ByMe",
                color = AppColors.TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1.0).sp
            )
            Text(
                text = "VPN",
                color = AppColors.SkyBlue,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1.0).sp
            )
        }
    }
}
