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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
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
import com.example.bymevpn.data.LocaleManager
import com.example.bymevpn.theme.AppColors
import kotlinx.coroutines.launch

private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

/**
 * Main Sign-In Screen styled to match the user's reference design to the pixel:
 * 1. Deep space backdrop with Earth curvature, atmosphere halo and global network arcs
 * 2. ByMeVPN shield logo with neon infinity loop and ambient backglow
 * 3. Prominent Google Sign-In button with white pill card and right chevron
 * 4. Distinct "или по почте" horizontal divider
 * 5. High-contrast dark inputs for email and password
 * 6. Vibrant neon green "Войти" button with right chevron
 * 7. Dedicated Telegram Bot card with badge and green "Войти через Telegram" button
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
    var telegramStatusText by remember { mutableStateOf<String?>(null) }

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
                        size = 108.dp,
                        isActive = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. Title: ByMe (White) + VPN (Green)
                    val brandText = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                color = Color.White,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        ) {
                            append("ByMe")
                        }
                        withStyle(
                            SpanStyle(
                                color = Color(0xFF00E676),
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
                        text = if (isRu) "Скорость. Свобода. Простота." else "Speed. Freedom. Simplicity.",
                        color = Color(0xFF7A8B9E),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // 4. Primary 1-Tap Google Sign-In Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .drawBehind {
                                // Subtle soft white/cyan bottom glow shadow
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(Color(0x2500E5FF), Color.Transparent),
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
                                        val rawErr = result.exceptionOrNull()?.message
                                        if (rawErr != "Sign-in cancelled") {
                                            authError = if (isRu) "Не удалось подтвердить вход через Google. Попробуйте позже." else "Could not verify Google sign-in. Please try again later."
                                        }
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
                                .background(Color(0xFF1A2634))
                        )
                        Text(
                            text = if (isRu) "или по почте" else "or via email",
                            color = Color(0xFF5A6E82),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(Color(0xFF1A2634))
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // 6. Email Input Field
                    ExactInputField(
                        value = email,
                        onValueChange = {
                            email = it
                            emailError = null
                            authError = null
                        },
                        hintText = if (isRu) "Адрес эл. почты" else "Email address",
                        leadingIcon = { MailIcon(color = Color(0xFF758A9E), size = 20.dp) },
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                        isError = emailError != null,
                        testTag = "email_input"
                    )

                    if (emailError != null) {
                        Text(
                            text = emailError ?: "",
                            color = Color(0xFFFF5252),
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
                        leadingIcon = { LockIcon(color = Color(0xFF758A9E), size = 20.dp) },
                        trailingIcon = {
                            IconButton(
                                onClick = { passwordVisible = !passwordVisible },
                                modifier = Modifier.size(36.dp)
                            ) {
                                EyeIcon(visible = passwordVisible, color = Color(0xFF758A9E), size = 20.dp)
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
                            color = Color(0xFFFF5252),
                            fontSize = 12.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 6.dp, top = 4.dp)
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

                    Spacer(modifier = Modifier.height(18.dp))

                    // 8. Main "Войти" Button (Green Gradient with Chevron)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .drawBehind {
                                // Vibrant neon green glow shadow underneath
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(Color(0x6000E676), Color.Transparent),
                                        center = Offset(size.width / 2f, size.height + 4f),
                                        radius = size.width * 0.45f
                                    ),
                                    radius = size.width * 0.45f,
                                    center = Offset(size.width / 2f, size.height + 4f)
                                )
                            }
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF00F59B),
                                        Color(0xFF00E575),
                                        Color(0xFF1DE9B6)
                                    )
                                )
                            )
                            .clickable(enabled = !isEmailLoading && !isGoogleLoading) { submitLogin() }
                            .testTag("login_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isEmailLoading) {
                            CircularProgressIndicator(
                                color = Color(0xFF002213),
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
                                    color = Color(0xFF002213),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                ChevronRightIcon(color = Color(0xFF002213), size = 18.dp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 9. Dedicated Telegram-Bot Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawBehind {
                                // Subtle radiant blue glow around card
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(Color(0x220088FF), Color.Transparent),
                                        center = Offset(size.width / 2f, size.height / 2f),
                                        radius = size.width * 0.52f
                                    ),
                                    radius = size.width * 0.52f,
                                    center = Offset(size.width / 2f, size.height / 2f)
                                )
                            }
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color(0xFF07121C))
                            .border(1.2.dp, Color(0xFF16324B), RoundedCornerShape(22.dp))
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
                                                listOf(Color(0xFF1E88E5), Color(0xFF1565C0))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    PaperPlaneIcon(color = Color.White, size = 22.dp)
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isRu) "Уже пользовались через\nTelegram-бота?" else "Already used via\nTelegram bot?",
                                        color = Color.White,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 19.sp
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = if (isRu) "Войдите в один клик через вашего бота" else "Sign in with 1 tap via your bot",
                                        color = Color(0xFF758A9E),
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
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color(0xFF00F59B),
                                                Color(0xFF00E575)
                                            )
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
                                                telegramStatusText = if (isRu) "Ожидание подтверждения в боте..." else "Waiting for confirmation..."

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
                                                    onSignInSuccess()
                                                } else {
                                                    authError = if (isRu) "Время ожидания истекло. Попробуйте снова." else "Confirmation timed out. Please try again."
                                                }
                                            } else {
                                                isTelegramLoading = false
                                                telegramStatusText = null
                                                openTelegram(context, "start=login")
                                            }
                                        }
                                    }
                                    .testTag("telegram_login_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isTelegramLoading) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(color = Color(0xFF002213), modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = telegramStatusText ?: (if (isRu) "Вход..." else "Logging in..."),
                                            color = Color(0xFF002213),
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
                                        PaperPlaneIcon(color = Color(0xFF002213), size = 18.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isRu) "Войти через Telegram" else "Log In with Telegram",
                                            color = Color(0xFF002213),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        ChevronRightIcon(color = Color(0xFF002213), size = 18.dp)
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
                            color = Color(0xFF758A9E),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Normal
                        )
                        Text(
                            text = if (isRu) "Зарегистрироваться" else "Sign Up",
                            color = Color(0xFF00F59B),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        ChevronRightIcon(color = Color(0xFF00F59B), size = 14.dp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
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
        isError -> Color(0xFFFF5252)
        isFocused -> Color(0xFF00E676)
        else -> Color(0xFF182836)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0C1620))
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
                        color = Color(0xFF5A6E82),
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Normal
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
                    cursorBrush = SolidColor(Color(0xFF00E676)),
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

private fun openTelegram(context: Context, param: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/ByMeVPNBot?$param"))
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "Telegram не установлен", Toast.LENGTH_SHORT).show()
    }
}
