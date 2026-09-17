package com.example.bymevpn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.bymevpn.screens.HomeScreen
import com.example.bymevpn.screens.SignInScreen
import com.example.bymevpn.screens.SignUpScreen
import com.example.bymevpn.screens.WelcomeScreen
import com.example.bymevpn.theme.ByMeVPNTheme

enum class AppScreen {
    Welcome,
    SignUp,
    SignIn,
    Home
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ByMeVPNTheme {
                MainAppNav()
            }
        }
    }
}

@Composable
fun MainAppNav() {
    var currentScreen by remember { mutableStateOf(AppScreen.Welcome) }
    var currentUserEmail by remember { mutableStateOf("mama.nikfjdj@gmail.com") }
    var isGoogleAccount by remember { mutableStateOf(true) }

    BackHandler(enabled = currentScreen != AppScreen.Welcome) {
        currentScreen = AppScreen.Welcome
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        label = "screen_transition"
    ) { screen ->
        when (screen) {
            AppScreen.Welcome -> {
                WelcomeScreen(
                    onSignUpClick = {
                        currentScreen = AppScreen.SignUp
                    },
                    onLogInClick = {
                        currentScreen = AppScreen.SignIn
                    }
                )
            }
            AppScreen.SignUp -> {
                SignUpScreen(
                    onSignInClick = {
                        currentScreen = AppScreen.SignIn
                    },
                    onSignUpSuccess = { email, isGoogle ->
                        currentUserEmail = email
                        isGoogleAccount = isGoogle
                        currentScreen = AppScreen.Home
                    }
                )
            }
            AppScreen.SignIn -> {
                SignInScreen(
                    onSignUpClick = {
                        currentScreen = AppScreen.SignUp
                    },
                    onSignInSuccess = { email, isGoogle ->
                        currentUserEmail = email
                        isGoogleAccount = isGoogle
                        currentScreen = AppScreen.Home
                    }
                )
            }
            AppScreen.Home -> {
                HomeScreen(
                    userEmail = currentUserEmail,
                    isGoogleAuth = isGoogleAccount,
                    onLogOut = {
                        currentScreen = AppScreen.Welcome
                    }
                )
            }
        }
    }
}
