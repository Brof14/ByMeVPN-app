package com.example.bymevpn.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bymevpn.components.EyeIcon
import com.example.bymevpn.components.GoogleLogoIcon
import com.example.bymevpn.components.LockIcon
import com.example.bymevpn.components.MailIcon
import com.example.bymevpn.components.PaperPlaneIcon
import com.example.bymevpn.components.ShieldLogo
import com.example.bymevpn.components.TelegramSignInDialog
import com.example.bymevpn.data.AccountRepository
import com.example.bymevpn.data.LocaleManager
import com.example.bymevpn.data.api.AccountActionErrorMessages
import com.example.bymevpn.data.api.ApiConfig
import com.example.bymevpn.data.auth.TelegramAuthPollPolicy
import com.example.bymevpn.data.auth.TelegramLinkOpener
import com.example.bymevpn.theme.AppColors
import com.example.bymevpn.theme.AppGradients
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

@Composable
fun SignUpScreen(
    onSignInClick: () -> Unit = {},
    onSignUpSuccess: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentLanguage by LocaleManager.currentLanguage.collectAsState()
    val isRu = remember(currentLanguage) { LocaleManager.isRussian(language = currentLanguage) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmError by remember { mutableStateOf<String?>(null) }
    var authError by remember { mutableStateOf<String?>(null) }
    var isEmailLoading by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }
    var isTelegramLoading by remember { mutableStateOf(false) }
    var telegramLoginJob by remember { mutableStateOf<Job?>(null) }
    var telegramStatusText by remember { mutableStateOf<String?>(null) }
    var telegramBotUrl by remember { mutableStateOf<String?>(null) }
    var telegramWaitSeconds by remember { mutableIntStateOf(0) }

    LaunchedEffect(isTelegramLoading) {
        if (isTelegramLoading) {
            telegramWaitSeconds = 0
            while (isTelegramLoading) {
                delay(1000)
                telegramWaitSeconds += 1
            }
        }
    }

    fun submitSignUp() {
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

        if (confirmPassword != password) {
            confirmError = if (isRu) "Пароли не совпадают" else "Passwords do not match"
            valid = false
        } else {
            confirmError = null
        }

        if (valid) {
            isEmailLoading = true
            authError = null
            scope.launch {
                val result = AccountRepository.registerWithEmail(context, trimmed, password)
                isEmailLoading = false
                if (result.isSuccess) {
                    onSignUpSuccess()
                } else {
                    val raw = result.exceptionOrNull()?.message
                        ?: (if (isRu) "Ошибка регистрации" else "Registration failed")
                    authError = raw.replace(Regex("^\\[[A-Za-z0-9_]+\\]\\s*"), "")
                }
            }
        }
    }

    fun startTelegramSignUp() {
        focusManager.clearFocus()
        isTelegramLoading = true
        authError = null
        telegramStatusText = if (isRu) "Открываем Telegram..." else "Opening Telegram..."
        telegramLoginJob = scope.launch {
            val sessionRes = AccountRepository.createTelegramAuthSession(context)
            if (sessionRes.isSuccess) {
                val session = sessionRes.getOrThrow()
                if (!TelegramLinkOpener.open(context, session.botUrl)) {
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
                            delay(1500)
                            continue
                        }
                        checkError = error
                        break
                    }
                    if (attempt % 7 == 0) {
                        telegramStatusText = if (isRu) "Всё ещё ждём подтверждение в боте…" else "Still waiting for the bot confirmation…"
                    }
                    delay(1500)
                }
                isTelegramLoading = false
                telegramStatusText = null
                telegramBotUrl = null
                if (confirmed) {
                    onSignUpSuccess()
                } else if (checkError != null) {
                    authError = AccountActionErrorMessages.telegramSignInCheck(checkError, isRu)
                } else {
                    authError = if (isRu) "Подтверждение не получено. Начните вход заново и нажмите «Старт» в боте." else "Confirmation was not received. Start again and tap Start in the bot."
                }
            } else {
                isTelegramLoading = false
                telegramStatusText = null
                authError = AccountActionErrorMessages.telegramSignInStart(sessionRes.exceptionOrNull(), isRu)
            }
        }
    }

    if (isTelegramLoading) {
        TelegramSignInDialog(
            isRu = isRu,
            statusText = telegramStatusText,
            waitSeconds = telegramWaitSeconds,
            onOpenTelegramAgain = {
                telegramBotUrl?.let { TelegramLinkOpener.open(context, it) }
            },
            onCancel = {
                telegramLoginJob?.cancel()
                isTelegramLoading = false
                telegramStatusText = null
                telegramBotUrl = null
            }
        )
    }

    val anyLoading = isEmailLoading || isGoogleLoading || isTelegramLoading

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AppColors.BgCanvas,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState, modifier = Modifier.padding(bottom = 24.dp))
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.BgCanvas)
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            val compactScreen = maxHeight < 680.dp
            val logoSize = if (compactScreen) 52.dp else 60.dp

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AuthBrandPanel(
                    isRu = isRu,
                    isSignUp = true,
                    compact = compactScreen
                )

                Spacer(modifier = Modifier.height(if (compactScreen) 16.dp else 20.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(AppColors.BgSurface)
                        .border(1.dp, AppColors.BorderDefault, RoundedCornerShape(20.dp))
                        .padding(18.dp)
                ) {
                    SignUpInputField(
                        value = email,
                        onValueChange = {
                            email = it
                            emailError = null
                            authError = null
                        },
                        hintText = if (isRu) "Email" else "Email address",
                        leadingIcon = { MailIcon(color = if (emailError != null) AppColors.StatusError else AppColors.TextMuted, size = 18.dp) },
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                        isError = emailError != null
                    )

                    if (emailError != null) {
                        Text(
                            text = emailError ?: "",
                            color = AppColors.StatusError,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp, top = 5.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    SignUpInputField(
                        value = password,
                        onValueChange = {
                            password = it
                            passwordError = null
                            authError = null
                        },
                        hintText = if (isRu) "Пароль (от 6 символов)" else "Password (6+ characters)",
                        leadingIcon = { LockIcon(color = if (passwordError != null) AppColors.StatusError else AppColors.TextMuted, size = 18.dp) },
                        trailingIcon = {
                            IconButton(
                                onClick = { passwordVisible = !passwordVisible },
                                modifier = Modifier.size(44.dp).semantics {
                                    contentDescription = if (passwordVisible) {
                                        if (isRu) "Скрыть пароль" else "Hide password"
                                    } else {
                                        if (isRu) "Показать пароль" else "Show password"
                                    }
                                }
                            ) {
                                EyeIcon(visible = passwordVisible, color = AppColors.TextMuted, size = 18.dp)
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next,
                        isError = passwordError != null
                    )

                    if (passwordError != null) {
                        Text(
                            text = passwordError ?: "",
                            color = AppColors.StatusError,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp, top = 5.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    SignUpInputField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            confirmError = null
                            authError = null
                        },
                        hintText = if (isRu) "Повторите пароль" else "Confirm password",
                        leadingIcon = { LockIcon(color = if (confirmError != null) AppColors.StatusError else AppColors.TextMuted, size = 18.dp) },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                        onImeAction = { submitSignUp() },
                        isError = confirmError != null
                    )

                    if (confirmError != null) {
                        Text(
                            text = confirmError ?: "",
                            color = AppColors.StatusError,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp, top = 5.dp)
                        )
                    }

                    if (authError != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(AppColors.BadgeRedBg)
                                .border(1.dp, AppColors.StatusError.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = authError ?: "",
                                color = AppColors.BadgeRedText,
                                fontSize = 12.5.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(AppGradients.PrimaryButton)
                            .clickable(enabled = !anyLoading) { submitSignUp() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isEmailLoading) {
                            CircularProgressIndicator(color = AppColors.BrandMintDarkText, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text(
                                text = if (isRu) "Создать аккаунт" else "Create Account",
                                color = AppColors.BrandMintDarkText,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = AppColors.BorderSubtle, thickness = 1.dp)
                    Text(
                        text = if (isRu) "или войти через" else "or continue with",
                        color = AppColors.TextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = AppColors.BorderSubtle, thickness = 1.dp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (ApiConfig.GOOGLE_AUTH_SERVER_VERIFIED) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(AppColors.BgSurface)
                                .border(1.dp, AppColors.BorderDefault, RoundedCornerShape(13.dp))
                                .clickable(enabled = !anyLoading) {
                                    focusManager.clearFocus()
                                    isGoogleLoading = true
                                    authError = null
                                    scope.launch {
                                        val result = AccountRepository.loginWithGoogle(context)
                                        isGoogleLoading = false
                                        if (result.isSuccess) {
                                            onSignUpSuccess()
                                        } else {
                                            val message = AccountActionErrorMessages.googleSignIn(result.exceptionOrNull(), isRu)
                                            authError = message.takeIf { it.isNotBlank() }
                                        }
                                    }
                                }
                                .testTag("google_login_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isGoogleLoading) {
                                CircularProgressIndicator(color = AppColors.TextPrimary, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    GoogleLogoIcon(size = 18.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Google",
                                        color = AppColors.TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(AppColors.BgSurface)
                            .border(1.dp, AppColors.BorderDefault, RoundedCornerShape(13.dp))
                            .clickable(enabled = !anyLoading) { startTelegramSignUp() }
                            .testTag("telegram_login_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isTelegramLoading) {
                            CircularProgressIndicator(
                                color = AppColors.SkyBlue,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                PaperPlaneIcon(color = AppColors.TelegramBlue, size = 17.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Telegram",
                                    color = AppColors.TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onSignInClick)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isRu) "Уже есть аккаунт? " else "Already have an account? ",
                        color = AppColors.TextSecondary,
                        fontSize = 13.5.sp
                    )
                    Text(
                        text = if (isRu) "Войти" else "Sign In",
                        color = AppColors.SkyBlue,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SignUpInputField(
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
    isError: Boolean = false
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
            .height(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.BgInput)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingIcon()
            Spacer(modifier = Modifier.width(11.dp))

            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = hintText,
                        color = AppColors.TextMuted,
                        fontSize = 14.sp
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
