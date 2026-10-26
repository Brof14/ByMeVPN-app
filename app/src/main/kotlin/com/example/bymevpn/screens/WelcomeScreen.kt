package com.example.bymevpn.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bymevpn.components.ShieldLogo
import com.example.bymevpn.components.TelegramLogoIcon
import com.example.bymevpn.data.AccountRepository
import com.example.bymevpn.data.subscription.SubscriptionManager
import com.example.bymevpn.theme.AppColors
import com.example.bymevpn.vpn.VpnManager
import kotlinx.coroutines.launch

/**
 * 01 Welcome Screen matching Figma 1:1:
 * - ByMeVPN Header with Shield & infinity badge
 * - "Создайте подписку в Telegram" main action
 * - "Подключить аккаунт" / paste subscription link directly
 */
@Composable
fun WelcomeScreen(
    onSignUpClick: () -> Unit = {},
    onLogInClick: () -> Unit = {},
    onLoginSuccess: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var subInputUrl by remember { mutableStateOf("") }
    var isSubLoading by remember { mutableStateOf(false) }
    var subError by remember { mutableStateOf<String?>(null) }
    var showInputCard by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        val detected = com.example.bymevpn.data.ClipboardKeyDetector.detectKey(context)
        if (!detected.isNullOrBlank()) {
            subInputUrl = detected
            showInputCard = true
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AppColors.BgCanvas
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // 1. Top Brand Logo & Shield
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ShieldLogo(
                    size = 110.dp,
                    isActive = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = Color.White)) {
                            append("ByMe")
                        }
                        withStyle(SpanStyle(color = AppColors.BrandGreen)) {
                            append("VPN")
                        }
                    },
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Скорость. Анонимность. Честность.",
                    color = AppColors.TextSecondary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            // 2. Central Card: "Создайте подписку в Telegram" (Matching Figma Frame 01)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(AppColors.CardBg)
                    .border(1.2.dp, AppColors.CardBorder, RoundedCornerShape(22.dp))
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Добро пожаловать",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Подключайтесь в один клик. Сервер выберется автоматически.",
                    color = AppColors.TextSecondary,
                    fontSize = 12.5.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Primary Neon Button: "Создайте подписку в Telegram"
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(AppColors.BrandGreen, AppColors.BrandGreenGlow)
                            )
                        )
                        .clickable {
                            openTelegramBot(context, "start=login")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TelegramLogoIcon(size = 20.dp, color = AppColors.DarkText)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Войти через Telegram-бота",
                            color = AppColors.DarkText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Secondary Button: Toggle direct subscription link field
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(AppColors.InputBg)
                        .border(1.dp, AppColors.CardBorder, RoundedCornerShape(14.dp))
                        .clickable { showInputCard = !showInputCard },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (showInputCard) "Скрыть ввод ссылки" else "Подключить по ссылке из бота",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Expandable Link/Key Input field
                if (showInputCard) {
                    Spacer(modifier = Modifier.height(14.dp))

                    BasicTextField(
                        value = subInputUrl,
                        onValueChange = {
                            subInputUrl = it
                            subError = null
                        },
                        textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                        cursorBrush = SolidColor(AppColors.BrandGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(65.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF070B0E))
                            .border(1.dp, if (subError != null) AppColors.StatusError else AppColors.InputBorder, RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        decorationBox = { innerTextField ->
                            if (subInputUrl.isEmpty()) {
                                Text(
                                    text = "Вставьте ссылку подписки или vless://...",
                                    color = AppColors.TextMuted,
                                    fontSize = 12.5.sp
                                )
                            }
                            innerTextField()
                        }
                    )

                    if (subError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = subError ?: "",
                            color = AppColors.StatusError,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AppColors.BrandGreen)
                            .clickable(enabled = !isSubLoading && subInputUrl.isNotBlank()) {
                                isSubLoading = true
                                subError = null
                                scope.launch {
                                    val res = SubscriptionManager.getInstance(context).applySubscriptionUrl(subInputUrl.trim())
                                    isSubLoading = false
                                    if (res.isSuccess) {
                                        AccountRepository.createLocalProfile(context, "ByMe User")
                                        VpnManager.getInstance(context).loadServers()
                                        onLoginSuccess()
                                    } else {
                                        subError = res.exceptionOrNull()?.message ?: "Ошибка подключения подписки"
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSubLoading) {
                            CircularProgressIndicator(color = AppColors.DarkText, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text(
                                text = "Активировать подписку",
                                color = AppColors.DarkText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Bottom Links: Log In via Email or Support
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.clickable { onLogInClick() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Уже есть аккаунт? ",
                        color = AppColors.TextSecondary,
                        fontSize = 13.5.sp
                    )
                    Text(
                        text = "Войти по почте",
                        color = AppColors.BrandGreen,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Нужна помощь? Написать в Telegram",
                    color = AppColors.TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable {
                        openTelegramBot(context, null)
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun openTelegramBot(context: Context, param: String?) {
    val url = if (param != null) "https://t.me/ByMeVPNBot?$param" else "https://t.me/ByMeVPNSupportBot"
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "Не удалось открыть Telegram", Toast.LENGTH_SHORT).show()
    }
}
