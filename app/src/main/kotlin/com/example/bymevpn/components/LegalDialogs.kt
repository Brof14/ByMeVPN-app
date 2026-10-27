package com.example.bymevpn.components

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.bymevpn.theme.AppColors

@Composable
fun PrivacyPolicyDialog(
    isRussian: Boolean = true,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(AppColors.CardBg)
                .border(1.2.dp, AppColors.CardBorder, RoundedCornerShape(24.dp))
                .padding(22.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isRussian) "Политика конфиденциальности" else "Privacy Policy",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "✕",
                        color = AppColors.TextSecondary,
                        fontSize = 18.sp,
                        modifier = Modifier.clickable { onDismiss() }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    LegalSection(
                        title = if (isRussian) "1. Принцип абсолютной анонимности (Zero-Logs)" else "1. Strict Zero-Log Principle",
                        body = if (isRussian)
                            "ByMeVPN придерживается строгой политики отсутствия логов. Мы НЕ регистрируем, НЕ записываем и НЕ передаем третьим лицам: ваши IP-адреса, историю посещенных веб-сайтов, DNS-запросы, передаваемые файлы или содержание трафика."
                        else
                            "ByMeVPN adheres to a strict Zero-Log policy. We NEVER log, record, or share with third parties: your IP addresses, browsing history, DNS queries, transferred files, or traffic contents."
                    )

                    LegalSection(
                        title = if (isRussian) "2. Какие данные мы обрабатываем" else "2. Data Processed",
                        body = if (isRussian)
                            "• Учетная запись: адрес электронной почты (или Google ID) для входа и восстановления доступа.\n• Подписка: срок действия и количество активных устройств (до 5).\n• Платежи: обрабатываются сертифицированными провайдерами (ЮKassa / Google Play). Мы не имеем доступа к данным ваших банковских карт."
                        else
                            "• Account: email address (or Google ID) for login and account recovery.\n• Subscription: validity period and registered device slots (up to 5).\n• Payments: processed by certified payment providers (YooKassa / Google Play). We do not store or process card numbers."
                    )

                    LegalSection(
                        title = if (isRussian) "3. Безопасность и шифрование" else "3. Encryption & Security",
                        body = if (isRussian)
                            "Для защиты соединения применяются современные алгоритмы сквозного шифрования (VLESS Reality, XTLS, TLS 1.3, AES-256-GCM / ChaCha20-Poly1305), исключающие перехват данных провайдерами или в открытых сетях."
                        else
                            "Network tunnels utilize state-of-the-art encryption algorithms (VLESS Reality, XTLS, TLS 1.3, AES-256-GCM / ChaCha20-Poly1305), preventing interception by ISPs or untrusted networks."
                    )

                    LegalSection(
                        title = if (isRussian) "4. Право на удаление данных" else "4. Right to Deletion",
                        body = if (isRussian)
                            "Вы можете полностью удалить свой аккаунт и связанные данные в любое время в разделе «Аккаунт» приложения или отправив запрос на сайте https://bymevpn.com/delete-account."
                        else
                            "You may permanently delete your account and all associated data at any time from the app's Account tab or by requesting deletion at https://bymevpn.com/delete-account."
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppColors.InputBg)
                            .border(1.dp, AppColors.CardBorder, RoundedCornerShape(12.dp))
                            .clickable {
                                openBrowserUrl(context, "https://bymevpn.com/privacy")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isRussian) "Открыть на сайте ↗" else "Open in Browser ↗",
                            color = AppColors.SkyBlue,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppColors.BrandGreen)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isRussian) "Понятно" else "Close",
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
fun TermsOfServiceDialog(
    isRussian: Boolean = true,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(AppColors.CardBg)
                .border(1.2.dp, AppColors.CardBorder, RoundedCornerShape(24.dp))
                .padding(22.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isRussian) "Условия использования" else "Terms of Service",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "✕",
                        color = AppColors.TextSecondary,
                        fontSize = 18.sp,
                        modifier = Modifier.clickable { onDismiss() }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    LegalSection(
                        title = if (isRussian) "1. Правила использования сервиса" else "1. Acceptable Use",
                        body = if (isRussian)
                            "ByMeVPN предназначен для защиты личной конфиденциальности, безопасности в открытых сетях и доступа к свободной информации. Запрещается использование сервиса для распространения вредоносного ПО, сетевых атак (DDoS) или незаконной деятельности."
                        else
                            "ByMeVPN is intended for personal privacy protection and network security. Use of the service for malware distribution, DDoS attacks, or unlawful activities is strictly prohibited."
                    )

                    LegalSection(
                        title = if (isRussian) "2. Подписки и лимиты устройств" else "2. Subscriptions & Limits",
                        body = if (isRussian)
                            "Одна активная подписка позволяет одновременно подключать до 5 личных устройств (смартфоны, планшеты, ТВ, компьютеры). Превышение лимита требует отключения неиспользуемых устройств в настройках аккаунта."
                        else
                            "Each active subscription allows connecting up to 5 personal devices simultaneously (smartphones, tablets, TVs, PCs). Exceeding this limit requires disconnecting unused devices."
                    )

                    LegalSection(
                        title = if (isRussian) "3. Возврат средств и отмена" else "3. Refunds & Cancellation",
                        body = if (isRussian)
                            "Мы предоставляем бесплатный 3-дневный пробный период без привязки карты. Если приобретенная подписка не работает должным образом, возврат средств доступен в течение 14 дней через службу поддержки @ByMeVPNSupportBot."
                        else
                            "A free 3-day trial is provided with no card required. If a purchased plan does not perform properly, refund requests can be made within 14 days via support @ByMeVPNSupportBot."
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppColors.InputBg)
                            .border(1.dp, AppColors.CardBorder, RoundedCornerShape(12.dp))
                            .clickable {
                                openBrowserUrl(context, "https://bymevpn.com/terms")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isRussian) "Открыть на сайте ↗" else "Open in Browser ↗",
                            color = AppColors.SkyBlue,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppColors.BrandGreen)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isRussian) "Понятно" else "Close",
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
private fun LegalSection(title: String, body: String) {
    Column(modifier = Modifier.padding(bottom = 14.dp)) {
        Text(
            text = title,
            color = AppColors.BrandGreen,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = body,
            color = AppColors.TextSecondary,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
    }
}

private fun openBrowserUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {}
}

@Composable
fun LegalTermsDialog(onDismiss: () -> Unit) {
    PrivacyPolicyDialog(isRussian = true, onDismiss = onDismiss)
}
