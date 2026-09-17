package com.example.bymevpn.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import com.example.bymevpn.R
import com.example.bymevpn.components.GradientBackground
import com.example.bymevpn.components.GradientButton
import com.example.bymevpn.components.PageIndicator
import com.example.bymevpn.components.ShieldLogo
import com.example.bymevpn.theme.AppColors
import com.example.bymevpn.theme.AppGradients
import com.example.bymevpn.theme.AppTypography
import kotlinx.coroutines.launch

/**
 * Welcome screen for ByMeVPN ported faithfully to Jetpack Compose.
 * Preserves the exact visual hierarchy, branding, custom shield canvas, and interaction flows.
 */
@Composable
fun WelcomeScreen(
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    val signUpText = stringResource(R.string.sign_up)
    val logInText = stringResource(R.string.log_in)
    val snackSignUp = stringResource(R.string.snack_sign_up)
    val snackLogIn = stringResource(R.string.snack_log_in)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .padding(bottom = 24.dp)
                    .testTag("welcome_snackbar_host")
            ) { data ->
                Snackbar(
                    containerColor = AppColors.SnackBarBg,
                    contentColor = AppColors.White,
                    shape = RoundedCornerShape(12.dp),
                    snackbarData = data
                )
            }
        }
    ) { innerPadding ->
        GradientBackground(
            modifier = Modifier.padding(innerPadding)
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val screenWidth = maxWidth
                val screenHeight = maxHeight

                // Logo width: ~48% of screen width clamped between 180dp and 240dp
                val logoSize = (screenWidth * 0.48f).coerceIn(180.dp, 240.dp)
                val horizontalPadding = (screenWidth * 0.06f).coerceIn(16.dp, 32.dp)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 600.dp)
                        .padding(horizontal = horizontalPadding),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(screenHeight * 0.048f))

                    // Animated Shield Logo
                    AnimatedVisibility(
                        visible = visible,
                        enter = scaleIn(
                            initialScale = 0.7f,
                            animationSpec = tween(800, easing = FastOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(600))
                    ) {
                        ShieldLogo(size = logoSize)
                    }

                    Spacer(modifier = Modifier.height(screenHeight * 0.055f))

                    // Animated Brand Title: "ByMe" (White) + "VPN" (Emerald Green)
                    AnimatedVisibility(
                        visible = visible,
                        enter = slideInVertically(
                            initialOffsetY = { 40 },
                            animationSpec = tween(1000, delayMillis = 100)
                        ) + fadeIn(animationSpec = tween(1000, delayMillis = 100))
                    ) {
                        val brandText = buildAnnotatedString {
                            withStyle(AppTypography.BrandWhite.toSpanStyle()) {
                                append(stringResource(R.string.brand_part_1))
                            }
                            withStyle(AppTypography.BrandGreen.toSpanStyle()) {
                                append(stringResource(R.string.brand_part_2))
                            }
                        }
                        Text(
                            text = brandText,
                            modifier = Modifier.testTag("brand_title")
                        )
                    }

                    Spacer(modifier = Modifier.height(screenHeight * 0.02f))

                    // Animated Slogan: "Speed. Anonymity. Honesty."
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(animationSpec = tween(1100, delayMillis = 200))
                    ) {
                        Text(
                            text = stringResource(R.string.slogan),
                            style = AppTypography.Slogan,
                            modifier = Modifier.testTag("brand_slogan")
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Page Indicator Dots (Total: 3, Active: 1)
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(animationSpec = tween(1200, delayMillis = 300))
                    ) {
                        PageIndicator(total = 3, active = 1)
                    }

                    Spacer(modifier = Modifier.height(screenHeight * 0.025f))

                    // Action Buttons (Sign Up & Log In)
                    AnimatedVisibility(
                        visible = visible,
                        enter = slideInVertically(
                            initialOffsetY = { 60 },
                            animationSpec = tween(800, delayMillis = 400)
                        ) + fadeIn(animationSpec = tween(800, delayMillis = 400))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(screenWidth * 0.045f)
                        ) {
                            GradientButton(
                                label = signUpText,
                                gradient = AppGradients.SignUp,
                                onClick = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(snackSignUp)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                testTag = "sign_up_button"
                            )

                            GradientButton(
                                label = logInText,
                                gradient = AppGradients.LogIn,
                                darkLabel = true,
                                onClick = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(snackLogIn)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                testTag = "log_in_button"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(screenHeight * 0.055f))
                }
            }
        }
    }
}
