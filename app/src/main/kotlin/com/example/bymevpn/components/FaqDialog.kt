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
        "VPN отключается, когда экран заблокирован",
        "VPN drops when the screen is locked",
        "Android и оболочки производителей ограничивают фоновую работу. Откройте «Настройки» → «Защита в фоне» и разрешите приложению работать без ограничений батареи; на Xiaomi, Huawei и Samsung дополнительно отключите оптимизацию в системных настройках батареи.",
        "Android and vendor skins limit background work. Open Settings → Background protection and allow the app to run without battery restrictions; on Xiaomi, Huawei and Samsung also disable battery optimisation in system settings.",
        ""
    ),
    FaqItem(
        "Сайт не открывается, хотя VPN включён",
        "A site won't open while the VPN is on",
        "Сначала проверьте, открывается ли сайт без VPN. Затем смените сервер или протокол: Hysteria 2 лучше на нестабильных сетях, VLESS Reality — на сетях с блокировками. Если не работает только банк или госуслуги, включите «Российские сайты напрямую» в настройках.",
        "First check whether the site opens without the VPN. Then switch the server or protocol: Hysteria 2 copes better with unstable networks, VLESS Reality with filtering. If only a bank or local service fails, enable “Russian sites directly” in Settings.",
        ""
    ),
    FaqItem(
        "Почему пинг у сервера иногда «Нет ответа»?",
        "Why does server latency sometimes show “No response”?",
        "Пинг — это проверка доступности узла по TCP. Провайдер или сеть могут блокировать такие проверки, а Hysteria 2 работает по UDP и по TCP не проверяется. Поэтому ориентируйтесь на фактическое подключение: если VPN запускается и трафик идёт, сервер доступен.",
        "Latency is a TCP reachability probe. Carriers may block such probes, and Hysteria 2 runs over UDP, so TCP cannot test it. Judge by the real connection: if the VPN starts and traffic flows, the server is reachable.",
        ""
    ),
    FaqItem(
        "Что такое лимит устройств?",
        "What is the device limit?",
        "Сервер считает устройства по установленному приложению, а не по IP-адресу. В разделе «Мои устройства» видно, какие устройства подтвердил сервер. Если телефон не появился в списке, значит сервер ещё не зарегистрировал его, а не то, что место закончилось.",
        "The server counts devices by app installation, not by IP address. “My devices” shows what the server has confirmed. If your phone is missing there, the server has not registered it yet — it does not mean the limit is full.",
        ""
    ),
    FaqItem(
        "Как перенести подписку на новый телефон",
        "How do I move my subscription to a new phone?",
        "Войдите тем же способом (Telegram или почта) на новом телефоне. Старое устройство при необходимости удалите в разделе «Мои устройства», чтобы освободить место в лимите тарифа.",
        "Sign in the same way (Telegram or email) on the new phone. If needed, remove the old device in “My devices” to free a slot in your plan's limit.",
        ""
    ),
    FaqItem(
        "Как проверить или отключить автопродление",
        "How to check or disable automatic renewal",
        "Откройте «Аккаунт» → «Настройки аккаунта». Там показан подтверждённый сервером статус. Если стоит «нет данных», приложение не выдумывает значение: управление автопродлением ещё не подключено к серверу.",
        "Open Account → Account settings. The server-confirmed status is shown there. If it says “no data”, the app does not invent a value: renewal management is not connected to the server yet.",
        ""
    ),
    FaqItem(
        "Вход через Telegram не завершается",
        "Telegram sign-in does not complete",
        "После перехода в бота нажмите «Старт» и вернитесь в приложение. Экран ожидания сам проверит подтверждение; кнопка «Открыть Telegram» поможет, если бот не открылся с первого раза. Если ожидание истекло, начните вход заново — старая ссылка больше не действует.",
        "After opening the bot, tap Start and return to the app. The waiting screen checks confirmation automatically; “Open Telegram” helps if the bot did not open the first time. If the wait expires, start again — the old link is no longer valid.",
        ""
    )
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
