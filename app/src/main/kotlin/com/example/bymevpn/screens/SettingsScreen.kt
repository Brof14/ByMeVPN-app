package com.example.bymevpn.screens

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.bymevpn.components.GradientBackground
import com.example.bymevpn.data.AppLanguage
import com.example.bymevpn.data.LocaleManager
import com.example.bymevpn.data.settings.AppSettings
import com.example.bymevpn.data.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class InstalledAppItem(
    val packageName: String,
    val appName: String,
    val isSystem: Boolean = false
)

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isRu = LocaleManager.isRussian(context)
    val settingsRepo = remember { SettingsRepository.getInstance(context) }
    val settings by settingsRepo.settingsFlow.collectAsState(initial = AppSettings())
    val currentLanguage by LocaleManager.currentLanguage.collectAsState()
    val scope = rememberCoroutineScope()

    var showAppPicker by remember { mutableStateOf(false) }

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
                // Header
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
                        text = if (isRu) "Настройки" else "Settings",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // SECTION 1: VPN & Connection
                SettingsSectionTitle(text = if (isRu) "Соединение и протокол" else "Connection & Protocol")
                SettingsCard {
                    // Protocol selector (WireGuard locked)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isRu) "VPN Протокол" else "VPN Protocol",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "WireGuard (ChaCha20-Poly1305, Noise IK)",
                                color = Color(0xFF26E875),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF132B45))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text("WireGuard", color = Color(0xFF00D4FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    SettingsDivider()

                    // Auto connect on boot
                    SettingsSwitchRow(
                        title = if (isRu) "Автоподключение при старте" else "Auto-connect on device boot",
                        subtitle = if (isRu) "Запускать VPN при включении устройства" else "Start VPN when device restarts",
                        checked = settings.autoConnectOnBoot,
                        onCheckedChange = { scope.launch { settingsRepo.updateAutoBoot(it) } }
                    )

                    SettingsDivider()

                    // Auto connect on open Wi-Fi
                    SettingsSwitchRow(
                        title = if (isRu) "Защита в открытых Wi-Fi" else "Auto-connect on open Wi-Fi",
                        subtitle = if (isRu) "Автоматически шифровать небезопасные сети" else "Encrypt unsecured public networks",
                        checked = settings.autoConnectOpenWifi,
                        onCheckedChange = { scope.launch { settingsRepo.updateAutoWifi(it) } }
                    )

                    SettingsDivider()

                    // Auto reconnect on drop
                    SettingsSwitchRow(
                        title = if (isRu) "Автоматическое переподключение" else "Auto-reconnect",
                        subtitle = if (isRu) "Восстанавливать связь при смене сети" else "Restore tunnel on network drop",
                        checked = settings.autoReconnect,
                        onCheckedChange = { scope.launch { settingsRepo.updateAutoReconnect(it) } }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // SECTION 2: Security & Kill Switch
                SettingsSectionTitle(text = if (isRu) "Безопасность" else "Security")
                SettingsCard {
                    SettingsSwitchRow(
                        title = "Kill Switch",
                        subtitle = if (isRu)
                            "Блокировать весь трафик вне VPN при обрыве соединения"
                        else
                            "Block all unencrypted traffic if connection drops",
                        checked = settings.killSwitch,
                        onCheckedChange = { scope.launch { settingsRepo.updateKillSwitch(it) } }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // SECTION 3: Split Tunneling
                SettingsSectionTitle(text = if (isRu) "Раздельное туннелирование" else "Split Tunneling")
                SettingsCard {
                    SettingsSwitchRow(
                        title = if (isRu) "Исключить выбранные приложения" else "Exclude selected applications",
                        subtitle = if (isRu)
                            "Трафик выбранных приложений пойдет в обход VPN напрямую"
                        else
                            "Selected apps will bypass VPN and route directly",
                        checked = settings.splitTunnelingEnabled,
                        onCheckedChange = { scope.launch { settingsRepo.updateSplitTunneling(it) } }
                    )

                    if (settings.splitTunnelingEnabled) {
                        SettingsDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAppPicker = true }
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isRu) "Список исключенных приложений" else "Manage excluded apps",
                                    color = Color.White,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (isRu)
                                        "Исключено: ${settings.excludedApps.size} приложений"
                                    else
                                        "Excluded: ${settings.excludedApps.size} apps",
                                    color = Color(0xFF00D4FF),
                                    fontSize = 12.sp
                                )
                            }
                            Text("Выбрать →", color = Color(0xFF26E875), fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // SECTION 4: Interface & Language
                SettingsSectionTitle(text = if (isRu) "Интерфейс и уведомления" else "Interface & Notifications")
                SettingsCard {
                    // Language selector
                    Text(
                        text = if (isRu) "Язык интерфейса" else "Interface Language",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppLanguage.values().forEach { lang ->
                            val isSelected = currentLanguage == lang
                            val label = when (lang) {
                                AppLanguage.RUSSIAN -> if (isRu) "Русский" else "Russian"
                                AppLanguage.ENGLISH -> "English"
                                AppLanguage.SYSTEM -> if (isRu) "Как в системе" else "System"
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) Color(0xFF1B3864) else Color(0xFF0D172B))
                                    .border(
                                        1.dp,
                                        if (isSelected) Color(0xFF00D4FF) else Color(0xFF162542),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        LocaleManager.setLanguage(lang)
                                        scope.launch { settingsRepo.setLanguage(lang.name.lowercase()) }
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color(0xFF00D4FF) else Color(0xFF94A3B8),
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    SettingsDivider()

                    // Notifications toggle
                    SettingsSwitchRow(
                        title = if (isRu) "Уведомления о статусе" else "Connection notifications",
                        subtitle = if (isRu) "Отображать уведомление о подключении" else "Show persistent notification while active",
                        checked = settings.connectionNotifications,
                        onCheckedChange = { scope.launch { settingsRepo.updateNotifications(it) } }
                    )

                    SettingsDivider()

                    // Speed display in notification
                    SettingsSwitchRow(
                        title = if (isRu) "Скорость в уведомлении" else "Show speed in notification",
                        subtitle = if (isRu) "Отображать текущий трафик и скорость" else "Display upload/download rate",
                        checked = settings.showSpeedInNotification,
                        onCheckedChange = { scope.launch { settingsRepo.updateShowSpeed(it) } }
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    if (showAppPicker) {
        SplitTunnelingAppDialog(
            selectedPackages = settings.excludedApps,
            onDismiss = { showAppPicker = false },
            onSave = { updated ->
                scope.launch { settingsRepo.setExcludedApps(updated) }
                showAppPicker = false
            }
        )
    }
}

@Composable
private fun SettingsSectionTitle(text: String) {
    Text(
        text = text,
        color = Color(0xFF94A3B8),
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF0C162A))
            .border(1.dp, Color(0xFF1E355B), RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
private fun SettingsDivider() {
    Spacer(modifier = Modifier.height(12.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0xFF152542))
    )
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = Color(0xFF94A3B8),
                fontSize = 12.sp
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF060B17),
                checkedTrackColor = Color(0xFF26E875),
                uncheckedThumbColor = Color(0xFF94A3B8),
                uncheckedTrackColor = Color(0xFF1A2A47)
            )
        )
    }
}

@Composable
private fun SplitTunnelingAppDialog(
    selectedPackages: Set<String>,
    onDismiss: () -> Unit,
    onSave: (Set<String>) -> Unit
) {
    val context = LocalContext.current
    val isRu = LocaleManager.isRussian(context)
    var appList by remember { mutableStateOf<List<InstalledAppItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var currentSelection by remember { mutableStateOf(selectedPackages.toMutableSet()) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val list = installed
                .filter { it.packageName != context.packageName }
                .map { info ->
                    InstalledAppItem(
                        packageName = info.packageName,
                        appName = pm.getApplicationLabel(info).toString(),
                        isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    )
                }
                .sortedBy { it.appName.lowercase() }
            withContext(Dispatchers.Main) {
                appList = list
                isLoading = false
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0C162A))
                .border(1.dp, Color(0xFF1E355B), RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (isRu) "Исключенные приложения" else "Excluded Applications",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF26E875))
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().height(360.dp)) {
                        items(appList) { app ->
                            val isChecked = currentSelection.contains(app.packageName)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val copy = currentSelection.toMutableSet()
                                        if (isChecked) copy.remove(app.packageName) else copy.add(app.packageName)
                                        currentSelection = copy
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(text = app.appName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    Text(text = app.packageName, color = Color(0xFF64748B), fontSize = 11.5.sp)
                                }

                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        val copy = currentSelection.toMutableSet()
                                        if (checked) copy.add(app.packageName) else copy.remove(app.packageName)
                                        currentSelection = copy
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color(0xFF26E875),
                                        checkmarkColor = Color(0xFF060B17),
                                        uncheckedColor = Color(0xFF64748B)
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF162542))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (isRu) "Отмена" else "Cancel", color = Color(0xFF94A3B8), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF26E875))
                            .clickable { onSave(currentSelection) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (isRu) "Сохранить" else "Save", color = Color(0xFF060B17), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
