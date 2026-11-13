package com.example.bymevpn.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bymevpn.components.ByMeTopBar
import com.example.bymevpn.components.ByMePrimaryButton
import com.example.bymevpn.data.payment.YooKassaPaymentHandler
import com.example.bymevpn.theme.AppColors
import com.example.bymevpn.data.LocaleManager
import com.example.bymevpn.data.subscription.SubscriptionManager
import com.example.bymevpn.data.subscription.TrialActivationPolicy
import kotlinx.coroutines.launch

@Composable
fun TrialScreen(onBackClick: () -> Unit, onChoosePlanClick: () -> Unit, onTrialActivated: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val language by LocaleManager.currentLanguage.collectAsState()
    val isRu = LocaleManager.isRussian(language = language)
    val scope = rememberCoroutineScope()
    val subManager = remember { SubscriptionManager.getInstance(context) }
    val subscription by subManager.subscriptionStatus.collectAsState()
    val checking by subManager.isSyncing.collectAsState()
    LaunchedEffect(Unit) { subManager.refreshStatus(forceBackend = true) }
    var activating by remember { mutableStateOf(false) }
    Column(modifier.fillMaxSize().background(AppColors.BgCanvas)) {
        ByMeTopBar(title = if (isRu) "Пробный период" else "Free trial", onBackClick = onBackClick, onMoreClick = null)
        Column(Modifier.padding(26.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text(if (subscription?.trialUnavailableReason == "trial_already_used") {
                if (isRu) "Пробный период использован" else "Free trial already used"
            } else if (isRu) "Пробный период" else "Free trial", color = AppColors.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(if (isRu) "Доступность пробного периода зависит от аккаунта." else "Trial availability depends on your account.", color = AppColors.TextPrimary, fontSize = 15.sp)
            if (subscription?.trialUnavailableReason == "trial_already_used") {
                Text(if (isRu) "Пробный период уже использован." else "The free trial has already been used.", color = AppColors.TextSecondary, fontSize = 15.sp)
            }
            if (TrialActivationPolicy.shouldOfferTrial(subscription)) ByMePrimaryButton(text = if (activating) (if (isRu) "Подключаем…" else "Activating…") else (if (isRu) "Активировать бесплатно" else "Activate free trial"), enabled = !checking && !activating, onClick = {
                if (!activating) {
                    activating = true
                    scope.launch {
                        val fresh = subManager.refreshStatus(forceBackend = true).getOrNull()
                        if (!TrialActivationPolicy.shouldOfferTrial(fresh)) {
                            activating = false
                            return@launch
                        }
                        val result = YooKassaPaymentHandler.activateTrial(context)
                        activating = false
                        if (result.isSuccess) onTrialActivated()
                        else Toast.makeText(
                            context,
                            com.example.bymevpn.data.api.AccountActionErrorMessages.activateTrial(
                                result.exceptionOrNull(), isRu
                            ),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            })
            ByMePrimaryButton(text = if (isRu) "Посмотреть тарифы" else "View plans", onClick = onChoosePlanClick)
        }
    }
}
