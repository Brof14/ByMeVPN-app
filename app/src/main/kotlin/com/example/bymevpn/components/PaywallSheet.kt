package com.example.bymevpn.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.bymevpn.BuildConfig
import com.example.bymevpn.data.LocaleManager
import com.example.bymevpn.data.api.ApiError
import com.example.bymevpn.data.api.ByMeApiClient
import com.example.bymevpn.data.api.PlanItem
import com.example.bymevpn.data.payment.PlanTariff
import com.example.bymevpn.data.payment.YooKassaPaymentHandler
import com.example.bymevpn.data.payment.roundedMonthlyPrice
import com.example.bymevpn.theme.AppColors
import com.example.bymevpn.theme.AppGradients
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun PaywallSheet(
    onDismissRequest: () -> Unit,
    onSubscriptionActivated: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentLanguage by LocaleManager.currentLanguage.collectAsState()
    val isRu = remember(currentLanguage) { LocaleManager.isRussian(language = currentLanguage) }
    val scope = rememberCoroutineScope()

    var serverPlans by remember { mutableStateOf<List<PlanItem>>(emptyList()) }
    var displayPlans by remember {
        mutableStateOf(
            if (BuildConfig.EXTERNAL_CHECKOUT_ENABLED) ByMeApiClient(context).getCachedPlans(2) else emptyList()
        )
    }
    var selectedPlan by remember { mutableStateOf<PlanTariff?>(null) }
    var selectedDeviceCount by remember { mutableIntStateOf(2) }
    var plansLoading by remember { mutableStateOf(BuildConfig.EXTERNAL_CHECKOUT_ENABLED) }
    var plansUnavailable by remember { mutableStateOf(false) }
    var mobileCatalogNotDeployed by remember { mutableStateOf(false) }
    var refreshSequence by remember { mutableIntStateOf(0) }
    var isProcessingPayment by remember { mutableStateOf(false) }

    LaunchedEffect(selectedDeviceCount, refreshSequence) {
        if (!BuildConfig.EXTERNAL_CHECKOUT_ENABLED) {
            plansLoading = false
            return@LaunchedEffect
        }
        val api = ByMeApiClient(context)
        serverPlans = emptyList()
        displayPlans = api.getCachedPlans(selectedDeviceCount)
        plansUnavailable = false
        mobileCatalogNotDeployed = false
        plansLoading = true
        try {
            serverPlans = api.getPlans(devices = selectedDeviceCount)
            displayPlans = serverPlans
            plansUnavailable = serverPlans.none { it.devices == selectedDeviceCount }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            serverPlans = emptyList()
            plansUnavailable = true
            mobileCatalogNotDeployed = (e as? ApiError)?.httpStatusCode == 404
        }
        plansLoading = false
    }

    val tariffOptions = filterPlansByDeviceCount(displayPlans, selectedDeviceCount).map { plan ->
        PlanTariff(
            code = plan.code,
            name = plan.name,
            period = plan.period,
            priceRub = plan.priceRub,
            pricePerMonthRub = roundedMonthlyPrice(plan.priceRub, plan.days),
            discountPercent = plan.discountPercent,
            isBestValue = plan.isPopular,
            days = plan.days,
            checkoutEnabled = plan.purchaseEnabled
        )
    }
    val confirmedTariffOptions = filterPlansByDeviceCount(serverPlans, selectedDeviceCount).map { plan ->
        PlanTariff(
            code = plan.code,
            name = plan.name,
            period = plan.period,
            priceRub = plan.priceRub,
            pricePerMonthRub = roundedMonthlyPrice(plan.priceRub, plan.days),
            discountPercent = plan.discountPercent,
            isBestValue = plan.isPopular,
            days = plan.days,
            checkoutEnabled = plan.purchaseEnabled
        )
    }
    val confirmedSelectedPlan = confirmedTariffOptions.firstOrNull { it.code == selectedPlan?.code }
    LaunchedEffect(displayPlans) {
        if (selectedPlan == null || tariffOptions.none { it.code == selectedPlan?.code }) {
            selectedPlan = tariffOptions.firstOrNull()
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(24.dp))
                .background(AppColors.BgSurface)
                .border(1.2.dp, AppColors.BorderDefault, RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(AppColors.BgSurfaceElevated)
                            .border(1.dp, AppColors.BorderAccent, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SparkleStarIcon(color = AppColors.BrandMintBright, size = 14.dp)
                        Text(
                            text = if (isRu) "Доступ ByMeVPN" else "ByMeVPN Access",
                            color = AppColors.SkyBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(AppColors.TopBarIconBg)
                            .border(1.dp, AppColors.TopBarIconBorder, CircleShape)
                            .clickable(role = Role.Button, onClick = onDismissRequest),
                        contentAlignment = Alignment.Center
                    ) {
                        CloseXIcon(color = AppColors.TextSecondary, size = 16.dp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                ShieldLogo(size = 70.dp, isActive = true)

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (isRu) "Подключите ByMeVPN" else "Activate ByMeVPN",
                    color = AppColors.TextPrimary,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(AppColors.BgInput)
                        .border(1.dp, AppColors.BorderDefault, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PaywallFeatureRow(
                        icon = { ShieldSecurityIcon(color = AppColors.SkyBlue, size = 18.dp) },
                        title = "VLESS Reality & Hysteria 2",
                        desc = if (isRu) "Стабильное шифрование в разных сетях" else "Stable encrypted tunnels across networks"
                    )
                    PaywallFeatureRow(
                        icon = { SparkleStarIcon(color = AppColors.BrandMintBright, size = 18.dp) },
                        title = if (isRu) "AdGuard DNS в комплекте" else "Built-in AdGuard DNS",
                        desc = if (isRu) "Фильтрация рекламных и трекерных доменов" else "Filters ad and tracking domains"
                    )
                    PaywallFeatureRow(
                        icon = { DevicesMultiIcon(color = AppColors.SkyBlue, size = 18.dp) },
                        title = if (isRu) "Работает на всех устройствах" else "Works across your devices",
                        desc = if (isRu) "Смартфон, планшет, ноутбук или ТВ" else "Phone, tablet, laptop, or TV"
                    )
                }

                if (!BuildConfig.EXTERNAL_CHECKOUT_ENABLED) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isRu) {
                            "Войдите в свой аккаунт ByMeVPN для синхронизации доступа или активируйте бесплатный 3-дневный пробный период."
                        } else {
                            "Sign in to your ByMeVPN account to sync access or activate the free 3-day trial."
                        },
                        color = AppColors.TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    ByMePrimaryButton(
                        text = if (isRu) "Понятно" else "Close",
                        onClick = onDismissRequest,
                        showArrow = false,
                        height = 48.dp
                    )
                    return@Column
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = if (isRu) "Выберите тариф" else "Choose a subscription plan",
                    color = AppColors.TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(AppColors.BgInput)
                        .border(1.dp, AppColors.BorderDefault, RoundedCornerShape(14.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(2, 5, 10).forEach { deviceCount ->
                        val isSelected = selectedDeviceCount == deviceCount
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) AppGradients.PrimaryButton
                                    else androidx.compose.ui.graphics.Brush.horizontalGradient(
                                        listOf(Color.Transparent, Color.Transparent)
                                    )
                                )
                                .clickable(enabled = !plansLoading) { selectedDeviceCount = deviceCount }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isRu) "$deviceCount устр." else "$deviceCount dev.",
                                color = if (isSelected) AppColors.BrandMintDark else AppColors.TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                tariffOptions.forEach { plan ->
                    val isSelected = selectedPlan?.code == plan.code
                    PlanCard(
                        plan = plan,
                        isSelected = isSelected,
                        isRu = isRu,
                        onClick = { selectedPlan = plan }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (plansLoading) {
                    CircularProgressIndicator(color = AppColors.SkyBlue, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                } else if (plansUnavailable || tariffOptions.isEmpty()) {
                    Text(
                        text = if (tariffOptions.isNotEmpty()) {
                            if (isRu) "Показываем сохранённые цены. Обновите тарифы перед оплатой." else "Showing saved prices. Refresh plans before checkout."
                        } else if (mobileCatalogNotDeployed) {
                            if (isRu) "Покупка пока недоступна. Попробуйте позже." else "Purchases are temporarily unavailable. Please try again later."
                        } else {
                            if (isRu) "Тарифы временно недоступны. Попробуйте позже." else "Plans are temporarily unavailable. Please try again later."
                        },
                        color = AppColors.TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = if (isRu) "Повторить" else "Try again",
                        color = AppColors.SkyBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .clickable { refreshSequence++ }
                            .padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (isRu) "Способы оплаты: СБП • SberPay • T-Pay • Карты МИР" else "Payment: SBP • SberPay • T-Pay • Mir Cards",
                    color = AppColors.TextMuted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (isRu) "Разовая оплата без автоматического списания с карты."
                    else "One-time payment without automatic recurring card charges.",
                    color = AppColors.TextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )

                val canPay = !isProcessingPayment && !plansLoading && confirmedSelectedPlan?.checkoutEnabled == true
                ByMePrimaryButton(
                    text = when {
                        isProcessingPayment -> if (isRu) "Открываем оплату…" else "Opening checkout…"
                        plansLoading -> if (isRu) "Проверяем цену…" else "Checking price…"
                        confirmedSelectedPlan?.checkoutEnabled == true ->
                            if (isRu) "Оплатить • ${confirmedSelectedPlan.priceRub} ₽" else "Pay • ${confirmedSelectedPlan.priceRub} ₽"
                        else -> if (isRu) "Оплата временно недоступна" else "Checkout temporarily unavailable"
                    },
                    enabled = canPay,
                    onClick = {
                        val tariff = confirmedSelectedPlan ?: return@ByMePrimaryButton
                        isProcessingPayment = true
                        scope.launch {
                            YooKassaPaymentHandler.openCheckout(
                                context = context,
                                tariff = tariff,
                                devices = selectedDeviceCount,
                                onCheckoutFailed = { isProcessingPayment = false },
                                onCheckoutLaunched = { isProcessingPayment = false }
                            )
                        }
                    }
                )
            }
        }
    }
}

internal fun filterPlansByDeviceCount(plans: List<PlanItem>, deviceCount: Int): List<PlanItem> =
    plans.filter { it.devices == deviceCount }

@Composable
private fun PaywallFeatureRow(
    icon: @Composable () -> Unit,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(modifier = Modifier.padding(top = 2.dp)) {
            icon()
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = title, color = AppColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(text = desc, color = AppColors.TextSecondary, fontSize = 12.5.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun PlanCard(
    plan: PlanTariff,
    isSelected: Boolean,
    isRu: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) AppColors.BrandMint else AppColors.BorderDefault
    val bg = if (isSelected) AppColors.BgSurfaceElevated else AppColors.BgInput

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(if (isSelected) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, if (isSelected) AppColors.BrandMint else AppColors.TextMuted, CircleShape)
                    .background(if (isSelected) AppColors.BrandMint else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(AppColors.BrandMintDark)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = plan.name,
                        color = AppColors.TextPrimary,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (plan.badge != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AppColors.BrandMint)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = plan.badge,
                                color = AppColors.BrandMintDark,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                Text(
                    text = "${plan.pricePerMonthRub} ₽ / ${if (isRu) "месяц" else "month"}",
                    color = if (isSelected) AppColors.SkyBlue else AppColors.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Text(
            text = "${plan.priceRub} ₽",
            color = AppColors.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
