package com.example.bymevpn.screens

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.bymevpn.components.ChatSupportIcon
import com.example.bymevpn.components.FaqDialog
import com.example.bymevpn.components.GradientBackground
import com.example.bymevpn.components.ShieldLogo
import com.example.bymevpn.components.TelegramLogoIcon
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
    val currentLanguage by LocaleManager.currentLanguage.collectAsState()
    val isRu = remember(currentLanguage) { LocaleManager.isRussian(language = currentLanguage) }
    val settingsRepo = remember { SettingsRepository.getInstance(context) }
    val settings by settingsRepo.settingsFlow.collectAsState(initial = AppSettings())
    val scope = rememberCoroutineScope()

    var showAppPicker by remember { mutableStateOf(false) }
    var showFaqDialog by remember { mutableStateOf(false) }

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

                // SECTION 1: Russian traffic bypass (Priority)
                SettingsSectionTitle(text = if (isRu) "Умная маршрутизация" else "Smart Routing")
                SettingsCard {
                    SettingsSwitchRow(
                        title = if (isRu) "Обход российских сайтов (РФ напрямую)" else "Bypass Russian websites (Direct RU)",
                        subtitle = if (isRu)
                            "Госуслуги, банки (Сбер, Т-Банк, ВТБ), Яндекс и сайты .ru работают напрямую без VPN на полной скорости, а зарубежные сайты — через VPN."
                        else
                            "Russian services, banks and .ru domains route directly at maximum provider speed; all other traffic goes through VPN.",
                        checked = settings.bypassRussianTraffic,
                        onCheckedChange = { scope.launch { settingsRepo.updateBypassRussianTraffic(it) } }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Warning notice
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF261D10))
                            .border(1.dp, Color(0xFFE5A118).copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Text("⚠️", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isRu)
                                    "Выключать не рекомендуется: российские банки и государственные сайты могут блокировать вход с зарубежных IP-адресов."
                                else
                                    "Disabling is not recommended: Russian banks and local services may restrict access from foreign IP addresses.",
                                color = Color(0xFFFFD580),
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // SECTION 2: General simple settings
                SettingsSectionTitle(text = if (isRu) "Основные параметры" else "General")
                SettingsCard {
                    // Auto connect on boot
                    SettingsSwitchRow(
                        title = if (isRu) "Автоподключение при старте" else "Auto-connect on boot",
                        subtitle = if (isRu) "Запускать защиту при включении телефона" else "Start VPN when device restarts",
                        checked = settings.autoConnectOnBoot,
                        onCheckedChange = { scope.launch { settingsRepo.updateAutoBoot(it) } }
                    )

                    SettingsDivider()

                    // Split tunneling / Exclude apps
                    SettingsSwitchRow(
                        title = if (isRu) "Исключить приложения" else "Exclude applications",
                        subtitle = if (isRu)
                            "Выбрать приложения, которые будут работать без VPN"
                        else
                            "Selected apps will bypass VPN tunnel",
                        checked = settings.splitTunnelingEnabled,
                        onCheckedChange = { scope.launch { settingsRepo.updateSplitTunneling(it) } }
                    )

                    if (settings.splitTunnelingEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF13223D))
                                .clickable { showAppPicker = true }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isRu)
                                    "Выбрано приложений: ${settings.excludedApps.size}"
                                else
                                    "Excluded apps: ${settings.excludedApps.size}",
                                color = Color(0xFF00D4FF),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (isRu) "Настроить →" else "Configure →",
                                color = Color(0xFF26E875),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // SECTION 3: Language
                SettingsSectionTitle(text = if (isRu) "Язык приложения" else "Language")
                SettingsCard {
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
                }

                Spacer(modifier = Modifier.height(20.dp))

                // SECTION 4: FAQ & Support (Help and Answers)
                SettingsSectionTitle(text = if (isRu) "Справка и поддержка" else "Help & Support")
                SettingsCard {
                    // FAQ
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showFaqDialog = true }
                            .padding(vertical = 4.dp),
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
                                Text("❓", fontSize = 17.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isRu) "Часто задаваемые вопросы (FAQ)" else "Frequently Asked Questions",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (isRu) "Ответы о банках, скорости и безопасности" else "Answers about speed, banks and security",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.5.sp
                                )
                            }
                        }
                        Text("→", color = Color(0xFF00D4FF), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    SettingsDivider()

                    // Telegram Support Bot
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/ByMeVPNSupportBot")).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Ignored
                                }
                            }
                            .padding(vertical = 4.dp),
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
                                ChatSupportIcon(size = 18.dp, color = Color(0xFF00D4FF))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isRu) "Поддержка в Telegram" else "Telegram Support Bot",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "@ByMeVPNSupportBot",
                                    color = Color(0xFF00D4FF),
                                    fontSize = 11.5.sp
                                )
                            }
                        }
                        Text("↗", color = Color(0xFF00D4FF), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    SettingsDivider()

                    // Telegram Official Channel
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/ByMeVPN")).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Ignored
                                }
                            }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2AABEE).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                TelegramLogoIcon(size = 18.dp, color = Color(0xFF2AABEE))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isRu) "Официальный Telegram канал" else "Official Telegram Channel",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "@ByMeVPN",
                                    color = Color(0xFF2AABEE),
                                    fontSize = 11.5.sp
                                )
                            }
                        }
                        Text("↗", color = Color(0xFF2AABEE), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // SECTION 5: About
                SettingsCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ShieldLogo(
                            size = 32.dp,
                            isActive = true,
                            glowAlpha = 0.9f
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(color = Color.White)) {
                                        append("ByMe")
                                    }
                                    withStyle(SpanStyle(color = Color(0xFF26E875))) {
                                        append("VPN")
                                    }
                                },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isRu) "Версия 1.0.0 • Защищенное соединение" else "Version 1.0.0 • Secure tunnel",
                                color = Color(0xFF64748B),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    if (showFaqDialog) {
        FaqDialog(onDismiss = { showFaqDialog = false })
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
    val currentLanguage by LocaleManager.currentLanguage.collectAsState()
    val isRu = remember(currentLanguage) { LocaleManager.isRussian(language = currentLanguage) }
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
