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
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.bymevpn.data.AccountRepository
import com.example.bymevpn.data.subscription.SubscriptionManager
import com.example.bymevpn.screens.AccountScreen
import com.example.bymevpn.screens.AdvancedSettingsScreen
import com.example.bymevpn.screens.DevicesScreen
import com.example.bymevpn.screens.HomeScreen
import com.example.bymevpn.screens.PricingScreen
import com.example.bymevpn.screens.RenewalScreen
import com.example.bymevpn.screens.SettingsScreen
import com.example.bymevpn.screens.SignInScreen
import com.example.bymevpn.screens.SignUpScreen
import com.example.bymevpn.screens.SupportScreen
import com.example.bymevpn.screens.TrialScreen
import com.example.bymevpn.theme.ByMeVPNTheme
import com.example.bymevpn.vpn.VpnManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

enum class AppScreen {
    SignIn,
    SignUp,
    Home,
    Settings,
    AdvancedSettings,
    Account,
    Devices,
    Renewal,
    Support,
    Trial,
    Pricing
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
        if (data.scheme == "bymevpn" && data.host == "payment") {
            val planCode = data.getQueryParameter("plan") ?: "1m"
            val days = data.getQueryParameter("days")?.toIntOrNull() ?: 30
            CoroutineScope(Dispatchers.Main).launch {
                val result = com.example.bymevpn.data.payment.YooKassaPaymentHandler.confirmPayment(applicationContext, planCode, days)
                val message = if (result.isSuccess) "Оплата подтверждена, подписка активирована" else "Оплата ещё не подтверждена сервером"
                Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
            }
            return
        }

        val subUrl = data.getQueryParameter("url")
            ?: data.getQueryParameter("sub")
            ?: if (data.scheme == "http" || data.scheme == "https") data.toString() else null
        val userName = data.getQueryParameter("name") ?: "Telegram User"

        if (!subUrl.isNullOrBlank()) {
            CoroutineScope(Dispatchers.Main).launch {
                val result = SubscriptionManager.getInstance(applicationContext).applySubscriptionUrl(subUrl)
                if (result.isSuccess) {
                    if (com.example.bymevpn.data.storage.SecureTokenStorage.getInstance(applicationContext).getAccessToken().isNullOrBlank() && AccountRepository.currentUser.value == null) {
                        AccountRepository.createLocalProfile(applicationContext, userName)
                    }
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
        CoroutineScope(Dispatchers.Main).launch {
            com.example.bymevpn.data.payment.YooKassaPaymentHandler.checkAndCompletePendingPayment(applicationContext)
        }
    }
}

@Composable
fun MainAppNav() {
    val context = LocalContext.current
    val currentUser by AccountRepository.currentUser.collectAsState()
    LaunchedEffect(Unit) {
        com.example.bymevpn.data.settings.SettingsRepository.getInstance(context).settingsFlow.collect { settings ->
            com.example.bymevpn.data.LocaleManager.setLanguage(com.example.bymevpn.data.AppLanguage.fromCode(settings.language))
        }
    }

    var currentScreen by remember {
        mutableStateOf(if (currentUser != null) AppScreen.Home else AppScreen.SignIn)
    }

    LaunchedEffect(currentUser) {
        val user = currentUser
        if (user != null && (currentScreen == AppScreen.SignIn || currentScreen == AppScreen.SignUp)) {
            currentScreen = AppScreen.Home
        } else if (user == null && currentScreen != AppScreen.SignIn && currentScreen != AppScreen.SignUp) {
            currentScreen = AppScreen.SignIn
        }
    }

    // Predictive back handling
    BackHandler(enabled = currentScreen != AppScreen.SignIn && currentScreen != AppScreen.Home) {
        currentScreen = when (currentScreen) {
            AppScreen.AdvancedSettings -> AppScreen.Settings
            AppScreen.Devices, AppScreen.Renewal -> AppScreen.Account
            AppScreen.Support -> AppScreen.Account
            AppScreen.Trial, AppScreen.Pricing -> AppScreen.Home
            AppScreen.Settings, AppScreen.Account -> AppScreen.Home
            AppScreen.SignUp -> AppScreen.SignIn
            else -> AppScreen.Home
        }
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "screen_transition"
    ) { screen ->
        when (screen) {
            AppScreen.SignIn -> {
                SignInScreen(
                    onSignUpClick = { currentScreen = AppScreen.SignUp },
                    onSignInSuccess = { currentScreen = AppScreen.Home }
                )
            }
            AppScreen.SignUp -> {
                SignUpScreen(
                    onSignInClick = { currentScreen = AppScreen.SignIn },
                    onSignUpSuccess = { currentScreen = AppScreen.Home }
                )
            }
            AppScreen.Home -> {
                HomeScreen(
                    onNavigateToAccount = { currentScreen = AppScreen.Account },
                    onNavigateToSettings = { currentScreen = AppScreen.Settings },
                    onNavigateToTrial = { currentScreen = AppScreen.Trial },
                    onNavigateToPricing = { currentScreen = AppScreen.Pricing },
                    onNavigateToSupport = { currentScreen = AppScreen.Support },
                    onLogOut = {
                        VpnManager.stopVpn(context)
                        AccountRepository.logout(context)
                        currentScreen = AppScreen.SignIn
                    }
                )
            }
            AppScreen.Settings -> {
                SettingsScreen(
                    onBackClick = { currentScreen = AppScreen.Home },
                    onNavigateToHome = { currentScreen = AppScreen.Home },
                    onNavigateToAccount = { currentScreen = AppScreen.Account },
                    onNavigateToAdvanced = { currentScreen = AppScreen.AdvancedSettings }
                )
            }
            AppScreen.AdvancedSettings -> {
                AdvancedSettingsScreen(
                    onBackClick = { currentScreen = AppScreen.Settings },
                    onNavigateToHome = { currentScreen = AppScreen.Home },
                    onNavigateToSettings = { currentScreen = AppScreen.Settings },
                    onNavigateToAccount = { currentScreen = AppScreen.Account }
                )
            }
            AppScreen.Account -> {
                AccountScreen(
                    onBackClick = { currentScreen = AppScreen.Home },
                    onNavigateToHome = { currentScreen = AppScreen.Home },
                    onNavigateToSettings = { currentScreen = AppScreen.Settings },
                    onNavigateToDevices = { currentScreen = AppScreen.Devices },
                    onNavigateToRenewal = { currentScreen = AppScreen.Renewal },
                    onNavigateToSupport = { currentScreen = AppScreen.Support },
                    onNavigateToTrial = { currentScreen = AppScreen.Trial },
                    onNavigateToPricing = { currentScreen = AppScreen.Pricing },
                    onLogOut = {
                        VpnManager.stopVpn(context)
                        AccountRepository.logout(context)
                        currentScreen = AppScreen.SignIn
                    }
                )
            }
            AppScreen.Devices -> {
                DevicesScreen(
                    onBackClick = { currentScreen = AppScreen.Account },
                    onNavigateToHome = { currentScreen = AppScreen.Home },
                    onNavigateToSettings = { currentScreen = AppScreen.Settings },
                    onNavigateToAccount = { currentScreen = AppScreen.Account }
                )
            }
            AppScreen.Renewal -> {
                RenewalScreen(
                    onBackClick = { currentScreen = AppScreen.Account },
                    onNavigateToHome = { currentScreen = AppScreen.Home },
                    onNavigateToSettings = { currentScreen = AppScreen.Settings },
                    onNavigateToAccount = { currentScreen = AppScreen.Account }
                )
            }
            AppScreen.Support -> {
                SupportScreen(
                    onBackClick = { currentScreen = AppScreen.Account },
                    onNavigateToHome = { currentScreen = AppScreen.Home },
                    onNavigateToSettings = { currentScreen = AppScreen.Settings },
                    onNavigateToAccount = { currentScreen = AppScreen.Account }
                )
            }
            AppScreen.Trial -> {
                TrialScreen(
                    onBackClick = { currentScreen = AppScreen.Home },
                    onChoosePlanClick = { currentScreen = AppScreen.Pricing },
                    onTrialActivated = { currentScreen = AppScreen.Home }
                )
            }
            AppScreen.Pricing -> {
                PricingScreen(
                    onBackClick = { currentScreen = AppScreen.Home }
                )
            }
        }
    }
}
