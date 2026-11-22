package com.example.bymevpn.screens

import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bymevpn.components.BottomNavTab
import com.example.bymevpn.components.ByMeBottomNav
import com.example.bymevpn.components.ByMeCardGroup
import com.example.bymevpn.components.ByMeHaptics
import com.example.bymevpn.components.ByMePrimaryButton
import com.example.bymevpn.components.ByMeSettingRow
import com.example.bymevpn.components.ByMeTopBar
import com.example.bymevpn.components.ChatHeartIcon
import com.example.bymevpn.components.ChevronRightIcon
import com.example.bymevpn.components.FaqDialog
import com.example.bymevpn.components.InfoCircleIcon
import com.example.bymevpn.components.LegalTermsDialog
import com.example.bymevpn.components.MailIcon
import com.example.bymevpn.components.QuestionHelpIcon
import com.example.bymevpn.components.ShieldSecurityIcon
import com.example.bymevpn.BuildConfig
import com.example.bymevpn.theme.AppColors
import com.example.bymevpn.data.LocaleManager
import androidx.compose.runtime.collectAsState

@Composable
fun SupportScreen(
    onBackClick: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAccount: () -> Unit,
    originTab: BottomNavTab = BottomNavTab.Account,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val language by LocaleManager.currentLanguage.collectAsState()
    val isRu = LocaleManager.isRussian(language = language)
    val t: (String, String) -> String = { ru, en -> if (isRu) ru else en }
    val scrollState = rememberScrollState()
    var showFaqDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.BgCanvas)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ByMeTopBar(
                title = t("Помощь", "Help"),
                onBackClick = onBackClick,
                onMoreClick = null
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 26.dp)
            ) {
                // Support Intro Hero
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp, bottom = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(21.dp))
                            .background(AppColors.Teal.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        ChatHeartIcon(color = AppColors.NeonTeal, size = 30.dp)
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = t("Мы рядом, если\nнужна помощь", "We're here\nto help"),
                        color = AppColors.TextPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 33.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = t("Напишите нам о проблеме или задайте вопрос по работе ByMeVPN.", "Contact us with a question or a problem using ByMeVPN."),
                        color = AppColors.TextSecondary,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    ByMePrimaryButton(
                        text = t("Написать в поддержку", "Contact support"),
                        onClick = {
                            ByMeHaptics.tap(context)
                            val url = "https://t.me/ByMeVPNSupportBot"
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                                .onFailure {
                                    Toast.makeText(
                                        context,
                                        t("Не удалось открыть Telegram. Напишите на support@bymevpn.com", "Could not open Telegram. Email support@bymevpn.com"),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        }
                    )
                }

                // Section: Полезное
                Text(
                    text = t("Полезное", "Useful links"),
                    color = AppColors.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 12.dp, bottom = 10.dp, start = 2.dp)
                )

                ByMeCardGroup {
                    ByMeSettingRow(
                        title = t("Ответы на вопросы", "FAQ"),
                        subtitle = t("Оплата, подключение и устройства", "Payments, connection and devices"),
                        icon = { QuestionHelpIcon(color = AppColors.Teal, size = 20.dp) },
                        trailing = { ChevronRightIcon(color = AppColors.TextMuted, size = 18.dp) },
                        onClick = {
                            ByMeHaptics.tap(context)
                            showFaqDialog = true
                        }
                    )
                    ByMeSettingRow(
                        title = t("Написать по email", "Contact by email"),
                        subtitle = "support@bymevpn.com",
                        icon = { MailIcon(color = AppColors.Teal, size = 20.dp) },
                        trailing = { ChevronRightIcon(color = AppColors.TextMuted, size = 18.dp) },
                        onClick = {
                            ByMeHaptics.tap(context)
                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:support@bymevpn.com")).apply {
                                putExtra(Intent.EXTRA_SUBJECT, t("Поддержка ByMeVPN", "ByMeVPN support"))
                            }
                            runCatching { context.startActivity(intent) }
                                .onFailure {
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    clipboard?.setPrimaryClip(ClipData.newPlainText("ByMeVPN support", "support@bymevpn.com"))
                                    Toast.makeText(
                                        context,
                                        t("Адрес поддержки скопирован: support@bymevpn.com", "Support address copied: support@bymevpn.com"),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        }
                    )
                }

                // Section: Информация
                Text(
                    text = t("Информация", "Information"),
                    color = AppColors.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 22.dp, bottom = 10.dp, start = 2.dp)
                )

                ByMeCardGroup {
                    ByMeSettingRow(
                        title = t("О приложении", "About the app"),
                        subtitle = t("Версия ${BuildConfig.VERSION_NAME}", "Version ${BuildConfig.VERSION_NAME}"),
                        icon = { InfoCircleIcon(color = AppColors.Teal, size = 20.dp) },
                        trailing = { ChevronRightIcon(color = AppColors.TextMuted, size = 18.dp) },
                        onClick = {
                            ByMeHaptics.tap(context)
                            showAboutDialog = true
                        }
                    )
                    ByMeSettingRow(
                        title = t("Документы и приватность", "Privacy and documents"),
                        subtitle = null,
                        icon = { ShieldSecurityIcon(color = AppColors.Teal, size = 20.dp) },
                        trailing = { ChevronRightIcon(color = AppColors.TextMuted, size = 18.dp) },
                        onClick = {
                            ByMeHaptics.tap(context)
                            showTermsDialog = true
                        },
                        showBottomDivider = false
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = t("Поддержка ByMeVPN", "ByMeVPN support"),
                    color = AppColors.TextMuted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
                )
            }

            ByMeBottomNav(
                currentTab = originTab,
                onTabSelected = { tab ->
                    when (tab) {
                        BottomNavTab.Home -> onNavigateToHome()
                        BottomNavTab.Settings -> onNavigateToSettings()
                        BottomNavTab.Account -> onNavigateToAccount()
                    }
                }
            )
        }

        if (showFaqDialog) {
            FaqDialog(onDismiss = { showFaqDialog = false })
        }
        if (showTermsDialog) {
            LegalTermsDialog(onDismiss = { showTermsDialog = false })
        }
        if (showAboutDialog) {
            AlertDialog(
                onDismissRequest = { showAboutDialog = false },
                title = { Text("ByMeVPN") },
                text = { Text(t("Версия ${BuildConfig.VERSION_NAME}. Для связи: support@bymevpn.com", "Version ${BuildConfig.VERSION_NAME}. Contact: support@bymevpn.com")) },
                confirmButton = {
                    TextButton(onClick = { showAboutDialog = false }) {
                        Text(t("Закрыть", "Close"))
                    }
                }
            )
        }
    }
}
