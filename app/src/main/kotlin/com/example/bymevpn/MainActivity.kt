package com.example.bymevpn

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.bymevpn.data.AccountRepository
import com.example.bymevpn.data.auth.isTelegramLoginHandoff
import com.example.bymevpn.data.auth.TelegramAuthPollPolicy
import com.example.bymevpn.data.auth.TelegramLinkOpener
import com.example.bymevpn.data.storage.SecureTokenStorage
import com.example.bymevpn.data.subscription.SubscriptionManager
import com.example.bymevpn.data.update.AppUpdateManager
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton

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
    private var telegramLoginJob: Job? = null
    private var lastAccountRefreshAt = 0L
    private var pendingExternalSubscription by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        AccountRepository.init(applicationContext)
        handleIntent(intent)
        enableEdgeToEdge()
        setContent {
            ByMeVPNTheme {
                Box(Modifier.fillMaxSize()) {
                    MainAppNav(onRestoreViaTelegram = {
                        startTelegramLoginFromConnection(allowAccountSwitch = true)
                    })
                    pendingExternalSubscription?.let { rawImport ->
                        ExternalSubscriptionImportDialog(
                            sourceHost = externalImportHost(rawImport),
                            onDismiss = { pendingExternalSubscription = null },
                            onConfirm = {
                                pendingExternalSubscription = null
                                importExternalSubscription(rawImport)
                            }
                        )
                    }
                }
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
        if (isTelegramLoginHandoff(data.toString())) {
            startTelegramLoginFromConnection()
            return
        }
        if (data.scheme == "bymevpn" && data.host == "payment") {
            val planCode = data.getQueryParameter("plan") ?: "1m"
            val days = data.getQueryParameter("days")?.toIntOrNull() ?: 30
            lifecycleScope.launch {
                val result = com.example.bymevpn.data.payment.YooKassaPaymentHandler.confirmPayment(applicationContext, planCode, days)
                val message = if (result.isSuccess) "Оплата подтверждена, подписка активирована" else "Оплата ещё не подтверждена сервером"
                Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
            }
            return
        }

        if (data.scheme.equals("bymevpn", ignoreCase = true) && data.host.equals("sub", ignoreCase = true)) {
            val subUrl = data.getQueryParameter("url") ?: data.getQueryParameter("sub")
            if (!subUrl.isNullOrBlank()) pendingExternalSubscription = subUrl
        }
    }

    private fun importExternalSubscription(rawImport: String) {
        val storage = SecureTokenStorage.getInstance(applicationContext)
        if (!storage.getAccessToken().isNullOrBlank() || AccountRepository.currentUser.value != null) {
            Toast.makeText(
                applicationContext,
                if (com.example.bymevpn.data.LocaleManager.isRussian()) {
                    "Внешнюю подписку нельзя импортировать в аккаунт ByMeVPN. Выйдите из аккаунта, чтобы открыть её отдельно."
                } else {
                    "External subscriptions cannot replace your ByMeVPN account. Sign out to open one separately."
                },
                Toast.LENGTH_LONG
            ).show()
            return
        }

        lifecycleScope.launch {
            val result = SubscriptionManager.getInstance(applicationContext)
                .applySubscriptionUrl(rawImport, includeDeviceHeaders = false)
            if (result.isSuccess) {
                AccountRepository.createLocalProfile(applicationContext, "Imported subscription")
                storage.setExternalProfileMode(true)
                SubscriptionManager.getInstance(applicationContext)
                    .acceptExternalProfile(result.getOrThrow())
                VpnManager.getInstance(applicationContext).loadServers()
                Toast.makeText(
                    applicationContext,
                    if (com.example.bymevpn.data.LocaleManager.isRussian()) "Подписка импортирована" else "Subscription imported",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    applicationContext,
                    if (com.example.bymevpn.data.LocaleManager.isRussian()) {
                        "Не удалось импортировать подписку. Проверьте ссылку и подключение к интернету."
                    } else {
                        "Could not import the subscription. Check the link and internet connection."
                    },
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun externalImportHost(rawImport: String): String = runCatching {
        val uri = Uri.parse(rawImport)
        uri.host?.takeIf { it.isNotBlank() } ?: ""
    }.getOrDefault("")

    private fun startTelegramLoginFromConnection(allowAccountSwitch: Boolean = false) {
        if (telegramLoginJob?.isActive == true) return
        if (!allowAccountSwitch &&
            !SecureTokenStorage.getInstance(applicationContext).getAccessToken().isNullOrBlank()
        ) {
            Toast.makeText(
                applicationContext,
                if (com.example.bymevpn.data.LocaleManager.isRussian()) {
                    "В ByMeVPN уже выполнен вход. Для смены аккаунта сначала выйдите из него."
                } else {
                    "ByMeVPN is already signed in. Sign out first to switch accounts."
                },
                Toast.LENGTH_LONG,
            ).show()
            return
        }

        telegramLoginJob = lifecycleScope.launch {
            val sessionResult = AccountRepository.createTelegramAuthSession(applicationContext)
            if (sessionResult.isFailure) {
                val message = if (com.example.bymevpn.data.LocaleManager.isRussian()) {
                    "Не удалось начать вход через Telegram. Попробуйте ещё раз."
                } else {
                    "Could not start Telegram sign-in. Please try again."
                }
                Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
                return@launch
            }
            val session = sessionResult.getOrThrow()
            if (!TelegramLinkOpener.open(this@MainActivity, session.botUrl)) {
                val message = if (com.example.bymevpn.data.LocaleManager.isRussian()) {
                    "Не удалось открыть Telegram. Установите Telegram и попробуйте снова."
                } else {
                    "Could not open Telegram. Install it and try again."
                }
                Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
                return@launch
            }

            val waitingMessage = if (com.example.bymevpn.data.LocaleManager.isRussian()) {
                "Подтвердите вход в ByMeVPN Bot. Аккаунт и подписка загрузятся в приложение."
            } else {
                "Confirm sign-in in ByMeVPN Bot. Your account and subscription will load in the app."
            }
            Toast.makeText(applicationContext, waitingMessage, Toast.LENGTH_LONG).show()
            var transientFailures = 0
            for (attempt in 1..40) {
                val result = AccountRepository.checkTelegramAuthSession(applicationContext, session.sessionToken)
                if (result.isSuccess && result.getOrNull() != null) {
                    val message = if (com.example.bymevpn.data.LocaleManager.isRussian()) {
                        "Вход через Telegram выполнен."
                    } else {
                        "Signed in with Telegram."
                    }
                    Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val error = result.exceptionOrNull()
                if (error != null) {
                    if (!TelegramAuthPollPolicy.shouldRetry(error, transientFailures)) {
                        val message = if (com.example.bymevpn.data.LocaleManager.isRussian()) {
                            "Не удалось подтвердить вход. Откройте ByMeVPN и попробуйте снова."
                        } else {
                            "Could not confirm sign-in. Open ByMeVPN and try again."
                        }
                        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
                        return@launch
                    }
                    transientFailures++
                }
                delay(1500)
            }
            val message = if (com.example.bymevpn.data.LocaleManager.isRussian()) {
                "Время ожидания истекло. Откройте ByMeVPN и попробуйте снова."
            } else {
                "Sign-in timed out. Open ByMeVPN and try again."
            }
            Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        VpnManager.getInstance(applicationContext).syncWithServiceState()
        // An unreachable update host must not delay account and server refresh.
        lifecycleScope.launch(Dispatchers.IO) {
            AppUpdateManager.checkForUpdates(applicationContext)
        }
        lifecycleScope.launch(Dispatchers.IO) {
            com.example.bymevpn.data.payment.YooKassaPaymentHandler.checkAndCompletePendingPayment(applicationContext)
        }
        lifecycleScope.launch(Dispatchers.IO) {
            val appContext = applicationContext

            val now = SystemClock.elapsedRealtime()
            val hasSession = !SecureTokenStorage.getInstance(appContext).getAccessToken().isNullOrBlank()
            if (hasSession && telegramLoginJob?.isActive != true &&
                (lastAccountRefreshAt == 0L || now - lastAccountRefreshAt >= 20_000L)
            ) {
                lastAccountRefreshAt = now
                coroutineScope {
                    val profile = async { AccountRepository.refreshProfile(appContext) }
                    val devices = async { AccountRepository.refreshDevices(appContext) }
                    val status = SubscriptionManager.getInstance(appContext)
                        .refreshStatus(forceBackend = true).getOrNull()
                    // /servers is entitlement-gated. Fetch it only after the
                    // current account's authoritative status confirms access.
                    if (status?.isUsable == true) VpnManager.getInstance(appContext).loadServers()
                    profile.await()
                    devices.await()
                }
            }
        }
    }
}

@Composable
private fun ExternalSubscriptionImportDialog(
    sourceHost: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val isRu = com.example.bymevpn.data.LocaleManager.isRussian()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isRu) "Импортировать конфигурацию?" else "Import this configuration?")
        },
        text = {
            Text(
                if (isRu) {
                    val source = sourceHost.ifBlank { "внешняя ссылка" }
                    "Источник: $source. Это сторонняя конфигурация: её оператор может видеть метаданные вашего трафика. Импорт заменит список серверов отдельного профиля. Продолжайте, только если доверяете источнику."
                } else {
                    val source = sourceHost.ifBlank { "external link" }
                    "Source: $source. This is a third-party configuration; its operator may see your traffic metadata. Importing replaces the server list in a separate profile. Continue only if you trust the source."
                }
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(if (isRu) "Импортировать" else "Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isRu) "Отмена" else "Cancel")
            }
        }
    )
}

@Composable
fun MainAppNav(onRestoreViaTelegram: () -> Unit = {}) {
    val context = LocalContext.current
    val currentUser by AccountRepository.currentUser.collectAsState()
    LaunchedEffect(Unit) {
        com.example.bymevpn.data.settings.SettingsRepository.getInstance(context).settingsFlow.collect { settings ->
            com.example.bymevpn.data.LocaleManager.setLanguage(com.example.bymevpn.data.AppLanguage.fromCode(settings.language))
        }
    }

    var currentScreen by rememberSaveable {
        mutableStateOf(if (currentUser != null) AppScreen.Home else AppScreen.SignIn)
    }
    var pricingDeviceCount by remember { mutableStateOf(2) }
    var pricingReturnScreen by rememberSaveable { mutableStateOf(AppScreen.Home) }
    var supportReturnScreen by rememberSaveable { mutableStateOf(AppScreen.Account) }
    fun openPricing(deviceCount: Int = 2) {
        pricingDeviceCount = deviceCount
        pricingReturnScreen = currentScreen
        currentScreen = AppScreen.Pricing
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
        transitionSpec = { fadeIn(animationSpec = tween(160)) togetherWith fadeOut(animationSpec = tween(160)) },
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
                    onNavigateToPricing = { openPricing() },
                    onNavigateToSupport = { supportReturnScreen = AppScreen.Home; currentScreen = AppScreen.Support },
                    onRestoreViaTelegram = onRestoreViaTelegram,
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
                    onNavigateToSupport = { supportReturnScreen = AppScreen.Account; currentScreen = AppScreen.Support },
                    onNavigateToTrial = { currentScreen = AppScreen.Trial },
                    onNavigateToPricing = { openPricing() },
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
                    onNavigateToAccount = { currentScreen = AppScreen.Account },
                    onNavigateToPricing = { devices -> openPricing(devices) }
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
                    onBackClick = { currentScreen = supportReturnScreen },
                    onNavigateToHome = { currentScreen = AppScreen.Home },
                    onNavigateToSettings = { currentScreen = AppScreen.Settings },
                    onNavigateToAccount = { currentScreen = AppScreen.Account },
                    originTab = if (supportReturnScreen == AppScreen.Home) com.example.bymevpn.components.BottomNavTab.Home else com.example.bymevpn.components.BottomNavTab.Account
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
                    onBackClick = { currentScreen = pricingReturnScreen },
                    initialDeviceCount = pricingDeviceCount
                )
            }
        }
    }
}
