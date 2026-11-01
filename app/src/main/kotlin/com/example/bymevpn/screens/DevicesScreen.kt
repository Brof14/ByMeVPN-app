package com.example.bymevpn.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bymevpn.components.BottomNavTab
import com.example.bymevpn.components.ByMeBottomNav
import com.example.bymevpn.components.ByMeTopBar
import com.example.bymevpn.components.SmartphoneIcon
import com.example.bymevpn.data.AccountRepository
import kotlinx.coroutines.launch
import android.widget.Toast
import com.example.bymevpn.data.LocaleManager
import com.example.bymevpn.data.subscription.SubscriptionManager
import com.example.bymevpn.theme.AppColors

internal fun visibleDeviceCount(devices: List<com.example.bymevpn.data.api.DeviceItem>): Int =
    devices.count { !it.isCurrent } + 1

@Composable
fun DevicesScreen(
    onBackClick: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val subManager = remember { SubscriptionManager.getInstance(context) }
    val subscription by subManager.subscriptionStatus.collectAsState()
    val devicesList by AccountRepository.userDevices.collectAsState()
    val language by LocaleManager.currentLanguage.collectAsState()
    val isRu = LocaleManager.isRussian(language = language)
    val scope = rememberCoroutineScope()
    var pendingRemoveId by remember { mutableStateOf<String?>(null) }
    var isRemoving by remember { mutableStateOf(false) }
    val otherDevices = devicesList.filterNot { it.isCurrent }

    LaunchedEffect(Unit) {
        AccountRepository.refreshDevices(context)
        subManager.refreshStatus(forceBackend = true)
    }

    val effectiveMax = subscription?.maxDevices?.takeIf { it > 0 }
    val effectiveConnected = visibleDeviceCount(devicesList)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.BgCanvas)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ByMeTopBar(
                title = if (isRu) "Мои устройства" else "My devices",
                onBackClick = onBackClick,
                onMoreClick = null
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 26.dp, vertical = 12.dp)
            ) {
                // Large device count display matching mockup 15_devices
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$effectiveConnected",
                        color = AppColors.TextPrimary,
                        fontSize = 60.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-3).sp
                    )
                    Text(
                        text = effectiveMax?.let { if (isRu) " из $it" else " of $it" } ?: "",
                        color = Color(0xFF80D3C6),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 10.dp, start = 4.dp)
                    )
                }

                Text(
                    text = if (effectiveMax == null) {
                        if (otherDevices.isEmpty()) {
                            if (isRu) "Пока отображается только это устройство." else "Only this device is shown for now."
                        } else {
                            if (isRu) "Сейчас отображается $effectiveConnected устройств." else "$effectiveConnected devices are shown now."
                        }
                    } else if (isRu) "Подключено устройств · максимум $effectiveMax" else "Devices connected · up to $effectiveMax",
                    color = Color(0xFF92A9A5),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )

                if (effectiveMax != null) {
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { (effectiveConnected.toFloat() / effectiveMax).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = AppColors.BrandMint,
                        trackColor = AppColors.CardBorder
                    )
                    Spacer(Modifier.height(20.dp))
                }

                val localModel = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}".trim()
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                val localOs = "Android ${android.os.Build.VERSION.RELEASE}"

                // The phone in your hand is always shown with its OS and model, even when
                // the backend has not confirmed it in the device list yet.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(17.dp))
                        .background(Color(0xFF142638))
                        .border(1.dp, Color(0xFF294453), RoundedCornerShape(17.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(37.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(Color(0xFF182A3B)),
                        contentAlignment = Alignment.Center
                    ) {
                        SmartphoneIcon(color = Color(0xFF7AC9CB), size = 21.dp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = localModel,
                            color = Color(0xFFEAF1EF),
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$localOs · " + (if (isRu) "это устройство" else "this device"),
                            color = Color(0xFF88A09F),
                            fontSize = 11.5.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(0xFF69D3BC))
                            )
                            Text(
                                text = if (isRu) "Это устройство" else "This device",
                                color = Color(0xFF8CDECA),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                if (otherDevices.isNotEmpty()) {
                    Text(
                        text = if (isRu) "ДРУГИЕ УСТРОЙСТВА" else "OTHER DEVICES",
                        color = Color(0xFF8BA3A4),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.padding(bottom = 10.dp, start = 2.dp)
                    )
                }

                if (otherDevices.isNotEmpty()) {
                    otherDevices.forEach { dev ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(89.dp)
                                .clip(RoundedCornerShape(17.dp))
                                .background(Color(0xFF142638))
                                .border(1.dp, Color(0xFF294453), RoundedCornerShape(17.dp))
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(37.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(Color(0xFF182A3B)),
                                contentAlignment = Alignment.Center
                            ) {
                                SmartphoneIcon(color = Color(0xFF7AC9CB), size = 21.dp)
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = dev.deviceModel,
                                    color = Color(0xFFEAF1EF),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${dev.platform} · ${dev.lastActive}",
                                    color = Color(0xFF88A09F),
                                    fontSize = 11.5.sp
                                )
                            }
                            Text(
                                text = if (isRu) "Удалить" else "Remove",
                                color = Color(0xFFFF8A8A),
                                fontSize = 11.sp,
                                modifier = Modifier.clickable { pendingRemoveId = dev.id }.padding(6.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                } else {
                    Text(
                        text = if (isRu) "Пока отображается только это устройство." else "Only this device is shown for now.",
                        color = Color(0xFF8AA1A1),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Info Box "ДОБАВИТЬ УСТРОЙСТВО"
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(17.dp))
                        .background(Color(0xFF142538))
                        .border(1.dp, Color(0xFF294455), RoundedCornerShape(17.dp))
                        .padding(18.dp)
                ) {
                    Text(
                        text = if (isRu) "ДОБАВИТЬ УСТРОЙСТВО" else "ADD A DEVICE",
                        color = Color(0xFF8FBBCA),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isRu) "Войдите в ByMeVPN на другом устройстве с тем же аккаунтом. Оно появится здесь после регистрации на сервере."
                            else "Sign in to ByMeVPN on another device with the same account. It will appear here once registered with the server.",
                        color = Color(0xFF9DB6AC),
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            ByMeBottomNav(
                currentTab = BottomNavTab.Account,
                onTabSelected = { tab ->
                    when (tab) {
                        BottomNavTab.Home -> onNavigateToHome()
                        BottomNavTab.Settings -> onNavigateToSettings()
                        BottomNavTab.Account -> onNavigateToAccount()
                    }
                }
            )
        }
        val targetId = pendingRemoveId
        if (targetId != null) {
            AlertDialog(
                onDismissRequest = { if (!isRemoving) pendingRemoveId = null },
                title = { Text(if (isRu) "Удалить устройство?" else "Remove device?") },
                text = { Text(if (isRu) "Устройство потеряет доступ к VPN. Продолжить?" else "This device will lose VPN access. Continue?") },
                confirmButton = {
                    TextButton(enabled = !isRemoving, onClick = {
                        isRemoving = true
                        scope.launch {
                            val result = AccountRepository.removeDevice(context, targetId)
                            isRemoving = false
                            if (result.isSuccess) pendingRemoveId = null
                            else Toast.makeText(context, if (isRu) "Не удалось удалить устройство" else "Could not remove device", Toast.LENGTH_LONG).show()
                        }
                    }) { Text(if (isRu) "Удалить" else "Remove", color = Color(0xFFFF7373)) }
                },
                dismissButton = {
                    TextButton(enabled = !isRemoving, onClick = { pendingRemoveId = null }) { Text(if (isRu) "Отмена" else "Cancel") }
                }
            )
        }
    }
}
