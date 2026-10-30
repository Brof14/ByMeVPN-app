package com.example.bymevpn.screens

import android.content.Intent
import android.net.Uri
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
import com.example.bymevpn.components.BellNewsIcon
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

@Composable
fun SupportScreen(
    onBackClick: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
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
                title = "Помощь",
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
                        text = "Мы рядом, если\nнужна помощь",
                        color = AppColors.TextPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 33.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Нашли ошибку или придумали улучшение? Напишите нам: за подтверждённые ошибки и принятые предложения добавим дни к подписке.",
                        color = Color(0xFF92AAA4),
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    ByMePrimaryButton(
                        text = "Написать в поддержку",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/bymevpn_support_bot"))
                            context.startActivity(intent)
                        }
                    )
                }

                // Section: Полезное
                Text(
                    text = "Полезное",
                    color = Color(0xFF8BA3A4),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 12.dp, bottom = 10.dp, start = 2.dp)
                )

                ByMeCardGroup {
                    ByMeSettingRow(
                        title = "Ответы на вопросы",
                        subtitle = "Оплата, подключение и устройства",
                        icon = { QuestionHelpIcon(color = Color(0xFF62CBB0), size = 20.dp) },
                        trailing = { ChevronRightIcon(color = Color(0xFF71898A), size = 18.dp) },
                        onClick = { showFaqDialog = true }
                    )
                    ByMeSettingRow(
                        title = "Новости ByMeVPN",
                        subtitle = "Обновления и работа сервиса",
                        icon = { BellNewsIcon(color = Color(0xFF62CBB0), size = 20.dp) },
                        trailing = { ChevronRightIcon(color = Color(0xFF71898A), size = 18.dp) },
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/bymevpn"))
                            context.startActivity(intent)
                        },
                        showBottomDivider = false
                    )
                }

                // Section: Информация
                Text(
                    text = "Информация",
                    color = Color(0xFF8BA3A4),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 22.dp, bottom = 10.dp, start = 2.dp)
                )

                ByMeCardGroup {
                    ByMeSettingRow(
                        title = "О приложении",
                        subtitle = "Версия 1.2.0 (Build 42)",
                        icon = { InfoCircleIcon(color = Color(0xFF62CBB0), size = 20.dp) },
                        trailing = { ChevronRightIcon(color = Color(0xFF71898A), size = 18.dp) }
                    )
                    ByMeSettingRow(
                        title = "Документы и приватность",
                        subtitle = null,
                        icon = { ShieldSecurityIcon(color = Color(0xFF62CBB0), size = 20.dp) },
                        trailing = { ChevronRightIcon(color = Color(0xFF71898A), size = 18.dp) },
                        onClick = { showTermsDialog = true },
                        showBottomDivider = false
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "ByMeVPN · Скорость. Свобода. Простота.",
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
