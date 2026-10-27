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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bymevpn.components.CheckmarkIcon
import com.example.bymevpn.components.ChevronLeftIcon
import com.example.bymevpn.components.ChevronRightIcon
import com.example.bymevpn.components.DevicesMultiIcon
import com.example.bymevpn.components.GlobeIcon
import com.example.bymevpn.components.ShieldLogo
import com.example.bymevpn.components.ByMePrimaryButton
import com.example.bymevpn.data.payment.YooKassaPaymentHandler
import com.example.bymevpn.theme.AppColors
import kotlinx.coroutines.launch

@Composable
fun TrialScreen(
    onBackClick: () -> Unit,
    onChoosePlanClick: () -> Unit,
    onTrialActivated: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

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
                .padding(bottom = 24.dp)
        ) {
            // Header: Back icon + kicker & Title
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
                        text = "BYMEVPN",
                        color = Color(0xFF68B8A7),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "Начните с 1 ₽",
                        color = AppColors.TextPrimary,
                        fontSize = 27.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-1.15).sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Trial Hero
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ShieldLogo(size = 87.dp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "3 дня полного доступа",
                    color = AppColors.TextPrimary,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Проверьте скорость, удобство и стабильность работы ByMeVPN.",
                    color = Color(0xFF8FA5A3),
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(26.dp))

            // Simple Benefits Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(Color(0xFF121F20))
                    .border(1.dp, Color(0xFF253A3A), RoundedCornerShape(17.dp))
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                BenefitRow(
                    icon = { GlobeIcon(color = Color(0xFFA1D5C5), size = 19.dp) },
                    text = "Все доступные серверы"
                )
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF233433)))
                BenefitRow(
                    icon = { DevicesMultiIcon(color = Color(0xFFA1D5C5), size = 19.dp) },
                    text = "До 2 устройств"
                )
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF233433)))
                BenefitRow(
                    icon = {
                        Text(
                            text = "⚡",
                            fontSize = 16.sp
                        )
                    },
                    text = "Подключение без сложной настройки"
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Billing Steps Card (1 ₽ today -> 89 ₽ after 3 days)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 29.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF111A1D))
                    .border(1.dp, Color(0xFF263939), RoundedCornerShape(18.dp))
                    .padding(horizontal = 17.dp, vertical = 20.dp)
            ) {
                // Step 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(31.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E5647)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "1",
                            color = Color(0xFF4DE7B2),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Сегодня",
                            color = AppColors.TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "3 дня пробного доступа",
                            color = Color(0xFF849D9A),
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        text = "1 ₽",
                        color = AppColors.TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Step line
                Box(
                    modifier = Modifier
                        .padding(start = 15.dp, top = 6.dp, bottom = 6.dp)
                        .width(1.dp)
                        .height(24.dp)
                        .background(Color(0xFF38514B))
                )

                // Step 2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(31.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF243434)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "2",
                            color = Color(0xFFAABDBB),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Через 3 дня",
                            color = AppColors.TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "30 дней доступа при автопродлении",
                            color = Color(0xFF849D9A),
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        text = "89 ₽",
                        color = AppColors.TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // CTA Button + Terms + Link to plans
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ByMePrimaryButton(
                    text = "Продолжить за 1 ₽",
                    onClick = {
                        scope.launch {
                            Toast.makeText(context, "Инициализация безопасной оплаты пробного доступа 1 ₽...", Toast.LENGTH_SHORT).show()
                            val success = YooKassaPaymentHandler.startPayment(
                                context = context,
                                planCode = "trial_1rub",
                                planTitle = "Пробный доступ 3 дня",
                                amountRub = 1,
                                days = 3
                            )
                            if (!success) {
                                // If payment browser can't open, notify user clearly
                                Toast.makeText(context, "Не удалось открыть страницу оплаты. Попробуйте снова.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(13.dp))
                Text(
                    text = "Если способ оплаты будет сохранён, подписка продлится автоматически. Отключить можно в любой момент.",
                    color = Color(0xFF8DA6A1),
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier
                        .clickable(onClick = onChoosePlanClick)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = "Сразу выбрать тариф",
                        color = Color(0xFFB9DFD3),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    ChevronRightIcon(color = Color(0xFFB9DFD3), size = 16.dp)
                }
            }
        }
    }
}

@Composable
private fun BenefitRow(
    icon: @Composable () -> Unit,
    text: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        icon()
        Text(
            text = text,
            color = Color(0xFFE4EEEB),
            fontSize = 12.sp,
            modifier = Modifier.weight(1f)
        )
        CheckmarkIcon(color = Color(0xFF52D9AA), size = 17.dp)
    }
}
