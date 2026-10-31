package com.example.bymevpn.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
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
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bymevpn.components.EyeIcon
import com.example.bymevpn.components.GoogleLogoIcon
import com.example.bymevpn.components.LockIcon
import com.example.bymevpn.components.MailIcon
import com.example.bymevpn.components.ShieldLogo
import com.example.bymevpn.components.TelegramLogoIcon
import com.example.bymevpn.data.AccountRepository
import com.example.bymevpn.data.LocaleManager
import com.example.bymevpn.theme.AppColors
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
    var isLoading by remember { mutableStateOf(false) }

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
            isLoading = true
            authError = null
            scope.launch {
                val result = AccountRepository.registerWithEmail(context, trimmed, password)
                isLoading = false
                if (result.isSuccess) {
                    onSignUpSuccess()
                } else {
                    val raw = result.exceptionOrNull()?.message ?: (if (isRu) "Ошибка регистрации" else "Registration failed")
                    authError = raw.replace(Regex("^\\[[A-Za-z0-9_]+\\]\\s*"), "")
                }
            }
        }
    }

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
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            val screenWidth = maxWidth
            val screenHeight = maxHeight
            val logoSize = (screenWidth * 0.35f).coerceIn(100.dp, 130.dp)
            val horizontalPadding = (screenWidth * 0.065f).coerceIn(20.dp, 26.dp)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 500.dp)
                    .verticalScroll(scrollState)
                    .padding(horizontal = horizontalPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(screenHeight * 0.02f))

                ShieldLogo(size = logoSize)

                Spacer(modifier = Modifier.height(14.dp))

                val brandText = buildAnnotatedString {
                    withStyle(SpanStyle(color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)) {
                        append("ByMe")
                    }
                    withStyle(SpanStyle(color = AppColors.BrandGreen, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)) {
                        append("VPN")
                    }
                }
                Text(text = brandText)

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isRu) "Создайте защищенный аккаунт" else "Create your secure account",
                    color = AppColors.TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (com.example.bymevpn.data.api.ApiConfig.GOOGLE_AUTH_SERVER_VERIFIED) {
                // Primary 1-Tap Google Sign-Up
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .clickable(enabled = !isLoading) {
                            isLoading = true
                            authError = null
                            scope.launch {
                                val result = AccountRepository.loginWithGoogle(context)
                                isLoading = false
                                if (result.isSuccess) {
                                    onSignUpSuccess()
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
                    if (isLoading) {
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

                Spacer(modifier = Modifier.height(18.dp))

                // Divider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = AppColors.CardBorder, thickness = 1.dp)
                    Text(
                        text = if (isRu) "ИЛИ ПО ПОЧТЕ" else "OR VIA EMAIL",
                        color = AppColors.TextMuted,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = AppColors.CardBorder, thickness = 1.dp)
                }

                Spacer(modifier = Modifier.height(18.dp))
                }

                // Email
                SignUpInputField(
                    value = email,
                    onValueChange = {
                        email = it
                        emailError = null
                        authError = null
                    },
                    hintText = if (isRu) "Адрес эл. почты" else "Email address",
                    leadingIcon = { MailIcon(color = if (emailError != null) AppColors.StatusError else AppColors.BrandGreen) },
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                    isError = emailError != null
                )

                if (emailError != null) {
                    Text(
                        text = emailError ?: "",
                        color = AppColors.StatusError,
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth().padding(start = 6.dp, top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Password
                SignUpInputField(
                    value = password,
                    onValueChange = {
                        password = it
                        passwordError = null
                        authError = null
                    },
                    hintText = if (isRu) "Пароль (от 6 символов)" else "Password (6+ characters)",
                    leadingIcon = { LockIcon(color = if (passwordError != null) AppColors.StatusError else AppColors.BrandGreen) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            EyeIcon(visible = passwordVisible, color = AppColors.TextSecondary)
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
                        modifier = Modifier.fillMaxWidth().padding(start = 6.dp, top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Confirm Password
                SignUpInputField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        confirmError = null
                        authError = null
                    },
                    hintText = if (isRu) "Повторите пароль" else "Confirm password",
                    leadingIcon = { LockIcon(color = if (confirmError != null) AppColors.StatusError else AppColors.BrandGreen) },
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
                        modifier = Modifier.fillMaxWidth().padding(start = 6.dp, top = 4.dp)
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

                Spacer(modifier = Modifier.height(22.dp))

                // Submit Button
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
                        .clickable(enabled = !isLoading) { submitSignUp() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = AppColors.DarkText, modifier = Modifier.size(22.dp))
                    } else {
                        Text(
                            text = if (isRu) "Создать аккаунт" else "Create Account",
                            color = AppColors.DarkText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Dedicated Telegram Bot Callout
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0C161C))
                        .border(1.dp, AppColors.ElectricBlue.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isRu) "Уже пользовались через Telegram-бота?" else "Already used via Telegram bot?",
                        color = Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isRu) "Войдите в один клик через вашего бота" else "Log in with 1 tap via your bot",
                        color = AppColors.TextSecondary,
                        fontSize = 11.5.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
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
                            .clickable {
                                onSignInClick()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TelegramLogoIcon(size = 18.dp, color = AppColors.DarkText)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isRu) "Войти через Telegram" else "Log In with Telegram",
                                color = AppColors.DarkText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Switch to Sign In
                val footerText = buildAnnotatedString {
                    withStyle(SpanStyle(color = AppColors.TextSecondary, fontSize = 14.sp)) {
                        append(if (isRu) "Уже есть аккаунт? " else "Already have an account? ")
                    }
                    withStyle(SpanStyle(color = AppColors.BrandGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)) {
                        append(if (isRu) "Войти" else "Sign In")
                    }
                }

                Box(
                    modifier = Modifier
                        .clickable(onClick = onSignInClick)
                        .padding(vertical = 8.dp)
                ) {
                    Text(text = footerText)
                }

                Spacer(modifier = Modifier.height(16.dp))
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
        isFocused -> AppColors.BrandGreen
        else -> AppColors.CardBorder
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.InputBg)
            .border(1.2.dp, borderColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp),
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
                        fontSize = 14.sp
                    )
                }

                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = Color.White,
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
                    cursorBrush = SolidColor(AppColors.BrandGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused }
                )
            }

            if (trailingIcon != null) {
                trailingIcon()
            }
        }
    }
}
