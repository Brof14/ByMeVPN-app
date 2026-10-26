package com.example.bymevpn.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.bymevpn.theme.AppColors

/**
 * Prominent In-App Disclosure dialog required by Google Play VpnService Policy.
 * Displayed before initiating VpnService.prepare() or activating the VPN tunnel.
 */
@Composable
fun VpnDisclosureDialog(
    isRussian: Boolean = true,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit = {}
) {
    Dialog(
        onDismissRequest = onDecline,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(AppColors.CardBg)
                .border(1.2.dp, AppColors.CardBorder, RoundedCornerShape(24.dp))
                .padding(22.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Shield Header Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(AppColors.CardBgElevated)
                        .border(1.dp, AppColors.BrandGreen.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    ShieldLogo(size = 32.dp, isActive = true, glowAlpha = 0.85f)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (isRussian) "Защита сетевого подключения" else "Network Privacy & VPN Disclosure",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isRussian)
                        "Уведомление в соответствии с требованиями безопасности Google Play"
                    else
                        "Prominent disclosure in accordance with Google Play Policy",
                    color = AppColors.TextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Feature Point 1: VpnService Tunnel
                DisclosurePoint(
                    icon = "🛡️",
                    title = if (isRussian) "Использование VpnService" else "VpnService Tunnel",
                    description = if (isRussian)
                        "ByMeVPN использует системный Android VpnService для создания безопасного зашифрованного сетевого туннеля от вашего устройства к выбранному серверу."
                    else
                        "ByMeVPN uses Android VpnService to create a secure, encrypted network tunnel from your device to our selected server."
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Feature Point 2: Traffic Encryption
                DisclosurePoint(
                    icon = "🔒",
                    title = if (isRussian) "Сквозное шифрование" else "End-to-End Encryption",
                    description = if (isRussian)
                        "Весь исходящий и входящий интернет-трафик защищается протоколами VLESS Reality и XTLS, предотвращая перехват данных в публичных сетях Wi-Fi."
                    else
                        "All network traffic is encrypted via VLESS Reality and XTLS protocols, shielding your data on untrusted and public Wi-Fi networks."
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Feature Point 3: Zero-Logs Guarantee
                DisclosurePoint(
                    icon = "🚫",
                    title = if (isRussian) "Строгая политика Zero-Logs" else "Strict Zero-Log Policy",
                    description = if (isRussian)
                        "Мы категорически не отслеживаем, не регистрируем и не продаем историю вашей активности в сети, посещенные веб-страницы или DNS-запросы."
                    else
                        "We never monitor, store, or sell your browsing history, visited websites, DNS queries, or private IP address."
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Privacy Policy Link
                Text(
                    text = if (isRussian) "Ознакомиться с Политикой конфиденциальности →" else "Read full Privacy Policy →",
                    color = AppColors.SkyBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onOpenPrivacyPolicy() }
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Buttons: Accept & Decline
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppColors.InputBg)
                            .border(1.dp, AppColors.CardBorder, RoundedCornerShape(12.dp))
                            .clickable { onDecline() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isRussian) "Отклонить" else "Decline",
                            color = AppColors.TextSecondary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1.3f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppColors.BrandGreen)
                            .clickable { onAccept() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isRussian) "Принять и продолжить" else "Accept & Continue",
                            color = AppColors.DarkText,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DisclosurePoint(
    icon: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.CardBgElevated)
            .border(1.dp, AppColors.CardBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(text = icon, fontSize = 20.sp)
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                color = AppColors.TextSecondary,
                fontSize = 11.5.sp,
                lineHeight = 15.sp
            )
        }
    }
}
