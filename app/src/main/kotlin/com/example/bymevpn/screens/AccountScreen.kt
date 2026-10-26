package com.example.bymevpn.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.bymevpn.components.AccountIcon
import com.example.bymevpn.components.GoogleLogoIcon
import com.example.bymevpn.components.TelegramLogoIcon
import com.example.bymevpn.data.AccountRepository
import com.example.bymevpn.data.LocaleManager
import com.example.bymevpn.data.subscription.SubscriptionManager
import com.example.bymevpn.theme.AppColors
import com.example.bymevpn.vpn.VpnManager
import kotlinx.coroutines.launch

@Composable
fun AccountScreen(
    onBackClick: () -> Unit = {},
    onLogOut: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentLanguage by LocaleManager.currentLanguage.collectAsState()
    val isRu = remember(currentLanguage) { LocaleManager.isRussian(language = currentLanguage) }
    val scope = rememberCoroutineScope()

    val currentUser by AccountRepository.currentUser.collectAsState()
    val userDevices by AccountRepository.userDevices.collectAsState()
    val subManager = remember { SubscriptionManager.getInstance(context) }
    val subscription by subManager.subscriptionStatus.collectAsState()

    val totalDevices = userDevices.size.coerceAtLeast(1)
    val maxDevices = subscription?.maxDevices ?: 5

    var showTariffModal by remember { mutableStateOf(false) }
    var showEditSubDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var deviceToDelete by remember { mutableStateOf<com.example.bymevpn.data.api.DeviceItem?>(null) }

    LaunchedEffect(Unit) {
        subManager.refreshStatus()
        AccountRepository.refreshDevices(context)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AppColors.BgCanvas
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // 1. Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(AppColors.CardBg)
                        .border(1.dp, AppColors.CardBorder, CircleShape)
                        .clickable { onBackClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("←", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = if (isRu) "Аккаунт" else "Account",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. User Profile Card (Figma Screen 07)
            val isSubActive = subscription?.status == "active" || subscription?.status == "trial"
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(AppColors.CardBg)
                    .border(1.2.dp, AppColors.CardBorder, RoundedCornerShape(22.dp))
                .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(AppColors.CardBgElevated)
                        .border(1.5.dp, if (isSubActive) AppColors.BrandGreen else AppColors.CardBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    TelegramLogoIcon(size = 36.dp, color = AppColors.TelegramBlue)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = currentUser?.name?.ifBlank { "ByMe User" } ?: "ByMe User",
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Active Status Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSubActive) AppColors.BrandGreen.copy(alpha = 0.12f) else AppColors.StatusError.copy(alpha = 0.12f))
                        .border(
                            1.dp,
                            if (isSubActive) AppColors.BrandGreen.copy(alpha = 0.4f) else AppColors.StatusError.copy(alpha = 0.4f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isSubActive) AppColors.BrandGreen else AppColors.StatusError)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSubActive) (if (isRu) "Активна" else "Active") else (if (isRu) "Не активна" else "Inactive"),
                        color = if (isSubActive) AppColors.BrandGreen else AppColors.StatusError,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Current Subscription Card (Figma Screen 07)
            val secondsRemaining = subscription?.secondsRemaining ?: (29L * 86400L)
            val daysRemaining = (secondsRemaining / 86400).coerceAtLeast(0)
            val totalPeriodDays = 30f
            val progressFraction = (daysRemaining / totalPeriodDays).coerceIn(0f, 1f)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(AppColors.CardBg)
                    .border(1.2.dp, AppColors.CardBorder, RoundedCornerShape(22.dp))
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isRu) "Текущий тариф" else "Current Plan",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(AppColors.CardBgElevated)
                            .border(1.dp, AppColors.BrandGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = subscription?.planName ?: if (isRu) "Стандартный безлимит" else "Standard Unlimited",
                            color = AppColors.BrandGreen,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Circular Progress
                    Box(modifier = Modifier.size(76.dp), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawArc(
                                color = Color(0xFF142026),
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = 7.dp.toPx())
                            )
                            drawArc(
                                color = if (isSubActive) AppColors.BrandGreen else AppColors.StatusError,
                                startAngle = -90f,
                                sweepAngle = 360f * (if (isSubActive) progressFraction else 0f),
                                useCenter = false,
                                style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$daysRemaining",
                                color = Color.White,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = if (isRu) "дн." else "days",
                                color = AppColors.TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(18.dp))

                    val expiresText = if (isSubActive) {
                        if (!subscription?.expiresAt.isNullOrBlank()) {
                            if (isRu) "Действует до ${subscription?.expiresAt}" else "Valid until ${subscription?.expiresAt}"
                        } else {
                            if (isRu) "Действует ещё $daysRemaining дн." else "Valid for $daysRemaining days"
                        }
                    } else {
                        if (isRu) "Подписка истекла" else "Subscription expired"
                    }

                    Column {
                        Text(
                            text = expiresText,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isRu) "$totalDevices из $maxDevices устройств занято" else "$totalDevices of $maxDevices devices active",
                            color = AppColors.TextSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            repeat(maxDevices) { index ->
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (index < totalDevices) AppColors.BrandGreen else AppColors.CardBorder)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Primary Action Button: "Продлить или изменить тариф" (Opens Figma Screen 08 Tariffs Modal)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(AppColors.BrandGreen, AppColors.BrandGreenGlow)
                            )
                        )
                        .clickable { showTariffModal = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isRu) "⚡ Продлить или изменить тариф" else "⚡ Renew or Change Plan",
                        color = AppColors.DarkText,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 1-tap Copy or Share Subscription for other devices (PC, Android TV, Mac, iOS)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppColors.InputBg)
                            .border(1.dp, AppColors.CardBorder, RoundedCornerShape(12.dp))
                            .clickable {
                                val url = com.example.bymevpn.data.storage.SecureTokenStorage.getInstance(context).getSubscriptionUrl()
                                if (!url.isNullOrBlank()) {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("ByMeVPN Key", url)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, if (isRu) "Ключ подписки скопирован!" else "Subscription key copied!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, if (isRu) "Нет активного ключа" else "No active key", Toast.LENGTH_SHORT).show()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📋", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isRu) "Скопировать ключ" else "Copy Key",
                                color = Color.White,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppColors.InputBg)
                            .border(1.dp, AppColors.CardBorder, RoundedCornerShape(12.dp))
                            .clickable {
                                val url = com.example.bymevpn.data.storage.SecureTokenStorage.getInstance(context).getSubscriptionUrl() ?: "https://t.me/ByMeVPNBot"
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "ByMeVPN")
                                    putExtra(Intent.EXTRA_TEXT, "ByMeVPN — Скорость. Анонимность. Честность.\nКлюч подключения: $url")
                                }
                                context.startActivity(Intent.createChooser(shareIntent, if (isRu) "Поделиться ключом ByMeVPN" else "Share ByMeVPN Key"))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("↗", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isRu) "Поделиться (ПК / ТВ)" else "Share (PC / TV)",
                                color = AppColors.SkyBlue,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Secondary Link: Manage 3x-ui link directly
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppColors.InputBg)
                        .border(1.dp, AppColors.CardBorder, RoundedCornerShape(12.dp))
                        .clickable { showEditSubDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isRu) "Вставить ссылку из Telegram-бота" else "Paste Link from Telegram Bot",
                        color = AppColors.SkyBlue,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Connected Devices Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(AppColors.CardBg)
                    .border(1.2.dp, AppColors.CardBorder, RoundedCornerShape(22.dp))
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isRu) "Подключенные устройства" else "Connected Devices",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(AppColors.CardBgElevated)
                            .border(1.dp, AppColors.BrandGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "$totalDevices / $maxDevices",
                            color = AppColors.BrandGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // List of connected devices
                userDevices.forEach { dev ->
                    val isCurrent = dev.isCurrent || dev.id == com.example.bymevpn.data.storage.SecureTokenStorage.getInstance(context).getInstallId(context)
                    val devIcon = when {
                        dev.platform.contains("Windows", ignoreCase = true) || dev.deviceModel.contains("PC", ignoreCase = true) -> "💻"
                        dev.platform.contains("TV", ignoreCase = true) || dev.deviceModel.contains("TV", ignoreCase = true) -> "📺"
                        dev.platform.contains("iOS", ignoreCase = true) || dev.deviceModel.contains("iPhone", ignoreCase = true) -> "🍏"
                        dev.platform.contains("Mac", ignoreCase = true) -> "🖥️"
                        else -> "📱"
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(AppColors.InputBg)
                            .border(1.dp, if (isCurrent) AppColors.BrandGreen.copy(alpha = 0.45f) else AppColors.CardBorder, RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Text(devIcon, fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = dev.deviceModel,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (isCurrent) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isRu) "(это устройство)" else "(this device)",
                                            color = AppColors.BrandGreen,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                Text(
                                    text = if (isCurrent) (if (isRu) "${dev.platform} • Активно сейчас" else "${dev.platform} • Active now") else "${dev.platform} • ${dev.lastActive}",
                                    color = AppColors.TextSecondary,
                                    fontSize = 11.5.sp
                                )
                            }
                        }

                        if (isCurrent) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(AppColors.BrandGreen)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AppColors.CardBgElevated)
                                    .border(1.dp, AppColors.StatusError.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .clickable { deviceToDelete = dev }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isRu) "Отключить" else "Disconnect",
                                    color = AppColors.StatusError,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Add device instructions card
                Spacer(modifier = Modifier.height(4.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0C161C))
                        .border(1.dp, AppColors.CardBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = if (isRu) "ℹ️ Как подключить ПК, ТВ или другое устройство:" else "ℹ️ How to connect PC, TV or other devices:",
                        color = Color.White,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isRu)
                            "1. Скопируйте ключ или ссылку подписки выше.\n2. Вставьте в ByMeVPN (или v2rayNG / Hiddify).\n3. Устройство мгновенно подключится к вашей подписке."
                        else
                            "1. Copy your key or share subscription link above.\n2. Paste into ByMeVPN (or v2rayNG / Hiddify).\n3. Connected device will appear here instantly.",
                        color = AppColors.TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5. Log Out & Danger Zone
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(AppColors.CardBg)
                    .border(1.dp, AppColors.CardBorder, RoundedCornerShape(14.dp))
                    .clickable {
                        scope.launch {
                            AccountRepository.logout(context)
                            onLogOut()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isRu) "Выйти из аккаунта" else "Log Out",
                    color = AppColors.TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Paywall Modal with YooKassa and Trial
    if (showTariffModal) {
        com.example.bymevpn.components.PaywallSheet(
            onDismissRequest = { showTariffModal = false },
            onSubscriptionActivated = {
                showTariffModal = false
                scope.launch {
                    subManager.refreshStatus()
                    AccountRepository.refreshDevices(context)
                }
            }
        )
    }

    // Confirmation dialog for removing a device
    if (deviceToDelete != null) {
        val dev = deviceToDelete!!
        Dialog(onDismissRequest = { deviceToDelete = null }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(AppColors.CardBg)
                    .border(1.2.dp, AppColors.StatusError, RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isRu) "Отключить устройство?" else "Disconnect device?",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isRu)
                            "Устройство «${dev.deviceModel}» будет отключено, и слот освободится для нового подключения."
                        else
                            "Device «${dev.deviceModel}» will be disconnected and its slot freed.",
                        color = AppColors.TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AppColors.InputBg)
                                .clickable { deviceToDelete = null },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (isRu) "Отмена" else "Cancel", color = Color.White, fontSize = 13.5.sp)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AppColors.StatusError)
                                .clickable {
                                    scope.launch {
                                        AccountRepository.removeDevice(context, dev.id)
                                        deviceToDelete = null
                                        Toast.makeText(context, if (isRu) "Устройство отключено, слот освобожден" else "Device disconnected", Toast.LENGTH_SHORT).show()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (isRu) "Отключить" else "Disconnect", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Modal: Edit 3x-ui Link
    if (showEditSubDialog) {
        var editSubUrl by remember {
            mutableStateOf(com.example.bymevpn.data.storage.SecureTokenStorage.getInstance(context).getSubscriptionUrl() ?: "")
        }
        var isSyncingSub by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { showEditSubDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(AppColors.CardBg)
                    .border(1.2.dp, AppColors.CardBorder, RoundedCornerShape(22.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = if (isRu) "Ссылка подписки" else "Subscription Link",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isRu) "Вставьте ссылку подписки или ключ из Telegram-бота:" else "Paste subscription link or key from Telegram bot:",
                        color = AppColors.TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    BasicTextField(
                        value = editSubUrl,
                        onValueChange = { editSubUrl = it },
                        textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                        cursorBrush = SolidColor(AppColors.BrandGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppColors.InputBg)
                            .border(1.dp, AppColors.InputBorder, RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isRu) "Отмена" else "Cancel",
                            color = AppColors.TextSecondary,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .clickable { showEditSubDialog = false }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(AppColors.BrandGreen)
                                .clickable(enabled = !isSyncingSub) {
                                    isSyncingSub = true
                                    scope.launch {
                                        val res = subManager.applySubscriptionUrl(editSubUrl.trim())
                                        isSyncingSub = false
                                        if (res.isSuccess) {
                                            VpnManager.getInstance(context).loadServers()
                                            showEditSubDialog = false
                                        } else {
                                            Toast.makeText(context, res.exceptionOrNull()?.message ?: "Ошибка", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = if (isRu) "Сохранить" else "Save",
                                color = AppColors.DarkText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TariffItemCard(
    title: String,
    price: String,
    subprice: String,
    badge: String?,
    isHighlight: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isHighlight) AppColors.CardBgElevated else AppColors.InputBg)
            .border(
                1.2.dp,
                if (isHighlight) AppColors.BrandGreen else AppColors.CardBorder,
                RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    if (badge != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AppColors.BrandGreen.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(text = badge, color = AppColors.BrandGreen, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subprice, color = AppColors.TextSecondary, fontSize = 12.sp)
            }

            Text(text = price, color = if (isHighlight) AppColors.BrandGreen else Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

private fun openTelegramBot(context: Context, param: String?) {
    val url = if (param != null) "https://t.me/ByMeVPNBot?$param" else "https://t.me/ByMeVPNSupportBot"
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "Не удалось открыть Telegram", Toast.LENGTH_SHORT).show()
    }
}
