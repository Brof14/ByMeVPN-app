package com.example.bymevpn.screens

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bymevpn.components.ChevronRightIcon
import com.example.bymevpn.components.CosmicGlobeBackground
import com.example.bymevpn.components.EyeIcon
import com.example.bymevpn.components.GoogleLogoIcon
import com.example.bymevpn.components.LockIcon
import com.example.bymevpn.components.MailIcon
import com.example.bymevpn.components.PaperPlaneIcon
import com.example.bymevpn.components.ShieldLogo
import com.example.bymevpn.data.AccountRepository
import com.example.bymevpn.data.api.ByMeApiClient
import com.example.bymevpn.data.auth.TelegramAuthPollPolicy
import com.example.bymevpn.data.LocaleManager
import com.example.bymevpn.theme.AppColors
import com.example.bymevpn.theme.AppGradients
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton

private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

/**
 * Main Sign-In Screen styled to match the user's reference design to the pixel:
 * 1. Deep space backdrop with Earth curvature, atmosphere halo and global network arcs
 * 2. ByMeVPN shield logo with neon infinity loop and ambient backglow
 * 3. Prominent Google Sign-In button with white pill card and right chevron
 * 4. Distinct "или по почте" horizontal divider
 * 5. High-contrast dark inputs for email and password
 * 6. Vibrant neon green "Войти" button with right chevron
 * 7. Optional Telegram compatibility card for legacy bot accounts
 * 8. "Нет аккаунта? Зарегистрироваться >" footer link
 */
@Composable
fun SignInScreen(
    onSignUpClick: () -> Unit = {},
    onSignInSuccess: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentLanguage by LocaleManager.currentLanguage.collectAsState()
    val isRu = remember(currentLanguage) { LocaleManager.isRussian(language = currentLanguage) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val compactScreen = LocalConfiguration.current.screenHeightDp < 700
    val scrollState = rememberScrollState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var authError by remember { mutableStateOf<String?>(null) }
    var isEmailLoading by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }
    var isTelegramLoading by remember { mutableStateOf(false) }
    var showPasswordReset by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    var resetPending by remember { mutableStateOf(false) }
    var resetSent by remember { mutableStateOf(false) }
    var resetError by remember { mutableStateOf<String?>(null) }
    var telegramLoginJob by remember { mutableStateOf<Job?>(null) }
    var telegramStatusText by remember { mutableStateOf<String?>(null) }
    var telegramBotUrl by remember { mutableStateOf<String?>(null) }
    var telegramWaitSeconds by remember { mutableIntStateOf(0) }

    androidx.compose.runtime.LaunchedEffect(isTelegramLoading) {
        if (isTelegramLoading) {
            telegramWaitSeconds = 0
            while (isTelegramLoading) {
                kotlinx.coroutines.delay(1000)
                telegramWaitSeconds += 1
            }
        }
    }

    fun submitLogin() {
        focusManager.clearFocus()
        val trimmed = email.trim()
        var valid = true

        if (trimmed.isEmpty() || !EMAIL_REGEX.matches(trimmed)) {
            emailError = if (isRu) "Введите корректный email" else "Please enter a valid email"
            valid = false
        } else {
            emailError = null
        }

        if (password.length < 6) {
            passwordError = if (isRu) "Пароль должен содержать от 6 символов" else "Password must be at least 6 characters"
            valid = false
        } else {
            passwordError = null
        }

        if (valid) {
            isEmailLoading = true
            authError = null
            scope.launch {
                val result = AccountRepository.loginWithEmail(context, trimmed, password)
                isEmailLoading = false
                if (result.isSuccess) {
                    onSignInSuccess()
                } else {
                    val raw = result.exceptionOrNull()?.message ?: (if (isRu) "Неверный логин или пароль" else "Invalid email or password")
                    authError = raw.replace(Regex("^\\[[A-Za-z0-9_]+\\]\\s*"), "")
                }
            }
        }
    }

    if (showPasswordReset) {
        AlertDialog(
            onDismissRequest = { if (!resetPending) showPasswordReset = false },
            containerColor = AppColors.BgSurfaceElevated,
            titleContentColor = AppColors.TextPrimary,
            textContentColor = AppColors.TextSecondary,
            title = { Text(if (isRu) "Восстановление пароля" else "Reset password") },
            text = {
                Column {
                    if (resetSent) {
                        Text(if (isRu) "Если аккаунт с этим email существует, мы отправим ссылку для смены пароля." else "If an account exists for this email, a password reset link will be sent.")
                    } else {
                        Text(if (isRu) "Введите email аккаунта. Ссылка придёт на почту." else "Enter your account email to receive a reset link.")
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = resetEmail,
                            onValueChange = { resetEmail = it; resetError = null },
                            label = { Text("Email") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            enabled = !resetPending,
                            isError = resetError != null
                        )
                        resetError?.let { Text(it, color = AppColors.StatusError, fontSize = 13.sp) }
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = !resetPending, onClick = {
                    if (resetSent) {
                        showPasswordReset = false
                    } else if (!EMAIL_REGEX.matches(resetEmail.trim())) {
                        resetError = if (isRu) "Введите корректный email" else "Enter a valid email"
                    } else {
                        resetPending = true
                        scope.launch {
                            try {
                                ByMeApiClient(context).requestPasswordReset(resetEmail)
                                resetSent = true
                            } catch (e: Exception) {
                                resetError = if (isRu) "Не удалось отправить запрос. Попробуйте позже." else "Could not send the request. Please try again later."
                            } finally {
                                resetPending = false
                            }
                        }
                    }
                }) {
                    Text(
                        text = if (resetSent) "OK" else if (resetPending) "…" else if (isRu) "Отправить" else "Send",
                        color = AppColors.SkyBlue
                    )
                }
            },
            dismissButton = {
                if (!resetSent) TextButton(enabled = !resetPending, onClick = { showPasswordReset = false }) {
                    Text(if (isRu) "Отмена" else "Cancel", color = AppColors.TextSecondary)
                }
            }
        )
    }

    if (isTelegramLoading) {
        androidx.compose.ui.window.Dialog(onDismissRequest = {}) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(AppColors.BgSurfaceElevated)
                    .border(1.2.dp, AppColors.BorderAccent, RoundedCornerShape(24.dp))
                    .padding(22.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Brush.verticalGradient(listOf(AppColors.BrandBlue, Color(0xFF0055CC)))),
                        contentAlignment = Alignment.Center
                    ) {
                        PaperPlaneIcon(color = Color.White, size = 21.dp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isRu) "Вход через Telegram" else "Telegram sign-in",
                            color = AppColors.TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isRu) "Осталось подтвердить в боте" else "One confirmation in the bot",
                            color = AppColors.TextSecondary,
                            fontSize = 11.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                TelegramStep(1, if (isRu) "Откройте @ByMeVPNBot в Telegram" else "Open @ByMeVPNBot in Telegram")
                TelegramStep(2, if (isRu) "Нажмите «Старт» в чате с ботом" else "Tap Start in the bot chat")
                TelegramStep(3, if (isRu) "Вернитесь в приложение — вход подтвердится сам" else "Return to the app — sign-in completes automatically")

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(AppColors.BgSurfaceAccent)
                        .border(1.dp, AppColors.BorderAccent, RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(
                        color = AppColors.SkyBlue,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = telegramStatusText ?: (if (isRu) "Ожидаем подтверждение…" else "Waiting for confirmation…"),
                            color = AppColors.TextPrimary,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isRu) "Проверяем автоматически · ${telegramWaitSeconds} с" else "Checking automatically · ${telegramWaitSeconds}s",
                            color = AppColors.TextSecondary,
                            fontSize = 10.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(AppGradients.PrimaryButton)
                        .clickable {
                            telegramBotUrl?.let {
                                com.example.bymevpn.data.auth.TelegramLinkOpener.open(context, it)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isRu) "Открыть Telegram снова" else "Open Telegram again",
                        color = AppColors.BrandMintDarkText,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .clickable {
                            telegramLoginJob?.cancel()
                            isTelegramLoading = false
                            telegramStatusText = null
                            telegramBotUrl = null
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isRu) "Отмена" else "Cancel",
                        color = AppColors.TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    CosmicGlobeBackground(modifier = modifier) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 480.dp)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(18.dp))

                    // 1. Shield Logo with ambient glowing halo
                    ShieldLogo(
                        size = if (compactScreen) 106.dp else 150.dp,
                        isActive = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. Title: ByMe (White) + VPN (Cyan)
                    val brandText = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                color = AppColors.TextPrimary,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        ) {
                            append("ByMe")
                        }
                        withStyle(
                            SpanStyle(
                                color = AppColors.SkyBlue,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                        ) {
                            append("VPN")
                        }
                    }
                    Text(
                        text = brandText,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // 3. Slogan: Скорость. Свобода. Простота.
                    Text(
                        text = if (isRu) "VPN для ваших устройств" else "VPN for your devices",
                        color = AppColors.TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    if (com.example.bymevpn.data.api.ApiConfig.GOOGLE_AUTH_SERVER_VERIFIED) {
                    // 4. Primary 1-Tap Google Sign-In Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .drawBehind {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(Color(0x2800D8FF), Color.Transparent),
                                        center = Offset(size.width / 2f, size.height + 4f),
                                        radius = size.width * 0.45f
                                    ),
                                    radius = size.width * 0.45f,
                                    center = Offset(size.width / 2f, size.height + 4f)
                                )
                            }
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White)
                            .clickable(enabled = !isGoogleLoading && !isEmailLoading) {
                                isGoogleLoading = true
                                authError = null
                                scope.launch {
                                    val result = AccountRepository.loginWithGoogle(context)
                                    isGoogleLoading = false
                                    if (result.isSuccess) {
                                        onSignInSuccess()
                                    } else {
                                        val message = com.example.bymevpn.data.api.AccountActionErrorMessages
                                            .googleSignIn(result.exceptionOrNull(), isRu)
                                        authError = message.takeIf { it.isNotBlank() }
                                    }
                                }
                            }
                            .testTag("google_login_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isGoogleLoading) {
                            CircularProgressIndicator(
                                color = Color(0xFF14191F),
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                GoogleLogoIcon(size = 22.dp)
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = if (isRu) "Продолжить с Google" else "Continue with Google",
                                    color = Color(0xFF14191F),
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                ChevronRightIcon(color = Color(0xFF14191F), size = 18.dp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // 5. Divider: или по почте
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(AppColors.BorderDefault)
                        )
                        Text(
                            text = if (isRu) "или по почте" else "or via email",
                            color = AppColors.TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(AppColors.BorderDefault)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    }

                    // 6. Email Input Field
                    ExactInputField(
                        value = email,
                        onValueChange = {
                            email = it
                            emailError = null
                            authError = null
                        },
                        hintText = if (isRu) "Адрес эл. почты" else "Email address",
                        leadingIcon = { MailIcon(color = AppColors.TextMuted, size = 20.dp) },
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                        isError = emailError != null,
                        testTag = "email_input"
                    )

                    if (emailError != null) {
                        Text(
                            text = emailError ?: "",
                            color = AppColors.StatusError,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 6.dp, top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 7. Password Input Field
                    ExactInputField(
                        value = password,
                        onValueChange = {
                            password = it
                            passwordError = null
                            authError = null
                        },
                        hintText = if (isRu) "Пароль" else "Password",
                        leadingIcon = { LockIcon(color = AppColors.TextMuted, size = 20.dp) },
                        trailingIcon = {
                            IconButton(
                                onClick = { passwordVisible = !passwordVisible },
                                modifier = Modifier.size(48.dp).semantics {
                                    contentDescription = if (passwordVisible) {
                                        if (isRu) "Скрыть пароль" else "Hide password"
                                    } else {
                                        if (isRu) "Показать пароль" else "Show password"
                                    }
                                }
                            ) {
                                EyeIcon(visible = passwordVisible, color = AppColors.TextMuted, size = 20.dp)
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                        onImeAction = { submitLogin() },
                        isError = passwordError != null,
                        testTag = "password_input"
                    )

                    if (passwordError != null) {
                        Text(
                            text = passwordError ?: "",
                            color = AppColors.StatusError,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 6.dp, top = 4.dp)
                        )
                    }

                    TextButton(
                        onClick = {
                            resetEmail = email.trim()
                            resetError = null
                            resetSent = false
                            showPasswordReset = true
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            text = if (isRu) "Забыли пароль?" else "Forgot password?",
                            color = AppColors.SkyBlue
                        )
                    }

                    if (authError != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = authError ?: "",
                            color = AppColors.StatusError,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // 8. Main "Войти" Button (Logo Cyan-Mint Gradient with Chevron)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .drawBehind {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(Color(0x4500D8FF), Color.Transparent),
                                        center = Offset(size.width / 2f, size.height + 4f),
                                        radius = size.width * 0.45f
                                    ),
                                    radius = size.width * 0.45f,
                                    center = Offset(size.width / 2f, size.height + 4f)
                                )
                            }
                            .clip(RoundedCornerShape(18.dp))
                            .background(AppGradients.PrimaryButton)
                            .clickable(enabled = !isEmailLoading && !isGoogleLoading) { submitLogin() }
                            .testTag("login_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isEmailLoading) {
                            CircularProgressIndicator(
                                color = AppColors.BrandMintDarkText,
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 22.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = if (isRu) "Войти" else "Sign In",
                                    color = AppColors.BrandMintDarkText,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                ChevronRightIcon(color = AppColors.BrandMintDarkText, size = 18.dp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 9. Dedicated Telegram-Bot Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawBehind {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(Color(0x280077FF), Color.Transparent),
                                        center = Offset(size.width / 2f, size.height / 2f),
                                        radius = size.width * 0.52f
                                    ),
                                    radius = size.width * 0.52f,
                                    center = Offset(size.width / 2f, size.height / 2f)
                                )
                            }
                            .clip(RoundedCornerShape(22.dp))
                            .background(AppColors.BgSurface)
                            .border(1.2.dp, AppColors.BorderTelegram, RoundedCornerShape(22.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Telegram circle badge
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(AppColors.BrandBlue, Color(0xFF0055CC))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    PaperPlaneIcon(color = Color.White, size = 22.dp)
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isRu) "Войти через Telegram" else "Continue with Telegram",
                                        color = AppColors.TextPrimary,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 19.sp
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = if (isRu) "Подтвердите вход в боте. Для восстановления подписки используйте прежний Telegram-аккаунт." else "Confirm in the bot. Use your original Telegram account to restore an existing subscription.",
                                        color = AppColors.TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Inner "Войти через Telegram" Button
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(AppGradients.PrimaryButton)
                                    .clickable(enabled = !isTelegramLoading) {
                                        isTelegramLoading = true
                                        authError = null
                                        telegramStatusText = if (isRu) "Открываем Telegram..." else "Opening Telegram..."
                                        telegramLoginJob = scope.launch {
                                            val sessionRes = AccountRepository.createTelegramAuthSession(context)
                                            if (sessionRes.isSuccess) {
                                                val session = sessionRes.getOrThrow()
                                                if (!com.example.bymevpn.data.auth.TelegramLinkOpener.open(context, session.botUrl)) {
                                                    isTelegramLoading = false
                                                    telegramStatusText = null
                                                    authError = if (isRu) "Не удалось открыть Telegram. Попробуйте ещё раз." else "Could not open Telegram. Please try again."
                                                    return@launch
                                                }
                                                telegramBotUrl = session.botUrl
                                                telegramStatusText = if (isRu) "Ожидаем подтверждение в боте…" else "Waiting for confirmation in the bot…"

                                                var confirmed = false
                                                var transientFailures = 0
                                                var checkError: Throwable? = null
                                                for (attempt in 1..40) {
                                                    val checkRes = AccountRepository.checkTelegramAuthSession(context, session.sessionToken)
                                                    if (checkRes.isSuccess && checkRes.getOrNull() != null) {
                                                        confirmed = true
                                                        break
                                                    }
                                                    val error = checkRes.exceptionOrNull()
                                                    if (error != null) {
                                                        if (TelegramAuthPollPolicy.shouldRetry(error, transientFailures)) {
                                                            transientFailures++
                                                            kotlinx.coroutines.delay(1500)
                                                            continue
                                                        }
                                                        checkError = error
                                                        break
                                                    }
                                                    if (attempt % 7 == 0) {
                                                        telegramStatusText = if (isRu) "Всё ещё ждём подтверждение в боте…" else "Still waiting for the bot confirmation…"
                                                    }
                                                    kotlinx.coroutines.delay(1500)
                                                }
                                                isTelegramLoading = false
                                                telegramStatusText = null
                                                telegramBotUrl = null
                                                if (confirmed) {
                                                    onSignInSuccess()
                                                } else if (checkError != null) {
                                                    authError = com.example.bymevpn.data.api.AccountActionErrorMessages.telegramSignInCheck(checkError, isRu)
                                                } else {
                                                    authError = if (isRu) "Подтверждение не получено. Начните вход заново и нажмите «Старт» в боте." else "Confirmation was not received. Start again and tap Start in the bot."
                                                }
                                            } else {
                                                isTelegramLoading = false
                                                telegramStatusText = null
                                                authError = com.example.bymevpn.data.api.AccountActionErrorMessages.telegramSignInStart(
                                                    sessionRes.exceptionOrNull(), isRu
                                                )
                                            }
                                        }
                                    }
                                    .testTag("telegram_login_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isTelegramLoading) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(color = AppColors.BrandMintDarkText, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = telegramStatusText ?: (if (isRu) "Вход..." else "Logging in..."),
                                            color = AppColors.BrandMintDarkText,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        PaperPlaneIcon(color = AppColors.BrandMintDarkText, size = 18.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isRu) "Продолжить через Telegram" else "Continue with Telegram",
                                            color = AppColors.BrandMintDarkText,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        ChevronRightIcon(color = AppColors.BrandMintDarkText, size = 18.dp)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 10. Footer: "Нет аккаунта? Зарегистрироваться >"
                    Row(
                        modifier = Modifier
                            .clickable(onClick = onSignUpClick)
                            .padding(vertical = 12.dp)
                            .testTag("signup_link"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isRu) "Нет аккаунта? " else "Don't have an account? ",
                            color = AppColors.TextSecondary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Normal
                        )
                        Text(
                            text = if (isRu) "Зарегистрироваться" else "Sign Up",
                            color = AppColors.SkyBlue,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        ChevronRightIcon(color = AppColors.SkyBlue, size = 14.dp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun TelegramStep(number: Int, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(AppColors.BgSurfaceAccent),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$number",
                color = AppColors.SkyBlue,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            color = AppColors.TextSecondary,
            fontSize = 12.5.sp,
            lineHeight = 17.sp
        )
    }
}

@Composable
private fun ExactInputField(
    value: String,
    onValueChange: (String) -> Unit,
    hintText: String,
    leadingIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    trailingIcon: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    onImeAction: () -> Unit = {},
    isError: Boolean = false,
    testTag: String = ""
) {
    var isFocused by remember { mutableStateOf(false) }

    val borderColor = when {
        isError -> AppColors.StatusError
        isFocused -> AppColors.SkyBlue
        else -> AppColors.BorderInput
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.BgInput)
            .border(1.2.dp, borderColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp)
            .testTag(testTag),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingIcon()
            Spacer(modifier = Modifier.width(12.dp))

            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = hintText,
                        color = AppColors.TextMuted,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = AppColors.TextPrimary,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    visualTransformation = visualTransformation,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = keyboardType,
                        imeAction = imeAction
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { onImeAction() },
                        onNext = { onImeAction() }
                    ),
                    cursorBrush = SolidColor(AppColors.SkyBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = hintText }
                        .onFocusChanged { isFocused = it.isFocused }
                )
            }

            if (trailingIcon != null) {
                trailingIcon()
            }
        }
    }
}
