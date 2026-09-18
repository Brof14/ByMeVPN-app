package com.example.bymevpn.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.scale
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
import com.example.bymevpn.components.GradientBackground
import com.example.bymevpn.components.LockIcon
import com.example.bymevpn.components.MailIcon
import com.example.bymevpn.components.ShieldLogo
import com.example.bymevpn.data.AccountRepository
import com.example.bymevpn.data.LocaleManager
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
        containerColor = Color.Transparent,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState, modifier = Modifier.padding(bottom = 24.dp))
        }
    ) { innerPadding ->
        GradientBackground(modifier = Modifier.padding(innerPadding)) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val screenWidth = maxWidth
                val screenHeight = maxHeight
                val logoSize = (screenWidth * 0.42f).coerceIn(140.dp, 168.dp)
                val horizontalPadding = (screenWidth * 0.065f).coerceIn(20.dp, 26.dp)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 500.dp)
                        .verticalScroll(scrollState)
                        .padding(horizontal = horizontalPadding),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(screenHeight * 0.035f))

                    ShieldLogo(size = logoSize)

                    Spacer(modifier = Modifier.height(14.dp))

                    val brandText = buildAnnotatedString {
                        withStyle(SpanStyle(color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)) {
                            append("ByMe")
                        }
                        withStyle(SpanStyle(color = Color(0xFF26E875), fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)) {
                            append("VPN")
                        }
                    }
                    Text(text = brandText)

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (isRu) "Скорость. Анонимность. Честность." else "Speed. Anonymity. Honesty.",
                        color = Color(0xFFCBD5E1),
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // Email Input Field
                    SignInInputField(
                        value = email,
                        onValueChange = {
                            email = it
                            emailError = null
                            authError = null
                        },
                        hintText = if (isRu) "Адрес эл. почты" else "Email address",
                        leadingIcon = { MailIcon(color = if (emailError != null) Color(0xFFFF5252) else Color(0xFF00C4FF)) },
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                        isError = emailError != null
                    )

                    if (emailError != null) {
                        Text(
                            text = emailError ?: "",
                            color = Color(0xFFFF5252),
                            fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth().padding(start = 6.dp, top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Password Input Field
                    SignInInputField(
                        value = password,
                        onValueChange = {
                            password = it
                            passwordError = null
                            authError = null
                        },
                        hintText = if (isRu) "Пароль" else "Password",
                        leadingIcon = { LockIcon(color = if (passwordError != null) Color(0xFFFF5252) else Color(0xFF00C4FF)) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                EyeIcon(visible = passwordVisible, color = Color(0xFF94A3B8))
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
                            color = Color(0xFFFF5252),
                            fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth().padding(start = 6.dp, top = 4.dp)
                        )
                    }

                    if (authError != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = authError ?: "",
                            color = Color(0xFFFF5252),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Sign In Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.horizontalGradient(listOf(Color(0xFF00C4FF), Color(0xFF26E875)))
                            )
                            .clickable(enabled = !isLoading) { submitLogin() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color(0xFF031015), modifier = Modifier.size(24.dp))
                        } else {
                            Text(
                                text = if (isRu) "Войти" else "Sign In",
                                color = Color(0xFF031015),
                                fontSize = 16.5.sp,
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
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF1E3250), thickness = 1.dp)
                        Text(
                            text = if (isRu) "ИЛИ" else "OR",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF1E3250), thickness = 1.dp)
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Google Sign-In with Credential Manager
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF0D172A))
                            .border(1.2.dp, Color(0xFF1E3458), RoundedCornerShape(14.dp))
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
                                        authError = if (rawErr != null && rawErr.contains("Google Web Client ID")) {
                                            if (isRu) "Вход через Google требует Web Client ID в Google Cloud. Войдите по Email или используйте аккаунт администратора ниже."
                                            else "Google Sign-In requires Web Client ID in Google Cloud. Sign in with Email or use Admin account below."
                                        } else {
                                            rawErr
                                        }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            GoogleLogoIcon(size = 22.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (isRu) "Войти через Google" else "Continue with Google",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Footer
                    val footerText = buildAnnotatedString {
                        withStyle(SpanStyle(color = Color(0xFF94A3B8), fontSize = 14.5.sp)) {
                            append(if (isRu) "Нет аккаунта? " else "Don't have an account? ")
                        }
                        withStyle(SpanStyle(color = Color(0xFF26E875), fontSize = 14.5.sp, fontWeight = FontWeight.ExtraBold)) {
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

                    Spacer(modifier = Modifier.height(20.dp))
                }
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
        isError -> Color(0xFFFF5252)
        isFocused -> Color(0xFF00C4FF)
        else -> Color(0xFF1E355B)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0B1424))
            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingIcon()
            Spacer(modifier = Modifier.width(14.dp))

            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = hintText,
                        color = Color(0xFF64748B),
                        fontSize = 15.sp
                    )
                }

                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 15.sp,
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
                    cursorBrush = SolidColor(Color(0xFF26E875)),
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
