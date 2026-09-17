package com.example.bymevpn.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bymevpn.components.AppleLogoIcon
import com.example.bymevpn.components.EyeIcon
import com.example.bymevpn.components.GoogleLogoIcon
import com.example.bymevpn.components.GoogleSignInDialog
import com.example.bymevpn.components.GradientBackground
import com.example.bymevpn.components.LockIcon
import com.example.bymevpn.components.MailIcon
import com.example.bymevpn.components.ShieldLogo
import kotlinx.coroutines.launch

private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
private val PHONE_REGEX = Regex("^\\+?[0-9\\s\\-()]{7,16}$")

/**
 * Sign Up Screen.
 * Ultra-crisp typography, zero input lag, full validation, and working Google registration.
 */
@Composable
fun SignUpScreen(
    onSignInClick: () -> Unit = {},
    onSignUpSuccess: (email: String, isGoogle: Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var emailOrPhone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var accountError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var showGoogleDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    fun validateAndSubmit() {
        focusManager.clearFocus()
        var valid = true

        val trimmedAccount = emailOrPhone.trim()
        if (trimmedAccount.isEmpty()) {
            accountError = "Please enter your email or phone"
            valid = false
        } else if (!EMAIL_REGEX.matches(trimmedAccount) && !PHONE_REGEX.matches(trimmedAccount)) {
            accountError = "Invalid email format or phone number"
            valid = false
        } else {
            accountError = null
        }

        if (password.isEmpty()) {
            passwordError = "Please enter your password"
            valid = false
        } else if (password.length < 8) {
            passwordError = "Password must be at least 8 characters"
            valid = false
        } else {
            passwordError = null
        }

        if (valid) {
            scope.launch {
                snackbarHostState.showSnackbar("Account created! Welcome to ByMeVPN.")
            }
            onSignUpSuccess(trimmedAccount, false)
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

                    // 3. Slogan: crisp, high-contrast Slate 300
                    Text(
                        text = "Speed. Anonymity. Honesty.",
                        color = Color(0xFFCBD5E1),
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.4.sp,
                        modifier = Modifier.testTag("brand_slogan")
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // 4. Input: Email or Phone
                    SignUpInputField(
                        value = emailOrPhone,
                        onValueChange = {
                            emailOrPhone = it
                            if (accountError != null) accountError = null
                        },
                        hintText = "Email or Phone number",
                        leadingIcon = { MailIcon(color = Color(0xFF94A3B8)) },
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                        isError = accountError != null,
                        testTag = "email_or_phone_input"
                    )

                    if (accountError != null) {
                        Text(
                            text = accountError ?: "",
                            color = Color(0xFFFF5252),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 14.dp, top = 4.dp)
                                .testTag("account_error_text")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 5. Input: Password
                    SignUpInputField(
                        value = password,
                        onValueChange = {
                            password = it
                            if (passwordError != null) passwordError = null
                        },
                        hintText = "Password (min 8 chars)",
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

                    Spacer(modifier = Modifier.height(24.dp))

                    // 6. Primary Action: "Create Account"
                    SignUpGradientButton(
                        label = "Create Account",
                        onClick = { validateAndSubmit() },
                        testTag = "create_account_button"
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
                            text = "OR",
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

                    // 8. Social Login: Google and Apple side-by-side
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Google registration button
                        SocialSquareButton(
                            onClick = { showGoogleDialog = true },
                            modifier = Modifier.weight(1f),
                            testTag = "google_signup_button"
                        ) {
                            GoogleLogoIcon(size = 24.dp)
                        }

                        // Apple registration button
                        SocialSquareButton(
                            onClick = {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Apple ID is available on iOS devices. Try Google Sign-In!")
                                }
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "apple_signup_button"
                        ) {
                            AppleLogoIcon(size = 24.dp)
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // 9. Footer: "Already have an account? Sign in"
                    val footerText = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                color = Color(0xFF94A3B8),
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Normal
                            )
                        ) {
                            append("Already have an account? ")
                        }
                        withStyle(
                            SpanStyle(
                                color = Color(0xFF26E875),
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        ) {
                            append("Sign in")
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clickable(onClick = onSignInClick)
                            .padding(vertical = 8.dp)
                            .testTag("sign_in_link")
                    ) {
                        Text(text = footerText)
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }

    if (showGoogleDialog) {
        GoogleSignInDialog(
            onDismiss = { showGoogleDialog = false },
            onAccountSelected = { userEmail, userName ->
                showGoogleDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar("Welcome to ByMeVPN, $userName!")
                }
                onSignUpSuccess(userEmail, true)
            }
        )
    }
}

/**
 * Text field styled for high contrast, instant responsiveness, and zero input lag.
 */
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
    isError: Boolean = false,
    testTag: String = "signup_input_field"
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
 * Full-width gradient "Create Account" button with high-contrast text.
 */
@Composable
private fun SignUpGradientButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "signup_gradient_btn"
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
            color = Color(0xFF031015), // High contrast dark font
            fontSize = 16.5.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.3.sp
        )
    }
}

/**
 * Social square button (Google / Apple) with dark container.
 */
@Composable
private fun SocialSquareButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "social_square_btn",
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1.0f,
        animationSpec = tween(100),
        label = "social_scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .height(54.dp)
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
        content()
    }
}
