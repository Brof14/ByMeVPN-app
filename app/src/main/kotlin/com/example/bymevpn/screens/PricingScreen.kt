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

@Composable
fun PricingScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var selectedDeviceCount by remember { mutableIntStateOf(2) } // 2, 5, 10
    var selectedPlan by remember { mutableStateOf(PlanDuration.Month15) }

    // Price calculation aligned with bot constants.py:
    // 2 devices: 89, 316, 552, 885
    // 5 devices: 109, 396, 712, 1185
    // 10 devices: 129, 476, 872, 1485
    val currentPrice = when (selectedDeviceCount) {
        5 -> when (selectedPlan) {
            PlanDuration.Month1 -> 109
            PlanDuration.Month4 -> 396
            PlanDuration.Month8 -> 712
            PlanDuration.Month15 -> 1185
        }
        10 -> when (selectedPlan) {
            PlanDuration.Month1 -> 129
            PlanDuration.Month4 -> 476
            PlanDuration.Month8 -> 872
            PlanDuration.Month15 -> 1485
        }
        else -> when (selectedPlan) {
            PlanDuration.Month1 -> 89
            PlanDuration.Month4 -> 316
            PlanDuration.Month8 -> 552
            PlanDuration.Month15 -> 885
        }
    }

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
                        text = "Выберите тариф",
                        color = AppColors.TextPrimary,
                        fontSize = 27.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-1.15).sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Одна подписка для всех ваших устройств",
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
                    text = "СКОЛЬКО УСТРОЙСТВ?",
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
                        label = "2 устройства",
                        selected = selectedDeviceCount == 2,
                        onClick = { selectedDeviceCount = 2 },
                        modifier = Modifier.weight(1f)
                    )
                    DeviceCountTab(
                        label = "5 устройств",
                        selected = selectedDeviceCount == 5,
                        onClick = { selectedDeviceCount = 5 },
                        modifier = Modifier.weight(1f)
                    )
                    DeviceCountTab(
                        label = "10 устройств",
                        selected = selectedDeviceCount == 10,
                        onClick = { selectedDeviceCount = 10 },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "СРОК ПОДПИСКИ",
                    color = AppColors.TextKicker,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 4 Plan options
                PlanDuration.values().forEach { plan ->
                    val priceForPlan = when (selectedDeviceCount) {
                        5 -> when (plan) {
                            PlanDuration.Month1 -> 109
                            PlanDuration.Month4 -> 396
                            PlanDuration.Month8 -> 712
                            PlanDuration.Month15 -> 1185
                        }
                        10 -> when (plan) {
                            PlanDuration.Month1 -> 129
                            PlanDuration.Month4 -> 476
                            PlanDuration.Month8 -> 872
                            PlanDuration.Month15 -> 1485
                        }
                        else -> when (plan) {
                            PlanDuration.Month1 -> 89
                            PlanDuration.Month4 -> 316
                            PlanDuration.Month8 -> 552
                            PlanDuration.Month15 -> 885
                        }
                    }

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
                                    color = if (isSelected) Color(0xFF37CF9E) else Color(0xFF283C3E),
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .clickable { selectedPlan = plan }
                                .padding(horizontal = 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = plan.title,
                                    color = AppColors.TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (plan == PlanDuration.Month1) "$monthlyRate ₽ за месяц" else "$monthlyRate ₽ / месяц ${plan.bonusSubtitle}",
                                    color = Color(0xFF8FA3A4),
                                    fontSize = 11.5.sp
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "$priceForPlan ₽",
                                    color = AppColors.TextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.5).sp
                                )

                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color(0xFF48E4B4) else Color.Transparent)
                                        .border(
                                            1.dp,
                                            if (isSelected) Color(0xFF48E4B4) else Color(0xFF607A77),
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
                                    .background(Color(0xFF38D9A5))
                                    .padding(horizontal = 9.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "ЛУЧШАЯ ЦЕНА",
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
                        text = "К оплате сейчас",
                        color = Color(0xFFA1B5B3),
                        fontSize = 13.sp
                    )
                    Text(
                        text = "$currentPrice ₽",
                        color = AppColors.TextPrimary,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Доступ на ${selectedPlan.totalMonths} ${if (selectedPlan.totalMonths == 1) "месяц" else "месяцев"} · до $selectedDeviceCount устройств",
                    color = Color(0xFF8B9F9E),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
                )

                // CTA Button
                ByMePrimaryButton(
                    text = "Продолжить · $currentPrice ₽",
                    onClick = {
                        scope.launch {
                            Toast.makeText(context, "Инициализация безопасной оплаты $currentPrice ₽...", Toast.LENGTH_SHORT).show()
                            val success = YooKassaPaymentHandler.startPayment(
                                context = context,
                                planCode = "plan_${selectedPlan.name.lowercase()}_${selectedDeviceCount}dev",
                                planTitle = "${selectedPlan.title} ($selectedDeviceCount устройств)",
                                amountRub = currentPrice,
                                days = selectedPlan.totalDays
                            )
                            if (!success) {
                                Toast.makeText(context, "Не удалось открыть форму оплаты. Попробуйте снова.", Toast.LENGTH_LONG).show()
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
                        text = "Безопасная оплата через ЮKassa",
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
            .background(if (selected) Color(0xFF43DDB1) else Color.Transparent)
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
