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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.bymevpn.BuildConfig
import com.example.bymevpn.data.LocaleManager
import com.example.bymevpn.theme.AppColors
import com.example.bymevpn.theme.AppGradients

@Composable
fun PrivacyPolicyDialog(
    isRussian: Boolean = LocaleManager.isRussian(),
    onDismiss: () -> Unit
) {
    LegalDocumentDialog(
        initialTab = 0,
        isRussian = isRussian,
        onDismiss = onDismiss
    )
}

@Composable
fun TermsOfServiceDialog(
    isRussian: Boolean = LocaleManager.isRussian(),
    onDismiss: () -> Unit
) {
    LegalDocumentDialog(
        initialTab = 1,
        isRussian = isRussian,
        onDismiss = onDismiss
    )
}

@Composable
fun LegalTermsDialog(onDismiss: () -> Unit) {
    val language by LocaleManager.currentLanguage.collectAsState()
    val isRu = remember(language) { LocaleManager.isRussian(language = language) }
    LegalDocumentDialog(
        initialTab = 0,
        isRussian = isRu,
        onDismiss = onDismiss
    )
}

@Composable
private fun LegalDocumentDialog(
    initialTab: Int,
    isRussian: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }
    val showingPrivacy = selectedTab == 0

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(AppColors.BgSurface)
                .border(1.2.dp, AppColors.BorderDefault, RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (showingPrivacy) {
                            if (isRussian) "Политика конфиденциальности" else "Privacy Policy"
                        } else {
                            if (isRussian) "Условия использования" else "Terms of Service"
                        },
                        color = AppColors.TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(AppColors.TopBarIconBg)
                            .border(1.dp, AppColors.TopBarIconBorder, CircleShape)
                            .clickable(role = Role.Button, onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        CloseXIcon(color = AppColors.TextSecondary, size = 16.dp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppColors.BgInput)
                        .border(1.dp, AppColors.BorderDefault, RoundedCornerShape(12.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LegalTabButton(
                        text = if (isRussian) "Конфиденциальность" else "Privacy",
                        selected = showingPrivacy,
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.weight(1f)
                    )
                    LegalTabButton(
                        text = if (isRussian) "Условия" else "Terms",
                        selected = !showingPrivacy,
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (showingPrivacy) {
                        LegalSection(
                            title = if (isRussian) "1. Работа VPN" else "1. VPN operation",
                            body = if (isRussian)
                                "Приложение направляет сетевой трафик через выбранный VPN-сервер. Для управления доступом и диагностики могут обрабатываться сведения об устройстве и подключении."
                            else
                                "The app routes network traffic through the selected VPN server. Device and connection information may be processed to manage access and diagnose problems."
                        )

                        val dataBodyRu = buildString {
                            append("• Учётная запись: адрес электронной почты (или Google / Telegram ID) для входа и синхронизации доступа.\n")
                            append("• Подписка: срок действия и количество активных устройств по условиям вашего аккаунта.")
                            if (BuildConfig.EXTERNAL_CHECKOUT_ENABLED) {
                                append("\n• Платежи: обрабатываются ЮKassa. Приложение не хранит реквизиты банковских карт.")
                            }
                        }
                        val dataBodyEn = buildString {
                            append("• Account: email address (or Google / Telegram ID) for sign-in and access sync.\n")
                            append("• Subscription: validity period and active device limits for your account.")
                            if (BuildConfig.EXTERNAL_CHECKOUT_ENABLED) {
                                append("\n• Payments: processed by YooKassa. The app does not store card numbers.")
                            }
                        }
                        LegalSection(
                            title = if (isRussian) "2. Какие данные мы обрабатываем" else "2. Data Processed",
                            body = if (isRussian) dataBodyRu else dataBodyEn
                        )

                        LegalSection(
                            title = if (isRussian) "3. Безопасность и шифрование" else "3. Encryption & Security",
                            body = if (isRussian)
                                "VPN-туннель использует шифрованные соединения для защиты трафика. Отдельные сайты и выбранные приложения могут открываться напрямую при настройке маршрутизации и раздельного туннелирования."
                            else
                                "VPN tunnels use encrypted connections to protect traffic. Selected websites or apps may bypass the tunnel when routing exclusions are enabled."
                        )

                        LegalSection(
                            title = if (isRussian) "4. Право на удаление данных" else "4. Right to Deletion",
                            body = if (isRussian)
                                "Чтобы удалить аккаунт, откройте раздел «Аккаунт» и выберите «Удалить аккаунт». Доступ к VPN и активный срок будут удалены."
                            else
                                "To delete your account, open Account and choose Delete account. VPN access and remaining subscription time will be removed."
                        )
                    } else {
                        LegalSection(
                            title = if (isRussian) "1. Правила использования сервиса" else "1. Acceptable Use",
                            body = if (isRussian)
                                "ByMeVPN предназначен для защиты личной конфиденциальности и безопасности в открытых сетях. Запрещается использование сервиса для распространения вредоносного ПО, сетевых атак (DDoS) или иной незаконной деятельности."
                            else
                                "ByMeVPN is intended for personal privacy protection and network security. Use of the service for malware distribution, DDoS attacks, or unlawful activities is strictly prohibited."
                        )

                        LegalSection(
                            title = if (isRussian) "2. Подписки и лимиты устройств" else "2. Subscriptions & Limits",
                            body = if (isRussian)
                                "Активная подписка позволяет одновременно подключать 2, 5 или 10 личных устройств в зависимости от уровня доступа аккаунта. Неиспользуемые устройства можно отключить в разделе «Устройства»."
                            else
                                "An active subscription allows connecting 2, 5, or 10 personal devices simultaneously depending on your account tier. Unused devices can be removed in the Devices screen."
                        )

                        LegalSection(
                            title = if (isRussian) "3. Пробный доступ и поддержка" else "3. Trial Access & Support",
                            body = if (isRussian)
                                "Мы предоставляем бесплатный 3-дневный пробный период без привязки карты. По вопросам работы сервиса обращайтесь на support@bymevpn.com."
                            else
                                "A free 3-day trial is provided with no card required. For assistance, contact support@bymevpn.com."
                        )
                    }
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
                            .background(AppColors.BgInput)
                            .border(1.dp, AppColors.BorderDefault, RoundedCornerShape(12.dp))
                            .clickable {
                                val targetUrl = if (showingPrivacy) "https://bymevpn.com/privacy" else "https://bymevpn.com/terms"
                                openBrowserUrl(context, targetUrl)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isRussian) "На сайте ↗" else "On web ↗",
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
                            .background(AppGradients.PrimaryButton)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isRussian) "Понятно" else "Close",
                            color = AppColors.BrandMintDark,
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
private fun LegalTabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) AppColors.BgSurfaceElevated else Color.Transparent)
            .then(
                if (selected) Modifier.border(1.dp, AppColors.BorderAccent, RoundedCornerShape(9.dp))
                else Modifier
            )
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) AppColors.BrandMintBright else AppColors.TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun LegalSection(title: String, body: String) {
    Column(modifier = Modifier.padding(bottom = 14.dp)) {
        Text(
            text = title,
            color = AppColors.SkyBlue,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = body,
            color = AppColors.TextSecondary,
            fontSize = 12.5.sp,
            lineHeight = 17.5.sp
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
