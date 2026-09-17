package com.example.bymevpn.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.bymevpn.components.AccountIcon
import com.example.bymevpn.components.GoogleLogoIcon
import com.example.bymevpn.components.GradientBackground
import com.example.bymevpn.data.AccountRepository
import com.example.bymevpn.data.LocaleManager
import com.example.bymevpn.data.api.DeviceItem
import com.example.bymevpn.data.subscription.SubscriptionManager
import kotlinx.coroutines.launch

@Composable
fun AccountScreen(
    onBackClick: () -> Unit = {},
    onLogOut: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isRu = LocaleManager.isRussian(context)
    val scope = rememberCoroutineScope()

    val currentUser by AccountRepository.currentUser.collectAsState()
    val userDevices by AccountRepository.userDevices.collectAsState()
    val subManager = remember { SubscriptionManager.getInstance(context) }
    val subscription by subManager.subscriptionStatus.collectAsState()
    val isSyncing by subManager.isSyncing.collectAsState()

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showTrialErrorDialog by remember { mutableStateOf<String?>(null) }
    var isDeleting by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        subManager.refreshStatus()
        AccountRepository.refreshDevices(context)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent
    ) { innerPadding ->
        GradientBackground(modifier = Modifier.padding(innerPadding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                // Top App Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F1D36))
                            .border(1.dp, Color(0xFF1E355B), CircleShape)
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

                Spacer(modifier = Modifier.height(24.dp))

                // User Info Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF0C162A))
                        .border(1.dp, Color(0xFF1E355B), RoundedCornerShape(20.dp))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF162A4E)),
                        contentAlignment = Alignment.Center
                    ) {
                        AccountIcon(color = Color(0xFF26E875), size = 36.dp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = currentUser?.name?.ifBlank { "ByMe User" } ?: "ByMe User",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = currentUser?.email ?: "account@bymevpn.com",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.5.sp
                    )

                    if (currentUser?.isGoogle == true) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF13223D))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            GoogleLogoIcon(size = 14.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isRu) "Google ID подключен" else "Google ID Connected",
                                color = Color(0xFFCBD5E1),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Subscription & Circular Progress Card
                val status = subscription?.status ?: "none"
                val planName = subscription?.planName ?: (if (isRu) "Базовый доступ" else "Free Account")
                val secondsRemaining = subscription?.secondsRemaining ?: 0L
                val daysRemaining = (secondsRemaining / 86400).coerceAtLeast(0)
                val totalPeriodDays = 30f
                val progressFraction = (daysRemaining / totalPeriodDays).coerceIn(0f, 1f)

                // Status Badge
                val badgeColor = when (status) {
                    "active" -> Color(0xFF26E875)
                    "trial" -> Color(0xFF00D4FF)
                    "grace" -> Color(0xFFFFB74D)
                    else -> Color(0xFFFF5252)
                }
                val badgeText = when (status) {
                    "active" -> if (isRu) "АКТИВНА" else "ACTIVE"
                    "trial" -> if (isRu) "ПРОБНЫЙ" else "TRIAL"
                    "grace" -> if (isRu) "ЛЬГОТНЫЙ" else "GRACE"
                    else -> if (isRu) "НЕТ ПОДПИСКИ" else "INACTIVE"
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF0C162A))
                        .border(1.dp, Color(0xFF1E355B), RoundedCornerShape(20.dp))
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isRu) "Подписка" else "Subscription",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(badgeColor.copy(alpha = 0.15f))
                                .border(1.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(text = badgeText, color = badgeColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Circular gauge
                        Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawArc(
                                    color = Color(0xFF162542),
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = 8.dp.toPx())
                                )
                                drawArc(
                                    color = badgeColor,
                                    startAngle = -90f,
                                    sweepAngle = 360f * (if (status == "none" || status == "expired") 0f else progressFraction),
                                    useCenter = false,
                                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$daysRemaining",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = if (isRu) "дн." else "days",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(18.dp))

                        Column {
                            Text(
                                text = planName,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (subscription?.expiresAt != null) {
                                    if (isRu) "Действует до: ${subscription?.expiresAt}" else "Valid until: ${subscription?.expiresAt}"
                                } else {
                                    if (isRu) "Нет активного тарифа" else "No active plan"
                                },
                                color = Color(0xFF94A3B8),
                                fontSize = 12.5.sp
                            )
                            Text(
                                text = if (isRu)
                                    "Устройств: ${subscription?.activeDevices ?: 1} из ${subscription?.maxDevices ?: 5}"
                                else
                                    "Devices: ${subscription?.activeDevices ?: 1} of ${subscription?.maxDevices ?: 5}",
                                color = Color(0xFF00D4FF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Buttons: "Try 3 days free" or "Renew in Web"
                    if (subscription?.trialAvailable == true && (status == "none" || status == "expired")) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF26E875))
                                .clickable {
                                    scope.launch {
                                        val result = subManager.activateTrial()
                                        if (result.isSuccess) {
                                            Toast.makeText(context, if (isRu) "Пробный период 3 дня активирован!" else "3-day trial activated!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            showTrialErrorDialog = result.exceptionOrNull()?.message ?: "Trial activation error"
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isRu) "🎁 Попробовать 3 дня бесплатно" else "🎁 Try 3 days free",
                                color = Color(0xFF060B17),
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Open Web portal via Chrome Custom Tabs
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF142442))
                            .border(1.dp, Color(0xFF203B6B), RoundedCornerShape(12.dp))
                            .clickable {
                                openCustomTab(context, "https://bymevpn-site.duckdns.org/billing")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isRu) "Управление подпиской на сайте ↗" else "Manage subscription on web ↗",
                            color = Color(0xFF00D4FF),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Devices List Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF0C162A))
                        .border(1.dp, Color(0xFF1E355B), RoundedCornerShape(20.dp))
                        .padding(20.dp)
                ) {
                    Text(
                        text = if (isRu) "Подключенные устройства" else "Connected Devices",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (userDevices.isEmpty()) {
                        Text(
                            text = if (isRu) "Нет данных об устройствах" else "No devices registered yet",
                            color = Color(0xFF64748B),
                            fontSize = 13.sp
                        )
                    } else {
                        userDevices.forEach { device ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF091222))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = device.deviceModel,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (device.isCurrent) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isRu) "(это устройство)" else "(this device)",
                                                color = Color(0xFF26E875),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${device.platform} • ${device.lastActive}",
                                        color = Color(0xFF64748B),
                                        fontSize = 11.5.sp
                                    )
                                }

                                if (!device.isCurrent) {
                                    Text(
                                        text = if (isRu) "Удалить" else "Remove",
                                        color = Color(0xFFFF5252),
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable {
                                                scope.launch {
                                                    AccountRepository.removeDevice(context, device.id)
                                                }
                                            }
                                            .padding(6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Log Out Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF14223A))
                        .border(1.dp, Color(0xFF1E355B), RoundedCornerShape(14.dp))
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
                        color = Color(0xFF94A3B8),
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Delete Account (2-step confirm)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF261017))
                        .border(1.dp, Color(0xFF5A1C29), RoundedCornerShape(14.dp))
                        .clickable { showDeleteConfirmDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isRu) "Удалить аккаунт навсегда" else "Delete Account Permanently",
                        color = Color(0xFFFF5252),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Delete Account Confirmation Dialog
    if (showDeleteConfirmDialog) {
        Dialog(onDismissRequest = { if (!isDeleting) showDeleteConfirmDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.2.dp, Color(0xFF5A1C29), RoundedCornerShape(22.dp))
                    .padding(22.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isRu) "Удаление аккаунта" else "Delete Account",
                        color = Color(0xFFFF5252),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (isRu)
                            "Вы уверены, что хотите удалить аккаунт ByMeVPN? Все подписки, сессии и история будут стёрты без возможности восстановления."
                        else
                            "Are you sure you want to permanently delete your ByMeVPN account? All subscriptions and sessions will be deleted.",
                        color = Color(0xFFCBD5E1),
                        fontSize = 13.5.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    if (isDeleting) {
                        CircularProgressIndicator(color = Color(0xFFFF5252))
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1E293B))
                                    .clickable { showDeleteConfirmDialog = false },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(if (isRu) "Отмена" else "Cancel", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFDC2626))
                                    .clickable {
                                        isDeleting = true
                                        scope.launch {
                                            val res = AccountRepository.deleteAccount(context)
                                            isDeleting = false
                                            showDeleteConfirmDialog = false
                                            if (res.isSuccess) {
                                                onLogOut()
                                            } else {
                                                Toast.makeText(context, res.exceptionOrNull()?.message ?: "Error deleting account", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(if (isRu) "Да, удалить" else "Confirm", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Trial error dialog
    showTrialErrorDialog?.let { err ->
        Dialog(onDismissRequest = { showTrialErrorDialog = null }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0C162A))
                    .padding(20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = if (isRu) "Пробный период" else "Trial Activation", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = err, color = Color(0xFFCBD5E1), fontSize = 13.5.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1E355B))
                            .clickable { showTrialErrorDialog = null },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "OK", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun openCustomTab(context: Context, url: String) {
    try {
        val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
        customTabsIntent.launchUrl(context, Uri.parse(url))
    } catch (e: Exception) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }
}
