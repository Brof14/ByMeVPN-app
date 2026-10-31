package com.example.bymevpn.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bymevpn.components.BottomNavTab
import com.example.bymevpn.components.ByMeBottomNav
import com.example.bymevpn.components.ByMeTopBar
import com.example.bymevpn.data.subscription.SubscriptionManager
import com.example.bymevpn.data.LocaleManager
import com.example.bymevpn.theme.AppColors

@Composable
fun RenewalScreen(
    onBackClick: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val language by LocaleManager.currentLanguage.collectAsState()
    val isRu = LocaleManager.isRussian(language = language)
    val manager = remember { SubscriptionManager.getInstance(context) }
    val subscription by manager.subscriptionStatus.collectAsState()
    val status = when (subscription?.autoRenew) {
        true -> if (isRu) "Включено по данным сервера" else "Enabled by server"
        false -> if (isRu) "Отключено по данным сервера" else "Disabled by server"
        null -> if (isRu) "Нет подтверждённых данных" else "No confirmed data"
    }
    Column(modifier.fillMaxSize().background(AppColors.BgCanvas)) {
        ByMeTopBar(title = if (isRu) "Автопродление" else "Auto-renewal", onBackClick = onBackClick, onMoreClick = null)
        Column(Modifier.weight(1f).padding(26.dp)) {
            Text(if (isRu) "Статус: $status" else "Status: $status", color = AppColors.TextPrimary, fontSize = 19.sp)
            Spacer(Modifier.height(16.dp))
            Text(if (isRu) "Данные о привязанной карте, сумме и дате списания пока недоступны. Изменение автопродления в приложении ещё не подключено." else "Card details, next payment amount and charge date are unavailable. Managing renewal is not yet supported in the app.", color = AppColors.TextPrimary, fontSize = 14.sp)
        }
        ByMeBottomNav(currentTab = BottomNavTab.Account, onTabSelected = { tab ->
            when (tab) {
                BottomNavTab.Home -> onNavigateToHome()
                BottomNavTab.Settings -> onNavigateToSettings()
                BottomNavTab.Account -> onNavigateToAccount()
            }
        })
    }
}
