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
    FaqItem("VPN включён, но сайт не открывается", "VPN is on, but a site won't open", "Попробуйте другой сервер. Если проблема остаётся, проверьте интернет без VPN и обратитесь в поддержку, указав название сервера.", "Try another server. If the problem remains, check your connection without VPN and contact support with the server name.", ""),
    FaqItem("Что означает задержка сервера?", "What does server latency mean?", "Задержка показывает время ответа в миллисекундах. Небольшое значение обычно лучше, но само по себе не гарантирует работу VPN.", "Latency is response time in milliseconds. Lower is usually better, but it does not guarantee VPN connectivity.", ""),
    FaqItem("Почему устройство не подтверждено?", "Why isn't my device confirmed?", "Список устройств предоставляет сервер. Если он недоступен, телефон показан отдельно; это не подтверждает его регистрацию или свободное место в тарифе.", "The server provides the device list. If unavailable, this phone is shown separately; that does not confirm registration or an available slot.", ""),
    FaqItem("Как узнать, включено ли автопродление?", "How can I check automatic renewal?", "Откройте аккаунт и его настройки. Если данных нет, статус не подтверждён; уточните его в поддержке.", "Open Account settings. If no data is shown, the status is unconfirmed; ask support.", ""),
    FaqItem("Как вернуть доступ через Telegram?", "How do I recover access through Telegram?", "На экране входа выберите Telegram и подтвердите вход в боте. Вернитесь в приложение и дождитесь проверки.", "Choose Telegram on the sign-in screen and confirm in the bot. Return to the app and wait for verification.", "")
)

@Composable
fun FaqDialog(onDismiss: () -> Unit) {
    val currentLanguage by LocaleManager.currentLanguage.collectAsState()
    val isRu = remember(currentLanguage) { LocaleManager.isRussian(language = currentLanguage) }
    var expandedIndex by remember { mutableStateOf<Int?>(null) } // First one open by default

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF101E1B))
                .border(1.2.dp, Color(0xFF28443C), RoundedCornerShape(24.dp))
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
                                .background(Color(0xFF62CBB0).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("?", color = Color(0xFF62CBB0), fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
                                color = Color(0xFF92A9A5),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF172A29))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✕", color = Color(0xFF92A9A5), fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
                                .background(if (isExpanded) Color(0xFF18332B) else Color(0xFF12221F))
                                .border(
                                    1.dp,
                                    if (isExpanded) Color(0xFF62CBB0) else Color(0xFF28443C),
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
                                    Text(
                                        text = if (isRu) item.questionRu else item.questionEn,
                                        color = Color.White,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    text = if (isExpanded) "▲" else "▼",
                                    color = Color(0xFF62CBB0),
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
                                            .background(Color(0xFF28443C))
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
                        .background(Color(0xFF172A29))
                        .clickable { onDismiss() }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isRu) "Понятно" else "Close",
                        color = Color(0xFF62CBB0),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
