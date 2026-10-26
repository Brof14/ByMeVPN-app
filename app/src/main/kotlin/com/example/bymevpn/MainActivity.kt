package com.example.bymevpn

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.bymevpn.data.AccountRepository
import com.example.bymevpn.data.subscription.SubscriptionManager
import com.example.bymevpn.screens.AccountScreen
import com.example.bymevpn.screens.HomeScreen
import com.example.bymevpn.screens.SettingsScreen
import com.example.bymevpn.screens.SignInScreen
import com.example.bymevpn.screens.SignUpScreen
import com.example.bymevpn.screens.WelcomeScreen
import com.example.bymevpn.theme.ByMeVPNTheme
import com.example.bymevpn.vpn.VpnManager
import kotlinx.coroutines.launch

enum class AppScreen {
    Welcome,
    SignUp,
    SignIn,
    Home,
    Settings,
    Account
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AccountRepository.init(applicationContext)
        handleIntent(intent)
        enableEdgeToEdge()
        setContent {
            ByMeVPNTheme {
                MainAppNav()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val data = intent?.data ?: return
        // Handles: bymevpn://sub?url=... OR https://bymevpn.app/sub?...
        val subUrl = data.getQueryParameter("url") ?: if (data.scheme == "http" || data.scheme == "https") data.toString() else null
        if (!subUrl.isNullOrBlank()) {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                val result = SubscriptionManager.getInstance(applicationContext).applySubscriptionUrl(subUrl)
                if (result.isSuccess) {
                    Toast.makeText(applicationContext, "Подписка успешно подключена!", Toast.LENGTH_LONG).show()
                    VpnManager.getInstance(applicationContext).loadServers()
                } else {
                    Toast.makeText(applicationContext, "Ошибка добавления подписки: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        VpnManager.getInstance(applicationContext).syncWithServiceState()
    }
}

@Composable
fun MainAppNav() {
    val context = LocalContext.current
    val currentUser by AccountRepository.currentUser.collectAsState()

    var currentScreen by remember {
        mutableStateOf(if (currentUser != null) AppScreen.Home else AppScreen.Welcome)
    }

    LaunchedEffect(currentUser) {
        val user = currentUser
        if (user != null && (currentScreen == AppScreen.Welcome || currentScreen == AppScreen.SignIn || currentScreen == AppScreen.SignUp)) {
            currentScreen = AppScreen.Home
        } else if (user == null && currentScreen != AppScreen.Welcome && currentScreen != AppScreen.SignIn && currentScreen != AppScreen.SignUp) {
            currentScreen = AppScreen.Welcome
        }
    }

    BackHandler(enabled = currentScreen != AppScreen.Welcome && currentScreen != AppScreen.Home) {
        currentScreen = when (currentScreen) {
            AppScreen.Settings, AppScreen.Account -> AppScreen.Home
            else -> AppScreen.Welcome
        }
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
                    onSignUpClick = { currentScreen = AppScreen.SignUp },
                    onLogInClick = { currentScreen = AppScreen.SignIn }
                )
            }
            AppScreen.SignUp -> {
                SignUpScreen(
                    onSignInClick = { currentScreen = AppScreen.SignIn },
                    onSignUpSuccess = { currentScreen = AppScreen.Home }
                )
            }
            AppScreen.SignIn -> {
                SignInScreen(
                    onSignUpClick = { currentScreen = AppScreen.SignUp },
                    onSignInSuccess = { currentScreen = AppScreen.Home }
                )
            }
            AppScreen.Home -> {
                HomeScreen(
                    onNavigateToAccount = { currentScreen = AppScreen.Account },
                    onNavigateToSettings = { currentScreen = AppScreen.Settings },
                    onLogOut = {
                        VpnManager.stopVpn(context)
                        AccountRepository.logout(context)
                        currentScreen = AppScreen.Welcome
                    }
                )
            }
            AppScreen.Settings -> {
                SettingsScreen(
                    onBackClick = { currentScreen = AppScreen.Home }
                )
            }
            AppScreen.Account -> {
                AccountScreen(
                    onBackClick = { currentScreen = AppScreen.Home },
                    onLogOut = {
                        VpnManager.stopVpn(context)
                        AccountRepository.logout(context)
                        currentScreen = AppScreen.Welcome
                    }
                )
            }
        }
    }
}
