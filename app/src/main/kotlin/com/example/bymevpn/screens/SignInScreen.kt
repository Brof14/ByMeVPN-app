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
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var authError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

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
            isLoading = true
            authError = null
            scope.launch {
                val result = AccountRepository.loginWithEmail(context, trimmed, password)
                isLoading = false
                if (result.isSuccess) {
                    onSignInSuccess()
                } else {
                    authError = result.exceptionOrNull()?.message ?: (if (isRu) "Ошибка авторизации" else "Sign in failed")
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
            val logoSize = (screenWidth * 0.38f).coerceIn(110.dp, 140.dp)
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
                    withStyle(SpanStyle(color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)) {
                        append("ByMe")
                    }
                    withStyle(SpanStyle(color = AppColors.BrandGreen, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)) {
                        append("VPN")
                    }
                }
                Text(text = brandText)

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isRu) "Скорость. Анонимность. Честность." else "Speed. Anonymity. Honesty.",
                    color = AppColors.TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Fast Telegram Bot Sign In (Recommended)
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
                            try {
                                val tgIntent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse("https://t.me/ByMeVPNBot?start=login")
                                )
                                context.startActivity(tgIntent)
                            } catch (_: Exception) {
                                android.widget.Toast.makeText(context, "Telegram не установлен", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TelegramLogoIcon(size = 20.dp, color = AppColors.DarkText)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isRu) "Войти через Telegram-бота" else "Log In with Telegram",
                            color = AppColors.DarkText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
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

                // Email Input Field
                SignInInputField(
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

                // Password Input Field
                SignInInputField(
                    value = password,
                    onValueChange = {
                        password = it
                        passwordError = null
                        authError = null
                    },
                    hintText = if (isRu) "Пароль" else "Password",
                    leadingIcon = { LockIcon(color = if (passwordError != null) AppColors.StatusError else AppColors.BrandGreen) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            EyeIcon(visible = passwordVisible, color = AppColors.TextSecondary)
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                    onImeAction = { submitLogin() },
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

                Spacer(modifier = Modifier.height(20.dp))

                // Sign In Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(AppColors.CardBgElevated)
                        .border(1.2.dp, AppColors.BrandGreen, RoundedCornerShape(14.dp))
                        .clickable(enabled = !isLoading) { submitLogin() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = AppColors.BrandGreen, modifier = Modifier.size(22.dp))
                    } else {
                        Text(
                            text = if (isRu) "Войти" else "Sign In",
                            color = AppColors.BrandGreen,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Google Sign-In with Credential Manager
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(AppColors.CardBg)
                        .border(1.dp, AppColors.CardBorder, RoundedCornerShape(14.dp))
                        .clickable(enabled = !isLoading) {
                            isLoading = true
                            authError = null
                            scope.launch {
                                val result = AccountRepository.loginWithGoogle(context)
                                isLoading = false
                                if (result.isSuccess) {
                                    onSignInSuccess()
                                } else {
                                    val rawErr = result.exceptionOrNull()?.message
                                    authError = rawErr
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GoogleLogoIcon(size = 20.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isRu) "Войти через Google" else "Continue with Google",
                            color = Color.White,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Switch to Sign Up
                val footerText = buildAnnotatedString {
                    withStyle(SpanStyle(color = AppColors.TextSecondary, fontSize = 14.sp)) {
                        append(if (isRu) "Нет аккаунта? " else "Don't have an account? ")
                    }
                    withStyle(SpanStyle(color = AppColors.BrandGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)) {
                        append(if (isRu) "Зарегистрироваться" else "Sign Up")
                    }
                }

                Box(
                    modifier = Modifier
                        .clickable(onClick = onSignUpClick)
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
private fun SignInInputField(
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
