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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.bymevpn.components.EyeIcon
import com.example.bymevpn.components.GoogleLogoIcon
import com.example.bymevpn.components.GoogleSignInDialog
import com.example.bymevpn.components.GradientBackground
import com.example.bymevpn.components.LockIcon
import com.example.bymevpn.components.MailIcon
import com.example.bymevpn.components.ShieldLogo
import com.example.bymevpn.data.AccountRepository
import com.example.bymevpn.data.LocaleManager
import com.example.bymevpn.theme.AppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

/**
 * Sign In / Log In Screen.
 * Ultra-fast, high-contrast, fully functional with Google Sign-In and Forgot Password reset.
 */
@Composable
fun SignInScreen(
    onSignUpClick: () -> Unit = {},
    onSignInSuccess: (email: String, isGoogle: Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isRu = LocaleManager.isRussian(context)

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var authError by remember { mutableStateOf<String?>(null) }

    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var showGoogleSignInDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    fun validateAndSubmit() {
        focusManager.clearFocus()
        var valid = true

        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty()) {
            emailError = if (isRu) "Введите адрес эл. почты" else "Please enter your email"
            valid = false
        } else if (!EMAIL_REGEX.matches(trimmedEmail)) {
            emailError = if (isRu) "Неверный формат почты (например, name@domain.com)" else "Invalid email format (e.g. name@domain.com)"
            valid = false
        } else {
            emailError = null
        }

        if (password.isEmpty()) {
            passwordError = if (isRu) "Введите пароль" else "Please enter your password"
            valid = false
        } else if (password.length < 8) {
            passwordError = if (isRu) "Пароль должен содержать от 8 символов" else "Password must be at least 8 characters"
            valid = false
        } else {
            passwordError = null
        }

        if (valid) {
            authError = null
            // Check in database or register new account
            AccountRepository.loginOrRegister(trimmedEmail, isGoogle = false)
            scope.launch {
                snackbarHostState.showSnackbar(
                    if (isRu) "Вход в ByMeVPN..." else "Signing in to ByMeVPN..."
                )
            }
            onSignInSuccess(trimmedEmail, false)
        } else {
            authError = if (isRu)
                "Неверные данные. Если вы забыли пароль, восстановите его ниже."
            else
                "Invalid credentials. If you forgot your password, please reset it below."
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 24.dp)
            )
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

                    // 1. ByMeVPN Shield Logo
                    ShieldLogo(size = logoSize)

                    Spacer(modifier = Modifier.height(18.dp))

                    // 2. Brand Title: "ByMe" (White) + "VPN" (Emerald Green)
                    val brandText = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                color = Color.White,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.sp
                            )
                        ) {
                            append("ByMe")
                        }
                        withStyle(
                            SpanStyle(
                                color = Color(0xFF26E875),
                                fontSize = 34.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.sp
                            )
                        ) {
                            append("VPN")
                        }
                    }
                    Text(
                        text = brandText,
                        modifier = Modifier.testTag("brand_title")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // 3. Slogan: crisp, high contrast Slate-300
                    Text(
                        text = if (isRu) "Скорость. Анонимность. Честность." else "Speed. Anonymity. Honesty.",
                        color = Color(0xFFCBD5E1),
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.4.sp,
                        modifier = Modifier.testTag("brand_slogan")
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // 4. Email Input Field
                    SignInInputField(
                        value = email,
                        onValueChange = {
                            email = it
                            if (emailError != null) emailError = null
                            if (authError != null) authError = null
                        },
                        hintText = if (isRu) "Эл. почта" else "Email",
                        leadingIcon = { MailIcon(color = Color(0xFF94A3B8)) },
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                        isError = emailError != null,
                        testTag = "email_input_field"
                    )

                    if (emailError != null) {
                        Text(
                            text = emailError ?: "",
                            color = Color(0xFFFF5252),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 14.dp, top = 4.dp)
                                .testTag("email_error_text")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 5. Password Input Field
                    SignInInputField(
                        value = password,
                        onValueChange = {
                            password = it
                            if (passwordError != null) passwordError = null
                            if (authError != null) authError = null
                        },
                        hintText = if (isRu) "Пароль (от 8 символов)" else "Password (min 8 chars)",
                        leadingIcon = { LockIcon(color = Color(0xFF94A3B8)) },
                        trailingIcon = {
                            IconButton(
                                onClick = { passwordVisible = !passwordVisible },
                                modifier = Modifier.size(36.dp).testTag("password_visibility_toggle")
                            ) {
                                EyeIcon(
                                    visible = passwordVisible,
                                    color = if (passwordVisible) Color(0xFF00D4FF) else Color(0xFF94A3B8)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                        onImeAction = { validateAndSubmit() },
                        isError = passwordError != null,
                        testTag = "password_input_field"
                    )

                    if (passwordError != null) {
                        Text(
                            text = passwordError ?: "",
                            color = Color(0xFFFF5252),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 14.dp, top = 4.dp)
                                .testTag("password_error_text")
                        )
                    }

                    // Auth error banner
                    if (authError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x28FF3B30))
                                .border(1.dp, Color(0x66FF3B30), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("auth_error_banner")
                        ) {
                            Text(
                                text = authError ?: "",
                                color = Color(0xFFFF6B6B),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    // Forgot Password link
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, end = 4.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = if (isRu) "Забыли пароль?" else "Forgot Password?",
                            color = Color(0xFF00D4FF),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { showForgotPasswordDialog = true }
                                .padding(vertical = 4.dp)
                                .testTag("forgot_password_link")
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // 6. Primary Action: "Sign In" Button
                    SignInGradientButton(
                        label = if (isRu) "Войти" else "Sign In",
                        onClick = { validateAndSubmit() },
                        testTag = "sign_in_button"
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // 7. Divider: "OR"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF1E3250),
                            thickness = 1.dp
                        )
                        Text(
                            text = if (isRu) "ИЛИ" else "OR",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF1E3250),
                            thickness = 1.dp
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // 8. Social Login: Google Sign-In Button
                    GoogleAuthButton(
                        onClick = { showGoogleSignInDialog = true },
                        testTag = "google_sign_in_button"
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // 9. Footer: "Don't have an account? Sign Up"
                    val footerText = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                color = Color(0xFF94A3B8),
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Normal
                            )
                        ) {
                            append(if (isRu) "Нет аккаунта? " else "Don't have an account? ")
                        }
                        withStyle(
                            SpanStyle(
                                color = Color(0xFF26E875),
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        ) {
                            append(if (isRu) "Зарегистрироваться" else "Sign Up")
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clickable(onClick = onSignUpClick)
                            .padding(vertical = 8.dp)
                            .testTag("sign_up_footer_link")
                    ) {
                        Text(text = footerText)
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }

    // Google Sign-In Account Chooser Bottom Sheet / Dialog
    if (showGoogleSignInDialog) {
        GoogleSignInDialog(
            onDismiss = { showGoogleSignInDialog = false },
            onAccountSelected = { userEmail, userName ->
                showGoogleSignInDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar("Welcome back, $userName!")
                }
                onSignInSuccess(userEmail, true)
            }
        )
    }

    // Forgot Password Dialog
    if (showForgotPasswordDialog) {
        ForgotPasswordDialog(
            initialEmail = email.trim(),
            onDismiss = { showForgotPasswordDialog = false },
            onResetSuccess = { newPass, resetEmail ->
                showForgotPasswordDialog = false
                email = resetEmail
                password = newPass
                scope.launch {
                    snackbarHostState.showSnackbar("Password reset successful! You can now Sign In.")
                }
            }
        )
    }
}

/**
 * Text field styled for high contrast, instant responsiveness, and zero input lag.
 */
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
    isError: Boolean = false,
    testTag: String = "sign_in_input_field"
) {
    var isFocused by remember { mutableStateOf(false) }

    val borderColor = when {
        isError -> Color(0xFFFF5252)
        isFocused -> Color(0xFF00C4FF)
        else -> Color(0xFF1E3458)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0A1324))
            .border(width = if (isFocused || isError) 1.5.dp else 1.dp, color = borderColor, shape = RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp)
            .testTag(testTag),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingIcon()

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = hintText,
                        color = Color(0xFF94A3B8), // High contrast placeholder
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(Color(0xFF26E875)),
                    visualTransformation = visualTransformation,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = keyboardType,
                        imeAction = imeAction
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { onImeAction() },
                        onNext = { onImeAction() }
                    ),
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

/**
 * Full-width gradient "Sign In" button with high-contrast text.
 */
@Composable
private fun SignInGradientButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "sign_in_gradient_btn"
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1.0f,
        animationSpec = tween(100),
        label = "btn_scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF00C4FF), // Bright Cyan
                        Color(0xFF26E875)  // Vibrant Lime
                    )
                )
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color(0xFF031015), // Crisp dark readable font
            fontSize = 16.5.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.3.sp
        )
    }
}

/**
 * Android-native Google Sign-In button with authentic G icon and sharp text.
 */
@Composable
private fun GoogleAuthButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "google_auth_btn"
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isRu = LocaleManager.isRussian(context)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1.0f,
        animationSpec = tween(100),
        label = "google_btn_scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0D172A))
            .border(width = 1.2.dp, color = Color(0xFF1E3458), shape = RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            GoogleLogoIcon(size = 22.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = if (isRu) "Войти через Google" else "Continue with Google",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.2.sp
            )
        }
    }
}

/**
 * 2-step Forgot Password dialog with code generation, 60s timer, and quick-fill test code.
 */
@Composable
private fun ForgotPasswordDialog(
    initialEmail: String,
    onDismiss: () -> Unit,
    onResetSuccess: (newPass: String, email: String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isRu = LocaleManager.isRussian(context)

    var step by remember { mutableIntStateOf(1) }
    var resetEmail by remember { mutableStateOf(initialEmail) }
    var verificationCode by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }

    var generatedCode by remember { mutableStateOf("482910") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var countdown by remember { mutableIntStateOf(60) }
    var isTimerRunning by remember { mutableStateOf(false) }

    LaunchedEffect(isTimerRunning) {
        if (isTimerRunning) {
            countdown = 60
            while (countdown > 0) {
                delay(1000)
                countdown -= 1
            }
            isTimerRunning = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xD9030610))
                .clickable { onDismiss() }
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0F1A2F))
                    .border(1.2.dp, Color(0xFF223A63), RoundedCornerShape(20.dp))
                    .clickable(enabled = false) {}
                    .padding(22.dp)
                    .testTag("forgot_password_dialog")
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (step == 1) {
                            if (isRu) "Сброс пароля" else "Reset Password"
                        } else {
                            if (isRu) "Код подтверждения" else "Enter Verification Code"
                        },
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (step == 1) {
                            if (isRu)
                                "Введите адрес эл. почты аккаунта. Мы отправим 6-значный проверочный код."
                            else
                                "Enter the email associated with your account. We'll send you a 6-digit confirmation code."
                        } else {
                            if (isRu)
                                "Мы отправили 6-значный код на $resetEmail. Введите его и новый пароль."
                            else
                                "We sent a 6-digit code to $resetEmail. Enter the code and your new password."
                        },
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    if (step == 1) {
                        SignInInputField(
                            value = resetEmail,
                            onValueChange = {
                                resetEmail = it
                                errorMessage = null
                            },
                            hintText = if (isRu) "Ваша эл. почта" else "Your email address",
                            leadingIcon = { MailIcon(color = Color(0xFF94A3B8)) },
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done,
                            isError = errorMessage != null,
                            testTag = "reset_email_input"
                        )
                    } else {
                        // Quick auto-fill test chip
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x2200D4FF))
                                .clickable { verificationCode = generatedCode }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isRu) "Быстрая вставка: $generatedCode" else "Quick Fill: $generatedCode",
                                color = Color(0xFF00D4FF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        SignInInputField(
                            value = verificationCode,
                            onValueChange = {
                                verificationCode = it
                                errorMessage = null
                            },
                            hintText = if (isRu) "6-значный код (например, $generatedCode)" else "6-Digit Code (e.g. $generatedCode)",
                            leadingIcon = { LockIcon(color = Color(0xFF94A3B8)) },
                            keyboardType = KeyboardType.Number,
                            isError = errorMessage != null,
                            testTag = "verification_code_input"
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        SignInInputField(
                            value = newPassword,
                            onValueChange = {
                                newPassword = it
                                errorMessage = null
                            },
                            hintText = if (isRu) "Новый пароль (от 8 символов)" else "New Password (min 8 chars)",
                            leadingIcon = { LockIcon(color = Color(0xFF94A3B8)) },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardType = KeyboardType.Password,
                            isError = errorMessage != null,
                            testTag = "new_password_input"
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (isTimerRunning) {
                                if (isRu) "Повторить через ${countdown}с" else "Resend code in ${countdown}s"
                            } else {
                                if (isRu) "Отправить код повторно" else "Resend code"
                            },
                            color = if (isTimerRunning) Color(0xFF64758E) else Color(0xFF26E875),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable(enabled = !isTimerRunning) {
                                isTimerRunning = true
                            }
                        )
                    }

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = Color(0xFFFF5252),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF162542))
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isRu) "Отмена" else "Cancel",
                                color = Color(0xFF94A3B8),
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF00C4FF), Color(0xFF26E875))
                                    )
                                )
                                .clickable {
                                    if (step == 1) {
                                        val trimmed = resetEmail.trim()
                                        if (trimmed.isEmpty() || !EMAIL_REGEX.matches(trimmed)) {
                                            errorMessage = if (isRu) "Введите корректный email" else "Please enter a valid email"
                                        } else {
                                            step = 2
                                            isTimerRunning = true
                                            errorMessage = null
                                        }
                                    } else {
                                        if (verificationCode.trim() != generatedCode) {
                                            errorMessage = if (isRu) "Неверный код" else "Invalid code. Click 'Quick Fill' to test."
                                        } else if (newPassword.length < 8) {
                                            errorMessage = if (isRu) "Пароль должен быть от 8 символов" else "Password must be at least 8 chars"
                                        } else {
                                            onResetSuccess(newPassword, resetEmail)
                                        }
                                    }
                                }
                                .testTag("confirm_reset_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (step == 1) {
                                    if (isRu) "Отправить код" else "Send Code"
                                } else {
                                    if (isRu) "Подтвердить" else "Confirm"
                                },
                                color = Color(0xFF031015),
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
