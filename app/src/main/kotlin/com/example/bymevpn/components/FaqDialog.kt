package com.example.bymevpn.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.bymevpn.data.LocaleManager

data class FaqItem(
    val questionRu: String,
    val questionEn: String,
    val answerRu: String,
    val answerEn: String,
    val icon: String = "💡"
)

val FAQ_ITEMS = listOf(
    FaqItem(
        questionRu = "Что делать, если VPN не подключается?",
        questionEn = "What to do if VPN won't connect?",
        answerRu = "1. Проверьте подключение к Wi-Fi или мобильной сети.\n2. Попробуйте сменить сервер (переключитесь между Нидерландами и Германией).\n3. Убедитесь, что выдано системное разрешение на подключение VPN.\n4. Если соединение всё ещё не устанавливается, напишите в нашу поддержку @ByMeVPNSupportBot — мы поможем в течение нескольких минут.",
        answerEn = "1. Check your Wi-Fi or mobile network connection.\n2. Try switching between Netherlands and Germany servers.\n3. Make sure the Android VPN permission is granted.\n4. If it still doesn't connect, tap the chat button in the top right to reach @ByMeVPNSupportBot.",
        icon = "🛠️"
    ),
    FaqItem(
        questionRu = "Будут ли работать Госуслуги, банки и доставка?",
        questionEn = "Will local banks and gov services work?",
        answerRu = "В настройках можно включить обход российских сайтов. Маршрут определяется базой доменов и IP; отдельные сервисы могут работать иначе.",
        answerEn = "You can enable bypass for Russian sites in settings. Routing uses a domain and IP database; individual services may behave differently.",
        icon = "🏦"
    ),
    FaqItem(
        questionRu = "Как оформить или продлить подписку?",
        questionEn = "How do I subscribe or renew?",
        answerRu = "Откройте раздел «Аккаунт» и выберите тариф. Подписка активируется после подтверждения оплаты сервером.",
        answerEn = "Open Account and choose a plan. The subscription activates after the server confirms payment.",
        icon = "💎"
    ),
    FaqItem(
        questionRu = "Безопасен ли ByMeVPN и сохраняются ли логи?",
        questionEn = "Is ByMeVPN secure and are logs kept?",
        answerRu = "Приложение использует шифрованное соединение с VPN-сервером. Данные о правилах хранения на сервере должны быть описаны в политике конфиденциальности сервиса.",
        answerEn = "The app uses an encrypted connection to the VPN server. Server-side data handling should be described in the service privacy policy.",
        icon = "🛡️"
    )
)

@Composable
fun FaqDialog(onDismiss: () -> Unit) {
    val currentLanguage by LocaleManager.currentLanguage.collectAsState()
    val isRu = remember(currentLanguage) { LocaleManager.isRussian(language = currentLanguage) }
    var expandedIndex by remember { mutableStateOf<Int?>(0) } // First one open by default

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0C162A))
                .border(1.2.dp, Color(0xFF1E355B), RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00D4FF).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("❓", fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isRu) "Частые вопросы" else "FAQ",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isRu) "Ответы и подсказки" else "Help & Answers",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF162542))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✕", color = Color(0xFF94A3B8), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(FAQ_ITEMS) { index, item ->
                        val isExpanded = expandedIndex == index
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isExpanded) Color(0xFF13223D) else Color(0xFF0A1222))
                                .border(
                                    1.dp,
                                    if (isExpanded) Color(0xFF00D4FF) else Color(0xFF162746),
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    expandedIndex = if (isExpanded) null else index
                                }
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(item.icon, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isRu) item.questionRu else item.questionEn,
                                        color = Color.White,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    text = if (isExpanded) "▲" else "▼",
                                    color = Color(0xFF00D4FF),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(Color(0xFF1E355B))
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = if (isRu) item.answerRu else item.answerEn,
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 12.5.sp,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF162542))
                        .clickable { onDismiss() }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isRu) "Понятно" else "Close",
                        color = Color(0xFF00D4FF),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
