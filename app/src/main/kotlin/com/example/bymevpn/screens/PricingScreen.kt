package com.example.bymevpn.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bymevpn.components.ByMePrimaryButton
import com.example.bymevpn.components.CheckmarkIcon
import com.example.bymevpn.components.ChevronLeftIcon
import com.example.bymevpn.components.LockIcon
import com.example.bymevpn.data.payment.YooKassaPaymentHandler
import com.example.bymevpn.data.api.ByMeApiClient
import com.example.bymevpn.data.api.PlanItem
import com.example.bymevpn.data.LocaleManager
import androidx.compose.runtime.collectAsState
import com.example.bymevpn.theme.AppColors
import com.example.bymevpn.theme.AppGradients
import kotlinx.coroutines.launch

enum class PlanDuration(
    val title: String,
    val bonusSubtitle: String,
    val totalMonths: Int,
    val totalDays: Int,
    val isPopular: Boolean = false
) {
    Month1("1 месяц", "за месяц", 1, 30),
    Month4("3 + 1 месяц", "· 1 мес. в подарок", 4, 120),
    Month8("6 + 2 месяца", "· 2 мес. в подарок", 8, 240),
    Month15("12 + 3 месяца", "· 3 мес. в подарок", 15, 450, isPopular = true)
}

private fun PlanDuration.apiCode(): String = when (this) {
    PlanDuration.Month1 -> "1m"
    PlanDuration.Month4 -> "3m"
    PlanDuration.Month8 -> "6m"
    PlanDuration.Month15 -> "12m"
}

/** Preview only. Checkout must use the server-confirmed /plans price. */
internal fun PlanDuration.matchesConfirmedPlan(plan: PlanItem, devices: Int): Boolean =
    plan.code == apiCode() &&
        plan.devices == devices &&
        plan.days == totalDays &&
        plan.priceRub > 0

internal fun PlanDuration.referencePriceRub(devices: Int): Int {
    require(devices in setOf(2, 5, 10))
    val months = when (this) {
        PlanDuration.Month1 -> 1
        PlanDuration.Month4 -> 3
        PlanDuration.Month8 -> 6
        PlanDuration.Month15 -> 12
    }
    val baseMonthly = when (months) { 1 -> 89; 3 -> 79; 6 -> 69; else -> 59 }
    val extraMonthly = when (devices) { 5 -> 20; 10 -> 40; else -> 0 }
    return (baseMonthly + extraMonthly) * (totalDays / 30)
}

@Composable
fun PricingScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val language by LocaleManager.currentLanguage.collectAsState()
    val isRu = LocaleManager.isRussian(language = language)
    val t: (String, String) -> String = { ru, en -> if (isRu) ru else en }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var selectedDeviceCount by remember { mutableIntStateOf(2) } // 2, 5, 10
    var selectedPlan by remember { mutableStateOf(PlanDuration.Month15) }
    val selectedProvider = "yookassa" // Only payment method accepted by the current backend.
    var serverPlans by remember { mutableStateOf<List<PlanItem>>(emptyList()) }
    var plansLoading by remember { mutableStateOf(true) }
    var priceError by remember { mutableStateOf(false) }
    var refreshSequence by remember { mutableIntStateOf(0) }
    var isPaying by remember { mutableStateOf(false) }

    LaunchedEffect(selectedDeviceCount, refreshSequence) {
        serverPlans = emptyList()
        priceError = false
        plansLoading = true
        try {
            serverPlans = ByMeApiClient(context).getPlans(selectedDeviceCount)
            priceError = serverPlans.none { it.devices == selectedDeviceCount && it.priceRub > 0 }
        } catch (_: Exception) {
            serverPlans = emptyList()
            priceError = true
        } finally {
            plansLoading = false
        }
    }

    // Only a server-confirmed exact plan is eligible for actual checkout.
    val currentPlan = serverPlans.firstOrNull { selectedPlan.matchesConfirmedPlan(it, selectedDeviceCount) }
    val currentPrice = currentPlan?.priceRub ?: 0
    val currentPriceLabel = if (currentPrice > 0) "$currentPrice ₽"
        else "≈ ${selectedPlan.referencePriceRub(selectedDeviceCount)} ₽"

    val perMonthPrice = when (selectedPlan) {
        PlanDuration.Month1 -> currentPrice
        PlanDuration.Month4 -> currentPrice / 4
        PlanDuration.Month8 -> currentPrice / 8
        PlanDuration.Month15 -> currentPrice / 15
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.BgCanvas)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(bottom = 32.dp)
        ) {
            // Topbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 26.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(37.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(AppColors.TopBarBackBg)
                        .border(1.dp, AppColors.TopBarBackBorder, RoundedCornerShape(14.dp))
                        .clickable(onClick = onBackClick),
                    contentAlignment = Alignment.Center
                ) {
                    ChevronLeftIcon(color = Color(0xFFC5D8D5), size = 18.dp)
                }

                Column {
                    Text(
                        text = t("Выберите тариф", "Choose a plan"),
                        color = AppColors.TextPrimary,
                        fontSize = 27.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-1.15).sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = t("Одна подписка для всех ваших устройств", "One plan for all your devices"),
                        color = Color(0xFF8FA3A4),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 26.dp)
            ) {
                Text(
                    text = t("СКОЛЬКО УСТРОЙСТВ?", "HOW MANY DEVICES?"),
                    color = AppColors.TextKicker,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Device segment tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(51.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(Color(0xFF142022))
                        .border(1.dp, Color(0xFF29403F), RoundedCornerShape(15.dp))
                        .padding(5.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    DeviceCountTab(
                        label = t("2 устройства", "2 devices"),
                        selected = selectedDeviceCount == 2,
                        onClick = { selectedDeviceCount = 2 },
                        modifier = Modifier.weight(1f)
                    )
                    DeviceCountTab(
                        label = t("5 устройств", "5 devices"),
                        selected = selectedDeviceCount == 5,
                        onClick = { selectedDeviceCount = 5 },
                        modifier = Modifier.weight(1f)
                    )
                    DeviceCountTab(
                        label = t("10 устройств", "10 devices"),
                        selected = selectedDeviceCount == 10,
                        onClick = { selectedDeviceCount = 10 },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                if (priceError && !plansLoading) {
                    Text(
                        text = if (selectedDeviceCount == 2)
                            t("Нет подтверждения цены от сервера. Проверьте сеть и повторите.", "Price not confirmed. Check your connection and retry.")
                        else
                            t("Цена пока предварительная: сервер ещё не подтвердил тариф на $selectedDeviceCount устройств.", "Estimated price: the server has not confirmed $selectedDeviceCount-device plans."),
                        color = AppColors.TextSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = t("Повторить загрузку", "Retry loading"),
                        color = AppColors.BrandMint,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .clickable { refreshSequence++ }
                            .padding(vertical = 12.dp)
                    )
                } else if (plansLoading) {
                    Text(t("Проверяем цены и доступность тарифов…", "Checking prices and availability…"), color = AppColors.TextSecondary, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = t("СРОК ПОДПИСКИ", "SUBSCRIPTION LENGTH"),
                    color = AppColors.TextKicker,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 4 Plan options
                PlanDuration.values().forEach { plan ->
                    val verifiedPrice = serverPlans.firstOrNull {
                        plan.matchesConfirmedPlan(it, selectedDeviceCount)
                    }?.priceRub
                    val priceForPlan = verifiedPrice ?: plan.referencePriceRub(selectedDeviceCount)

                    val monthlyRate = when (plan) {
                        PlanDuration.Month1 -> priceForPlan
                        PlanDuration.Month4 -> priceForPlan / 4
                        PlanDuration.Month8 -> priceForPlan / 8
                        PlanDuration.Month15 -> priceForPlan / 15
                    }

                    val isSelected = selectedPlan == plan
                    val isPopular = plan.isPopular

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(95.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    if (isSelected && isPopular) Color(0xFF152A26)
                                    else if (isSelected) Color(0xFF132221)
                                    else Color(0xFF111C1D)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Color(0xFF69D3BC) else Color(0xFF283C3E),
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .clickable { selectedPlan = plan }
                                .padding(horizontal = 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = if (isRu) plan.title else when (plan) { PlanDuration.Month1 -> "1 month"; PlanDuration.Month4 -> "3 + 1 months"; PlanDuration.Month8 -> "6 + 2 months"; PlanDuration.Month15 -> "12 + 3 months" },
                                    color = AppColors.TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (verifiedPrice == null) t("Предварительно · $monthlyRate ₽/мес.", "Estimate · $monthlyRate RUB/month") else if (plan == PlanDuration.Month1) "$monthlyRate ₽ за месяц" else "$monthlyRate ₽ / месяц ${plan.bonusSubtitle}",
                                    color = Color(0xFF8FA3A4),
                                    fontSize = 11.5.sp
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = if (verifiedPrice != null) "$priceForPlan ₽" else "≈ $priceForPlan ₽",
                                    color = AppColors.TextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.5).sp
                                )

                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color(0xFF78D3C9) else Color.Transparent)
                                        .border(
                                            1.dp,
                                            if (isSelected) Color(0xFF78D3C9) else Color(0xFF607A77),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        CheckmarkIcon(color = Color(0xFF0A281E), size = 13.dp)
                                    }
                                }
                            }
                        }

                        if (isPopular) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-16).dp, y = (-8).dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF69D3BC))
                                    .padding(horizontal = 9.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = t("ЛУЧШАЯ ЦЕНА", "BEST VALUE"),
                                    color = Color(0xFF06231A),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.4.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = t("СПОСОБ ОПЛАТЫ", "PAYMENT METHOD"),
                    color = AppColors.TextKicker,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF132023))
                        .border(1.dp, Color(0xFF263A3C), RoundedCornerShape(14.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (selectedProvider == "yookassa") Color(0xFF69D3BC) else Color.Transparent)
                            
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = t("💳 Карта / СБП", "💳 Card / SBP"),
                            color = if (selectedProvider == "yookassa") Color(0xFF10241E) else Color(0xFF829C9A),
                            fontSize = 12.sp,
                            fontWeight = if (selectedProvider == "yookassa") FontWeight.Bold else FontWeight.Medium
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(11.dp))
                            .background(Color.Transparent)
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = t("Криптовалюта недоступна", "Crypto unavailable"),
                            color = Color(0xFF829C9A),
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Transparent Price Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(width = 0.dp, color = Color.Transparent)
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = t("К оплате сейчас", "Amount due"),
                        color = Color(0xFFA1B5B3),
                        fontSize = 13.sp
                    )
                    Text(
                        text = currentPriceLabel,
                        color = AppColors.TextPrimary,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = if (isRu) "Доступ на ${selectedPlan.totalMonths} ${if (selectedPlan.totalMonths == 1) "месяц" else "месяцев"} · до $selectedDeviceCount устройств" else "Access for ${selectedPlan.totalMonths} months · up to $selectedDeviceCount devices",
                    color = Color(0xFF8B9F9E),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
                )

                // CTA Button
                ByMePrimaryButton(
                    text = when {
                        isPaying -> t("Открываем оплату…", "Opening checkout…")
                        currentPrice > 0 -> t("Оплатить · $currentPrice ₽", "Pay · $currentPrice RUB")
                        plansLoading -> t("Загружаем тарифы…", "Loading plans…")
                        else -> t("Оплата пока недоступна", "Payment unavailable")
                    },
                    enabled = currentPlan != null && !isPaying,
                    onClick = {
                        val confirmed = currentPlan ?: return@ByMePrimaryButton
                        isPaying = true
                        scope.launch {
                            try {
                                val success = YooKassaPaymentHandler.startPayment(
                                    context = context,
                                    planCode = confirmed.code,
                                    planTitle = if (isRu) "${selectedPlan.title} ($selectedDeviceCount устройств)" else "$selectedDeviceCount devices",
                                    amountRub = confirmed.priceRub,
                                    days = confirmed.days,
                                    devices = selectedDeviceCount,
                                    provider = selectedProvider
                                )
                                if (!success) {
                                    Toast.makeText(context, t("Не удалось открыть оплату. Проверьте подключение и попробуйте снова.", "Could not open checkout. Check your connection."), Toast.LENGTH_LONG).show()
                                }
                            } finally {
                                isPaying = false
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LockIcon(color = Color(0xFF729D90), size = 15.dp)
                    Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                    Text(
                        text = t("Безопасная оплата через ЮKassa", "Secure checkout via YooKassa"),
                        color = Color(0xFF729D90),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceCountTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .background(if (selected) Color(0xFF69D3BC) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color(0xFF10241E) else Color(0xFF829C9A),
            fontSize = 11.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
