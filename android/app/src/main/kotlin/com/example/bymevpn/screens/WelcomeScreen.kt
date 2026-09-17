package com.example.bymevpn.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.bymevpn.theme.AppGradients
import com.example.bymevpn.theme.AppTypography

/**
 * Welcome screen matching the reference design 1:1.
 */
@Composable
fun WelcomeScreen(
    onSignUpClick: () -> Unit = {},
    onLogInClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val signUpText = stringResource(R.string.sign_up)
    val logInText = stringResource(R.string.log_in)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent
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

                val logoSize = (screenWidth * 0.53f).coerceIn(195.dp, 230.dp)
                val horizontalPadding = (screenWidth * 0.065f).coerceIn(22.dp, 28.dp)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 600.dp)
                        .padding(horizontal = horizontalPadding),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(screenHeight * 0.05f))

                    // Shield Logo with centered crossover ribbon
                    ShieldLogo(size = logoSize)

                    Spacer(modifier = Modifier.height(screenHeight * 0.04f))

                    // Brand Title: "ByMe" (White) + "VPN" (Emerald Green)
                    val brandText = buildAnnotatedString {
                        withStyle(AppTypography.BrandWhite.toSpanStyle()) {
                            append("ByMe")
                        }
                        withStyle(AppTypography.BrandGreen.toSpanStyle()) {
                            append("VPN")
                        }
                    }
                    Text(
                        text = brandText,
                        modifier = Modifier.testTag("brand_title")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Slogan: "Speed. Anonymity. Honesty."
                    Text(
                        text = "Speed. Anonymity. Honesty.",
                        style = AppTypography.Slogan,
                        modifier = Modifier.testTag("brand_slogan")
                    )

                    // Push action controls to the bottom
                    Spacer(modifier = Modifier.weight(1f))

                    // Action Buttons: Sign Up & Log In
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GradientButton(
                            label = signUpText,
                            gradient = AppGradients.SignUp,
                            shadowColor = Color(0x350058F6),
                            onClick = onSignUpClick,
                            modifier = Modifier.weight(1f),
                            testTag = "sign_up_button"
                        )

                        GradientButton(
                            label = logInText,
                            gradient = AppGradients.LogIn,
                            shadowColor = Color(0x3014DE6C),
                            onClick = onLogInClick,
                            modifier = Modifier.weight(1f),
                            testTag = "log_in_button"
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Four Page Indicator Dots (Active: Dot 2)
                    PageIndicator(total = 4, active = 1)

                    Spacer(modifier = Modifier.height(screenHeight * 0.025f))
                }
            }
        }
    }
}
