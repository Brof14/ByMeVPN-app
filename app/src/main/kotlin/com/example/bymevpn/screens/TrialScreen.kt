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
import kotlinx.coroutines.launch

@Composable
fun TrialScreen(onBackClick: () -> Unit, onChoosePlanClick: () -> Unit, onTrialActivated: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val language by LocaleManager.currentLanguage.collectAsState()
    val isRu = LocaleManager.isRussian(language = language)
    val scope = rememberCoroutineScope()
    var activating by remember { mutableStateOf(false) }
    Column(modifier.fillMaxSize().background(AppColors.BgCanvas)) {
        ByMeTopBar(title = if (isRu) "3 дня бесплатно" else "3 days free", onBackClick = onBackClick, onMoreClick = null)
        Column(Modifier.padding(26.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text(if (isRu) "Попробуйте VPN бесплатно" else "Try VPN for free", color = AppColors.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(if (isRu) "Пробный доступ на 3 дня, если он ещё не использован. После окончания выберите тариф, чтобы продолжить." else "Get 3 days of free access if your trial has not been used. Choose a plan to continue afterwards.", color = AppColors.TextPrimary, fontSize = 15.sp)
            ByMePrimaryButton(text = if (activating) (if (isRu) "Подключаем…" else "Activating…") else (if (isRu) "Активировать бесплатно" else "Activate free trial"), onClick = {
                if (!activating) {
                    activating = true
                    scope.launch {
                        val result = YooKassaPaymentHandler.activateTrial(context)
                        activating = false
                        if (result.isSuccess) onTrialActivated()
                        else Toast.makeText(context, if (isRu) "Не удалось активировать пробный доступ. Попробуйте позже." else "Could not activate trial. Try again later.", Toast.LENGTH_LONG).show()
                    }
                }
            })
            ByMePrimaryButton(text = if (isRu) "Посмотреть тарифы" else "View plans", onClick = onChoosePlanClick)
        }
    }
}
