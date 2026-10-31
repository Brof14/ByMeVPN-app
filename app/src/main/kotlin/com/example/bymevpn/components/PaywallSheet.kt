package com.example.bymevpn.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.bymevpn.data.LocaleManager
import com.example.bymevpn.data.api.ApiError
import com.example.bymevpn.data.api.ByMeApiClient
import com.example.bymevpn.data.api.PlanItem
import com.example.bymevpn.data.payment.PlanTariff
import com.example.bymevpn.data.payment.YooKassaPaymentHandler
import com.example.bymevpn.theme.AppColors
import kotlinx.coroutines.launch

/**
 * Premium Paywall Sheet (Happ & Top VPN style).
 * Offers:
 * 1) 3-Day Free Trial activation in 1 tap (no credit card required)
 * 2) YooKassa subscription plans (SBP, SberPay, T-Pay, Mir cards)
 * 3) Payments are created and confirmed through the app backend.
 */
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
    var selectedPlan by remember { mutableStateOf<PlanTariff?>(null) }
    var plansLoading by remember { mutableStateOf(true) }
    var plansUnavailable by remember { mutableStateOf(false) }
    var mobileCatalogNotDeployed by remember { mutableStateOf(false) }
    var isActivatingTrial by remember { mutableStateOf(false) }
    var isProcessingPayment by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            serverPlans = ByMeApiClient(context).getPlans(devices = 2)
            plansUnavailable = serverPlans.isEmpty()
        } catch (e: Exception) {
            plansUnavailable = true
            mobileCatalogNotDeployed = (e as? ApiError)?.httpStatusCode == 404
        } finally {
            plansLoading = false
        }
    }

    val tariffOptions = serverPlans.map { plan ->
        PlanTariff(
            code = plan.code,
            name = plan.name,
            period = plan.period,
            priceRub = plan.priceRub,
            pricePerMonthRub = if (plan.days > 0) plan.priceRub * 30 / plan.days else plan.priceRub,
            discountPercent = plan.discountPercent,
            isBestValue = plan.isPopular,
            days = plan.days
        )
    }
    LaunchedEffect(serverPlans) {
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
                .clip(RoundedCornerShape(26.dp))
                .background(AppColors.CardBg)
                .border(1.2.dp, AppColors.CardBorder, RoundedCornerShape(26.dp))
                .padding(22.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Close Button & Crown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(AppColors.CardBgElevated)
                            .border(1.dp, AppColors.BrandGreen.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isRu) "👑 ПРЕМИУМ" else "👑 PREMIUM",
                            color = AppColors.BrandGreen,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(AppColors.InputBg)
                            .clickable { onDismissRequest() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✕", color = AppColors.TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                ShieldLogo(size = 70.dp, isActive = true)

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (isRu) "Активируйте ByMeVPN" else "Activate ByMeVPN",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isRu) "Свободный интернет без ограничений и блокировок" else "Free internet without limits and censorship",
                    color = AppColors.TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Feature Bullet points
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(AppColors.InputBg)
                        .border(1.dp, AppColors.CardBorder, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PaywallFeatureRow(
                        icon = "⚡",
                        title = if (isRu) "Скорость до 1 Гбит/с" else "Speed up to 1 Gbps",
                        desc = if (isRu) "YouTube 4K, Reels, игры и скачивание без задержек" else "YouTube 4K, streaming & gaming without lag"
                    )
                    PaywallFeatureRow(
                        icon = "🛡️",
                        title = if (isRu) "VLESS Reality & Hysteria 2" else "VLESS Reality & Hysteria 2",
                        desc = if (isRu) "Полная маскировка трафика от блокировок" else "Undetectable traffic masking"
                    )
                    PaywallFeatureRow(
                        icon = "🚫",
                        title = if (isRu) "Блокировка рекламы" else "Ad-blocking built in",
                        desc = if (isRu) "AdGuard DNS защищает от баннеров и слежки" else "AdGuard DNS blocks banners & trackers"
                    )
                    PaywallFeatureRow(
                        icon = "📱",
                        title = if (isRu) "До 5 устройств на аккаунт" else "Up to 5 devices per account",
                        desc = if (isRu) "Телефон, ПК, планшет, Smart TV" else "Phone, PC, tablet, Smart TV"
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Free Trial Banner (1-tap trial)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF0F261D), AppColors.CardBgElevated)
                            )
                        )
                        .border(1.3.dp, AppColors.BrandGreen, RoundedCornerShape(18.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🎁", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isRu) "Попробуйте 3 дня бесплатно" else "Try 3 days for free",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isRu) "Без привязки карты • Все серверы сразу" else "No card needed • All servers unlocked",
                                color = AppColors.TextSecondary,
                                fontSize = 11.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppColors.BrandGreen)
                            .clickable(enabled = !isActivatingTrial) {
                                isActivatingTrial = true
                                scope.launch {
                                    val res = YooKassaPaymentHandler.activateTrial(context)
                                    isActivatingTrial = false
                                    if (res.isSuccess) {
                                        Toast.makeText(
                                            context,
                                            if (isRu) "Пробный период 3 дня активирован! Все серверы открыты." else "3-day trial activated! All servers unlocked.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        onSubscriptionActivated()
                                        onDismissRequest()
                                    } else {
                                        Toast.makeText(context, "Не удалось активировать триал", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isActivatingTrial) {
                            CircularProgressIndicator(color = AppColors.DarkText, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text(
                                text = if (isRu) "Активировать 3 дня бесплатно" else "Activate 3 Days Free",
                                color = AppColors.DarkText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Tariff selection header
                Text(
                    text = if (isRu) "Или выберите тариф:" else "Or choose a subscription plan:",
                    color = AppColors.TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Plans list
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
                    CircularProgressIndicator(color = AppColors.BrandGreen, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                } else if (plansUnavailable || tariffOptions.isEmpty()) {
                    Text(
                        text = if (mobileCatalogNotDeployed) {
                            if (isRu) "Сервер тарифов ещё не обновлён. Оплата пока недоступна." else "The server has not enabled mobile plans yet. Checkout is unavailable."
                        } else {
                            if (isRu) "Тарифы временно недоступны. Попробуйте позже." else "Plans are temporarily unavailable. Please try again later."
                        },
                        color = AppColors.TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Payment methods preview (SBP, SberPay, Mir)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isRu) "Способы оплаты: СБП • SberPay • T-Pay • Карты МИР" else "Payment: SBP • SberPay • T-Pay • Mir Cards",
                        color = AppColors.TextMuted,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Primary YooKassa Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(AppColors.BrandGreen, AppColors.BrandGreenGlow)
                            )
                        )
                        .clickable(enabled = !isProcessingPayment && !plansLoading && selectedPlan != null) {
                            isProcessingPayment = true
                            YooKassaPaymentHandler.openCheckout(
                                context = context,
                                tariff = selectedPlan!!,
                                onCheckoutFailed = { isProcessingPayment = false },
                                onCheckoutLaunched = { isProcessingPayment = false }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isProcessingPayment) {
                        CircularProgressIndicator(color = AppColors.DarkText, modifier = Modifier.size(22.dp))
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚡", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isRu) "Оплатить через ЮKassa • ${selectedPlan?.priceRub?.let { "$it ₽" } ?: "—"}" else "Pay via YooKassa • ${selectedPlan?.priceRub?.let { "$it ₽" } ?: "—"}",
                                color = AppColors.DarkText,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun PaywallFeatureRow(
    icon: String,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(text = icon, fontSize = 17.sp, modifier = Modifier.padding(top = 1.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(text = desc, color = AppColors.TextSecondary, fontSize = 11.5.sp, lineHeight = 16.sp)
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
    val borderColor = if (isSelected) AppColors.BrandGreen else AppColors.CardBorder
    val bg = if (isSelected) Color(0xFF0F1E19) else AppColors.InputBg

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(if (isSelected) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Radio circle
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, if (isSelected) AppColors.BrandGreen else AppColors.TextMuted, CircleShape)
                    .background(if (isSelected) AppColors.BrandGreen else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(AppColors.BgCanvas)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = plan.name,
                        color = Color.White,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (plan.badge != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AppColors.BrandGreen)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = plan.badge,
                                color = AppColors.DarkText,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                Text(
                    text = "${plan.pricePerMonthRub} ₽ / ${if (isRu) "месяц" else "month"}",
                    color = if (isSelected) AppColors.BrandGreen else AppColors.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Text(
            text = "${plan.priceRub} ₽",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
