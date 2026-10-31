package com.example.bymevpn.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.bymevpn.theme.AppColors

/**
 * One-time, compact and honest VPN traffic disclosure; deliberately no oversized
 * Google Play / Zero-Logs marketing panel. Android still presents its own VPN permission prompt.
 */
@Composable
fun VpnDisclosureDialog(
    isRussian: Boolean = true,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit = {}
) {
    Dialog(onDismissRequest = onDecline) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.CardBg, RoundedCornerShape(18.dp))
                .border(1.dp, AppColors.CardBorder, RoundedCornerShape(18.dp))
                .padding(22.dp)
        ) {
            Text(
                if (isRussian) "Подключение к VPN" else "VPN connection",
                color = AppColors.TextPrimary,
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
            Text(
                if (isRussian)
                    "ByMeVPN использует Android VpnService и направляет сетевой трафик через выбранный VPN-сервер. Если включён обход для российских сайтов, они открываются напрямую."
                else
                    "ByMeVPN uses Android VpnService to route network traffic through the selected VPN server. If Russian-site bypass is enabled, those sites connect directly.",
                color = AppColors.TextSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
            Spacer(Modifier.height(12.dp))
            Text(
                if (isRussian) "Политика конфиденциальности" else "Privacy policy",
                color = AppColors.BrandMint,
                modifier = Modifier.clickable(onClick = onOpenPrivacyPolicy),
                fontSize = 13.sp
            )
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    if (isRussian) "Отмена" else "Cancel",
                    modifier = Modifier.weight(1f).clickable(onClick = onDecline).padding(vertical = 12.dp),
                    color = AppColors.TextSecondary
                )
                Text(
                    if (isRussian) "Продолжить" else "Continue",
                    modifier = Modifier.weight(1f).clickable(onClick = onAccept).padding(vertical = 12.dp),
                    color = AppColors.BrandMint,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
