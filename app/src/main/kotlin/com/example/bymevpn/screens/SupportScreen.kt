package com.example.bymevpn.screens

import android.content.Intent
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
import com.example.bymevpn.components.ByMePrimaryButton
import com.example.bymevpn.components.ByMeSettingRow
import com.example.bymevpn.components.ByMeTopBar
import com.example.bymevpn.components.ChatHeartIcon
import com.example.bymevpn.components.ChevronRightIcon
import com.example.bymevpn.components.FaqDialog
import com.example.bymevpn.components.InfoCircleIcon
import com.example.bymevpn.components.LegalTermsDialog
import com.example.bymevpn.components.QuestionHelpIcon
import com.example.bymevpn.components.ShieldSecurityIcon
import com.example.bymevpn.theme.AppColors
import com.example.bymevpn.data.LocaleManager
import androidx.compose.runtime.collectAsState

@Composable
fun SupportScreen(
    onBackClick: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val language by LocaleManager.currentLanguage.collectAsState()
    val isRu = LocaleManager.isRussian(language = language)
    val t: (String, String) -> String = { ru, en -> if (isRu) ru else en }
    val scrollState = rememberScrollState()
    var showFaqDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }

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
                            .background(Color(0xFF19382E)),
                        contentAlignment = Alignment.Center
                    ) {
                        ChatHeartIcon(color = Color(0xFF45E3AE), size = 30.dp)
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
                        text = t("Нашли ошибку или придумали улучшение? Напишите нам: за подтверждённые ошибки и принятые предложения добавим дни к подписке.", "Found a bug or have a suggestion? Contact us: confirmed reports and accepted suggestions may earn extra days."),
                        color = Color(0xFF92AAA4),
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    ByMePrimaryButton(
                        text = t("Написать в поддержку", "Contact support"),
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:support@bymevpn.com")).apply {
                                putExtra(Intent.EXTRA_SUBJECT, t("Поддержка ByMeVPN", "ByMeVPN support"))
                            }
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                Toast.makeText(context, t("На устройстве не настроена почта", "No email app is configured"), Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                // Section: Полезное
                Text(
                    text = t("Полезное", "Useful links"),
                    color = Color(0xFF8BA3A4),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 12.dp, bottom = 10.dp, start = 2.dp)
                )

                ByMeCardGroup {
                    ByMeSettingRow(
                        title = t("Ответы на вопросы", "FAQ"),
                        subtitle = t("Оплата, подключение и устройства", "Payments, connection and devices"),
                        icon = { QuestionHelpIcon(color = Color(0xFF62CBB0), size = 20.dp) },
                        trailing = { ChevronRightIcon(color = Color(0xFF71898A), size = 18.dp) },
                        onClick = { showFaqDialog = true }
                    )
                }

                // Section: Информация
                Text(
                    text = t("Информация", "Information"),
                    color = Color(0xFF8BA3A4),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 22.dp, bottom = 10.dp, start = 2.dp)
                )

                ByMeCardGroup {
                    ByMeSettingRow(
                        title = t("О приложении", "About the app"),
                        subtitle = t("Версия 1.0.0", "Version 1.0.0"),
                        icon = { InfoCircleIcon(color = Color(0xFF62CBB0), size = 20.dp) },
                        trailing = { ChevronRightIcon(color = Color(0xFF71898A), size = 18.dp) }
                    )
                    ByMeSettingRow(
                        title = t("Документы и приватность", "Privacy and documents"),
                        subtitle = null,
                        icon = { ShieldSecurityIcon(color = Color(0xFF62CBB0), size = 20.dp) },
                        trailing = { ChevronRightIcon(color = Color(0xFF71898A), size = 18.dp) },
                        onClick = { showTermsDialog = true },
                        showBottomDivider = false
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = t("ByMeVPN · Скорость. Свобода. Простота.", "ByMeVPN · Speed. Freedom. Simplicity."),
                    color = Color(0xFF617E79),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
                )
            }

            ByMeBottomNav(
                currentTab = BottomNavTab.Settings,
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
    }
}
