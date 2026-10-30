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
import kotlinx.coroutines.launch

@Composable
fun TrialScreen(onBackClick: () -> Unit, onChoosePlanClick: () -> Unit, onTrialActivated: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var activating by remember { mutableStateOf(false) }
    Column(modifier.fillMaxSize().background(AppColors.BgCanvas)) {
        ByMeTopBar(title = "3 дня бесплатно", onBackClick = onBackClick, onMoreClick = null)
        Column(Modifier.padding(26.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text("Попробуйте VPN бесплатно", color = AppColors.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Пробный доступ на 3 дня, если он ещё не использован. После окончания выберите тариф, чтобы продолжить.", color = AppColors.TextPrimary, fontSize = 15.sp)
            ByMePrimaryButton(text = if (activating) "Подключаем…" else "Активировать бесплатно", onClick = {
                if (!activating) {
                    activating = true
                    scope.launch {
                        val result = YooKassaPaymentHandler.activateTrial(context)
                        activating = false
                        if (result.isSuccess) onTrialActivated()
                        else Toast.makeText(context, "Не удалось активировать пробный доступ. Попробуйте позже.", Toast.LENGTH_LONG).show()
                    }
                }
            })
            ByMePrimaryButton(text = "Посмотреть тарифы", onClick = onChoosePlanClick)
        }
    }
}
