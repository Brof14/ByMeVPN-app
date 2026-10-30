package com.example.bymevpn.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.example.bymevpn.components.GoogleLogoIcon
import com.example.bymevpn.components.MailIcon
import com.example.bymevpn.components.PaywallSheet
import com.example.bymevpn.components.ShieldLogo
import com.example.bymevpn.components.TelegramLogoIcon
import com.example.bymevpn.data.AccountRepository
import com.example.bymevpn.data.ClipboardKeyDetector
import com.example.bymevpn.data.LocaleManager
import com.example.bymevpn.data.subscription.SubscriptionManager
import com.example.bymevpn.theme.AppColors
import com.example.bymevpn.vpn.VpnManager
import kotlinx.coroutines.launch

/**
 * 01 Welcome & Authentication Screen tailored for Google Play launch:
 * 1. Prominent 1-tap Google Sign-In as primary method
 * 2. Email sign in & registration
 * 3. Dedicated prompt: «Уже пользовались через Telegram-бота?» with direct Telegram button
 * 4. Direct subscription link / clipboard auto-import
 */
@Composable
fun WelcomeScreen(
    onSignUpClick: () -> Unit = {},
    onLogInClick: () -> Unit = {},
    onLoginSuccess: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentLanguage by LocaleManager.currentLanguage.collectAsState()
    val isRu = remember(currentLanguage) { LocaleManager.isRussian(language = currentLanguage) }
    val scope = rememberCoroutineScope()

    var isGoogleLoading by remember { mutableStateOf(false) }
    var isTelegramLoading by remember { mutableStateOf(false) }
    var telegramStatusText by remember { mutableStateOf<String?>(null) }
    var authError by remember { mutableStateOf<String?>(null) }
    var showPaywall by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }

    var subInputUrl by remember { mutableStateOf("") }
    var isSubLoading by remember { mutableStateOf(false) }
    var subError by remember { mutableStateOf<String?>(null) }
    var showInputCard by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        val detected = ClipboardKeyDetector.detectKey(context)
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
                .padding(horizontal = 22.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            // 1. Top Brand Logo & Shield
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ShieldLogo(
                    size = 105.dp,
                    isActive = true
                )

                Spacer(modifier = Modifier.height(12.dp))

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
                    text = if (isRu) "Скорость. Анонимность. Честность." else "Speed. Anonymity. Honesty.",
                    color = AppColors.TextSecondary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Main Authentication Card (Google Play standard)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(AppColors.CardBg)
                    .border(1.2.dp, AppColors.CardBorder, RoundedCornerShape(22.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isRu) "Добро пожаловать" else "Welcome",
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isRu) "Выберите удобный способ входа в приложение" else "Choose your preferred sign-in method",
                    color = AppColors.TextSecondary,
                    fontSize = 12.5.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                // PRIMARY 1-TAP ACTION: Google Sign-In
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .clickable(enabled = !isGoogleLoading) {
                            isGoogleLoading = true
                            authError = null
                            scope.launch {
                                val result = AccountRepository.loginWithGoogle(context)
                                isGoogleLoading = false
                                if (result.isSuccess) {
                                    val canConnect = SubscriptionManager.getInstance(context).canConnect()
                                    if (canConnect) {
                                        onLoginSuccess()
                                    } else {
                                        showPaywall = true
                                    }
                                } else {
                                    val rawErr = result.exceptionOrNull()?.message
                                    if (rawErr != "Sign-in cancelled") {
                                        authError = if (isRu) "Не удалось подтвердить вход через Google. Попробуйте позже." else "Could not verify Google sign-in. Please try again later."
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isGoogleLoading) {
                        CircularProgressIndicator(color = AppColors.DarkText, modifier = Modifier.size(22.dp))
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            GoogleLogoIcon(size = 20.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isRu) "Продолжить с Google" else "Continue with Google",
                                color = Color(0xFF1A1A1A),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // SECONDARY ACTION: Email Sign In
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(AppColors.InputBg)
                        .border(1.dp, AppColors.CardBorder, RoundedCornerShape(14.dp))
                        .clickable { onLogInClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MailIcon(size = 18.dp, color = Color.White)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isRu) "Войти по почте" else "Sign In with Email",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (authError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = authError ?: "",
                        color = AppColors.StatusError,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Divider: ИЛИ
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = AppColors.CardBorder, thickness = 1.dp)
                    Text(
                        text = if (isRu) "ИЛИ" else "OR",
                        color = AppColors.TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = AppColors.CardBorder, thickness = 1.dp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // DEDICATED TELEGRAM PROMPT (Per User Request)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0C161C))
                        .border(1.2.dp, AppColors.ElectricBlue.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isRu) "Уже пользовались через Telegram-бота?" else "Already used via Telegram bot?",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (isRu) "Войдите в один клик через вашего бота или вставьте ключ" else "Sign in with 1 tap via bot or paste your access key",
                        color = AppColors.TextSecondary,
                        fontSize = 11.5.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Button: "Войти через Telegram"
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(AppColors.BrandGreen, AppColors.BrandGreenGlow)
                                )
                            )
                            .clickable(enabled = !isTelegramLoading) {
                                isTelegramLoading = true
                                authError = null
                                telegramStatusText = if (isRu) "Открываем Telegram..." else "Opening Telegram..."
                                scope.launch {
                                    val sessionRes = AccountRepository.createTelegramAuthSession(context)
                                    if (sessionRes.isSuccess) {
                                        val session = sessionRes.getOrThrow()
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(session.botUrl))
                                            context.startActivity(intent)
                                        } catch (_: Exception) {
                                            Toast.makeText(context, "Не удалось открыть Telegram", Toast.LENGTH_SHORT).show()
                                        }
                                        telegramStatusText = if (isRu) "Ожидание подтверждения в боте..." else "Waiting for bot confirmation..."

                                        var confirmed = false
                                        for (attempt in 1..40) {
                                            kotlinx.coroutines.delay(1500)
                                            val checkRes = AccountRepository.checkTelegramAuthSession(context, session.sessionToken)
                                            if (checkRes.isSuccess && checkRes.getOrNull() != null) {
                                                confirmed = true
                                                break
                                            }
                                        }
                                        isTelegramLoading = false
                                        telegramStatusText = null
                                        if (confirmed) {
                                            val canConnect = SubscriptionManager.getInstance(context).canConnect()
                                            if (canConnect) {
                                                onLoginSuccess()
                                            } else {
                                                showPaywall = true
                                            }
                                        } else {
                                            authError = if (isRu) "Время ожидания истекло. Нажмите кнопку снова." else "Confirmation timed out. Please try again."
                                        }
                                    } else {
                                        isTelegramLoading = false
                                        telegramStatusText = null
                                        openTelegramBot(context, "start=login")
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isTelegramLoading) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(color = AppColors.DarkText, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = telegramStatusText ?: (if (isRu) "Подключение..." else "Connecting..."),
                                    color = AppColors.DarkText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TelegramLogoIcon(size = 18.dp, color = AppColors.DarkText)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isRu) "Войти через Telegram-бота" else "Log In with Telegram",
                                    color = AppColors.DarkText,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Button: "Подключить по ссылке из бота"
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AppColors.InputBg)
                            .border(1.dp, AppColors.CardBorder, RoundedCornerShape(10.dp))
                            .clickable { showInputCard = !showInputCard },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (showInputCard) (if (isRu) "Скрыть ввод ключа" else "Hide Key Input") else (if (isRu) "Ввести ключ вручную" else "Enter Key Manually"),
                            color = AppColors.SkyBlue,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Expandable Key / Link Input Field
                    AnimatedVisibility(visible = showInputCard) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            BasicTextField(
                                value = subInputUrl,
                                onValueChange = {
                                    subInputUrl = it
                                    subError = null
                                },
                                textStyle = TextStyle(color = Color.White, fontSize = 12.5.sp),
                                cursorBrush = SolidColor(AppColors.BrandGreen),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF070B0E))
                                    .border(1.dp, if (subError != null) AppColors.StatusError else AppColors.InputBorder, RoundedCornerShape(10.dp))
                                    .padding(8.dp),
                                decorationBox = { innerTextField ->
                                    if (subInputUrl.isEmpty()) {
                                        Text(
                                            text = if (isRu) "Вставьте ссылку подписки или vless://..." else "Paste subscription link or vless://...",
                                            color = AppColors.TextMuted,
                                            fontSize = 12.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            )

                            if (subError != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = subError ?: "",
                                    color = AppColors.StatusError,
                                    fontSize = 11.5.sp,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
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
                                                subError = res.exceptionOrNull()?.message ?: (if (isRu) "Ошибка подключения ключа" else "Invalid key")
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSubLoading) {
                                    CircularProgressIndicator(color = AppColors.DarkText, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else {
                                    Text(
                                        text = if (isRu) "Активировать подписку" else "Activate Key",
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

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Footer Links: Sign Up & Telegram Support
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.clickable { onSignUpClick() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isRu) "Нет аккаунта? " else "Don't have an account? ",
                        color = AppColors.TextSecondary,
                        fontSize = 13.5.sp
                    )
                    Text(
                        text = if (isRu) "Зарегистрироваться" else "Sign Up",
                        color = AppColors.BrandGreen,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (isRu) "Поддержка в Telegram: @ByMeVPNSupportBot" else "Telegram Support: @ByMeVPNSupportBot",
                    color = AppColors.TextMuted,
                    fontSize = 11.5.sp,
                    modifier = Modifier.clickable {
                        openTelegramBot(context, null)
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isRu) "Условия использования" else "Terms of Service",
                        color = AppColors.TextMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.clickable { showTermsDialog = true }
                    )
                    Text(
                        text = " • ",
                        color = AppColors.TextMuted,
                        fontSize = 11.sp
                    )
                    Text(
                        text = if (isRu) "Конфиденциальность" else "Privacy Policy",
                        color = AppColors.TextMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.clickable { showPrivacyDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }
    }

    if (showPaywall) {
        PaywallSheet(
            onDismissRequest = {
                showPaywall = false
                onLoginSuccess()
            },
            onSubscriptionActivated = {
                showPaywall = false
                onLoginSuccess()
            }
        )
    }

    if (showPrivacyDialog) {
        com.example.bymevpn.components.PrivacyPolicyDialog(
            isRussian = isRu,
            onDismiss = { showPrivacyDialog = false }
        )
    }

    if (showTermsDialog) {
        com.example.bymevpn.components.TermsOfServiceDialog(
            isRussian = isRu,
            onDismiss = { showTermsDialog = false }
        )
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
