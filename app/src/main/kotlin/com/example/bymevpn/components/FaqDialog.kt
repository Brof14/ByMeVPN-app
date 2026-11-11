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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.window.DialogProperties
import com.example.bymevpn.data.LocaleManager
import com.example.bymevpn.theme.AppColors

data class FaqItem(
    val questionRu: String,
    val questionEn: String,
    val answerRu: String,
    val answerEn: String,
    val icon: String = "💡"
)

val FAQ_ITEMS = listOf(
    FaqItem(
        "Не удаётся подключиться",
        "I can't connect",
        "Выберите другой сервер и попробуйте ещё раз. Если ошибка повторяется, переключитесь между Wi‑Fi и мобильной сетью и отправьте код ошибки в поддержку.",
        "Choose another server and try again. If it still fails, switch between Wi‑Fi and mobile data, then send the error code to support.",
        ""
    ),
    FaqItem(
        "VPN отключается при блокировке экрана",
        "VPN disconnects when the screen locks",
        "Разрешите ByMeVPN работу без ограничений батареи в настройках телефона. На Xiaomi также включите автозапуск и закрепите приложение в недавних.",
        "Allow ByMeVPN to run without battery restrictions. On Xiaomi, also enable autostart and lock the app in Recents.",
        ""
    ),
    FaqItem(
        "Достигнут лимит устройств",
        "I've reached the device limit",
        "Удалите старое устройство в разделе «Устройства» или выберите тариф на 5 либо 10 устройств прямо там.",
        "Remove an old device in Devices, or choose a plan for 5 or 10 devices there.",
        ""
    ),
    FaqItem(
        "Как включить пробный период?",
        "How do I start the free trial?",
        "Нажмите «Активировать 3 дня бесплатно» на главном экране. Пробный период доступен один раз на аккаунт. Если появится ошибка, повторите попытку позже и сообщите поддержке код ошибки.",
        "Tap “Activate 3 days free” on Home. The trial is available once per account. If an error appears, try again later and send the error code to support.",
        ""
    ),
    FaqItem(
        "Не получается войти",
        "I can't sign in",
        "Войдите тем же способом, которым регистрировались. Для старого аккаунта из Telegram-бота выберите на экране входа вариант для прежних аккаунтов.",
        "Use the same sign-in method you registered with. For an older Telegram-bot account, choose the legacy account option on the sign-in screen.",
        ""
    )
)

@Composable
fun FaqDialog(onDismiss: () -> Unit) {
    val currentLanguage by LocaleManager.currentLanguage.collectAsState()
    val isRu = remember(currentLanguage) { LocaleManager.isRussian(language = currentLanguage) }
    var expandedIndex by remember { mutableStateOf<Int?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 460.dp)
                .fillMaxHeight(0.84f)
                .clip(RoundedCornerShape(24.dp))
                .background(AppColors.BgSurface)
                .border(1.2.dp, AppColors.BorderDefault, RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
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
                                .background(AppColors.BadgeGreenBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("?", color = AppColors.BrandMint, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isRu) "Частые вопросы" else "FAQ",
                                color = AppColors.TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isRu) "Короткие ответы по делу" else "Quick answers",
                                color = AppColors.TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(AppColors.BgSurfaceAccent)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✕", color = AppColors.TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(FAQ_ITEMS) { index, item ->
                        val isExpanded = expandedIndex == index
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isExpanded) AppColors.BgSurfaceElevated else AppColors.BgSurfaceAccent)
                                .border(
                                    1.dp,
                                    if (isExpanded) AppColors.BrandMint else AppColors.BorderDefault,
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
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(if (isExpanded) AppColors.BadgeGreenBg else AppColors.BgSurfaceAccent),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = (index + 1).toString().padStart(2, '0'),
                                            color = AppColors.BrandMint,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = if (isRu) item.questionRu else item.questionEn,
                                        color = AppColors.TextPrimary,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(AppColors.BgSurfaceAccent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isExpanded) "−" else "+",
                                        color = AppColors.BrandMint,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
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
                                            .background(AppColors.BorderDefault)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = if (isRu) item.answerRu else item.answerEn,
                                        color = AppColors.TextSecondary,
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
                        .background(AppColors.BgSurfaceAccent)
                        .clickable { onDismiss() }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isRu) "Понятно" else "Close",
                        color = AppColors.BrandMint,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
