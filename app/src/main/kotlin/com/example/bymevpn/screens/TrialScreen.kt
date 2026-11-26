package com.example.bymevpn.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bymevpn.BuildConfig
import com.example.bymevpn.components.ByMePrimaryButton
import com.example.bymevpn.components.ByMeSecondaryButton
import com.example.bymevpn.components.ByMeTopBar
import com.example.bymevpn.components.CheckmarkIcon
import com.example.bymevpn.components.ShieldLogo
import com.example.bymevpn.data.LocaleManager
import com.example.bymevpn.data.api.AccountActionErrorMessages
import com.example.bymevpn.data.subscription.SubscriptionManager
import com.example.bymevpn.data.subscription.TrialActivationPolicy
import com.example.bymevpn.theme.AppColors
import com.example.bymevpn.theme.AppGradients
import kotlinx.coroutines.launch

@Composable
fun TrialScreen(
    onBackClick: () -> Unit,
    onChoosePlanClick: () -> Unit,
    onTrialActivated: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val language by LocaleManager.currentLanguage.collectAsState()
    val isRu = LocaleManager.isRussian(language = language)
    val t: (String, String) -> String = { ru, en -> if (isRu) ru else en }
    val scope = rememberCoroutineScope()
    val subManager = remember { SubscriptionManager.getInstance(context) }
    val subscription by subManager.subscriptionStatus.collectAsState()
    val checking by subManager.isSyncing.collectAsState()
    var activating by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { subManager.refreshStatus(forceBackend = true) }

    val offerTrial = TrialActivationPolicy.shouldOfferTrial(subscription)
    val trialAlreadyUsed = subscription?.trialUnavailableReason == "trial_already_used"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.BgCanvas)
    ) {
        ByMeTopBar(
            title = t("Пробный период", "Free trial"),
            onBackClick = onBackClick,
            onMoreClick = null
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 26.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ShieldLogo(size = 92.dp, isActive = true)

            Spacer(Modifier.height(16.dp))

            Text(
                text = if (trialAlreadyUsed) {
                    t("Пробный период использован", "Free trial already used")
                } else {
                    t("3 дня ByMeVPN бесплатно", "3 days of ByMeVPN free")
                },
                color = AppColors.TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = if (trialAlreadyUsed) {
                    if (BuildConfig.EXTERNAL_CHECKOUT_ENABLED) {
                        t("Выберите тариф, чтобы продолжить пользоваться VPN.", "Choose a plan to continue using VPN.")
                    } else {
                        t("Войдите в аккаунт с активной подпиской или обновите статус.", "Sign in with an active subscription or refresh your status.")
                    }
                } else {
                    t("Проверьте скорость и стабильность без привязки карты.", "Test speed and stability with no card required.")
                },
                color = AppColors.TextSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(22.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(AppGradients.AccentCard)
                    .border(1.dp, AppColors.BorderAccent, RoundedCornerShape(20.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TrialFeatureRow(t("Полный доступ ко всем серверам на 3 дня", "Full access to all servers for 3 days"))
                TrialFeatureRow(t("Быстрое подключение по VLESS Reality", "Fast VLESS Reality connection"))
                TrialFeatureRow(t("Без автосписаний и привязки карты", "No automatic charges or card required"))
            }

            Spacer(Modifier.height(24.dp))

            if (offerTrial) {
                ByMePrimaryButton(
                    text = if (activating) t("Подключаем…", "Activating…") else t("Активировать бесплатно", "Activate free trial"),
                    enabled = !checking && !activating,
                    onClick = {
                        if (!activating) {
                            activating = true
                            scope.launch {
                                val fresh = subManager.refreshStatus(forceBackend = true).getOrNull()
                                if (!TrialActivationPolicy.shouldOfferTrial(fresh)) {
                                    activating = false
                                    return@launch
                                }
                                val result = subManager.activateTrial()
                                activating = false
                                if (result.isSuccess) {
                                    onTrialActivated()
                                } else {
                                    Toast.makeText(
                                        context,
                                        AccountActionErrorMessages.activateTrial(result.exceptionOrNull(), isRu),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    }
                )
            } else if (!BuildConfig.EXTERNAL_CHECKOUT_ENABLED) {
                ByMePrimaryButton(
                    text = if (checking) t("Проверяем…", "Checking…") else t("Обновить статус", "Refresh status"),
                    enabled = !checking,
                    showArrow = false,
                    onClick = { scope.launch { subManager.refreshStatus(forceBackend = true) } }
                )
            }

            if (BuildConfig.EXTERNAL_CHECKOUT_ENABLED) {
                if (offerTrial) Spacer(Modifier.height(12.dp))
                if (offerTrial) {
                    ByMeSecondaryButton(
                        text = t("Посмотреть тарифы", "View plans"),
                        onClick = onChoosePlanClick
                    )
                } else {
                    ByMePrimaryButton(
                        text = t("Посмотреть тарифы", "View plans"),
                        onClick = onChoosePlanClick
                    )
                }
            }
        }
    }
}

@Composable
private fun TrialFeatureRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(AppColors.BgSurfaceAccent)
                .padding(5.dp),
            contentAlignment = Alignment.Center
        ) {
            CheckmarkIcon(color = AppColors.BrandMint, size = 14.dp)
        }
        Text(
            text = text,
            color = AppColors.TextPrimary,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}
