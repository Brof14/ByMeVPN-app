package com.example.bymevpn.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bymevpn.components.BottomNavTab
import com.example.bymevpn.components.ByMeBottomNav
import com.example.bymevpn.components.ByMeCardGroup
import com.example.bymevpn.components.ByMeSettingRow
import com.example.bymevpn.components.ByMeToggle
import com.example.bymevpn.components.ByMeTopBar
import com.example.bymevpn.components.CheckmarkIcon
import com.example.bymevpn.components.CloseXIcon
import com.example.bymevpn.components.RefreshIcon
import com.example.bymevpn.components.SplitTunnelIcon
import com.example.bymevpn.components.rememberByMeHaptics
import com.example.bymevpn.data.LocaleManager
import com.example.bymevpn.data.settings.AppSettings
import com.example.bymevpn.data.settings.SettingsRepository
import com.example.bymevpn.theme.AppColors
import com.example.bymevpn.theme.AppGradients
import com.example.bymevpn.vpn.VpnConnectionState
import com.example.bymevpn.vpn.VpnManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class InstalledAppItem(
    val packageName: String,
    val label: String
)

@Composable
fun SplitTunnelingScreen(
    onBackClick: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settingsRepo = remember { SettingsRepository.getInstance(context) }
    val vpnManager = remember { VpnManager.getInstance(context) }
    val settings by settingsRepo.settingsFlow.collectAsState(initial = AppSettings())
    val vpnState by vpnManager.state.collectAsState()
    val language by LocaleManager.currentLanguage.collectAsState()
    val isRu = LocaleManager.isRussian(language = language)
    val scope = rememberCoroutineScope()
    val haptics = rememberByMeHaptics()

    var installedApps by remember { mutableStateOf<List<InstalledAppItem>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var hasPendingTunnelChange by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLoadingApps = true
        installedApps = loadLauncherApps(context)
        isLoadingApps = false
    }

    val filteredApps = remember(installedApps, searchQuery, settings.excludedApps) {
        val query = searchQuery.trim()
        installedApps
            .asSequence()
            .filter { app ->
                query.isEmpty() ||
                    app.label.contains(query, ignoreCase = true) ||
                    app.packageName.contains(query, ignoreCase = true)
            }
            .sortedWith(
                compareByDescending<InstalledAppItem> { it.packageName in settings.excludedApps }
                    .thenBy { it.label.lowercase() }
            )
            .toList()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.BgCanvas)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ByMeTopBar(
                title = if (isRu) "Раздельное туннелирование" else "Split tunneling",
                onBackClick = onBackClick,
                onMoreClick = null
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp)
            ) {
                ByMeCardGroup {
                    ByMeSettingRow(
                        title = if (isRu) "Исключения для приложений" else "App bypass",
                        subtitle = if (isRu) {
                            "Отмеченные приложения идут напрямую без VPN"
                        } else {
                            "Selected apps connect directly without VPN"
                        },
                        icon = { SplitTunnelIcon(color = AppColors.SkyBlue, size = 20.dp) },
                        trailing = {
                            ByMeToggle(
                                checked = settings.splitTunnelingEnabled,
                                onCheckedChange = { enabled ->
                                    haptics.selection()
                                    if (vpnState == VpnConnectionState.CONNECTED) {
                                        hasPendingTunnelChange = true
                                    }
                                    scope.launch { settingsRepo.updateSplitTunneling(enabled) }
                                }
                            )
                        },
                        showBottomDivider = false
                    )
                }

                if (hasPendingTunnelChange && vpnState == VpnConnectionState.CONNECTED) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(AppGradients.ActivePlanCard)
                            .border(1.dp, AppColors.BorderAccent, RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (isRu) {
                                "Переподключите VPN, чтобы применить список"
                            } else {
                                "Reconnect VPN to apply updated app rules"
                            },
                            color = AppColors.TextSoft,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(AppGradients.PrimaryButton)
                                .clickable(role = Role.Button) {
                                    haptics.tap()
                                    hasPendingTunnelChange = false
                                    vpnManager.reconnect()
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            RefreshIcon(color = AppColors.BrandMintDark, size = 14.dp)
                            Text(
                                text = if (isRu) "Обновить" else "Reconnect",
                                color = AppColors.BrandMintDark,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isRu) {
                            "Исключено приложений: ${settings.excludedApps.size}"
                        } else {
                            "Bypassed apps: ${settings.excludedApps.size}"
                        },
                        color = AppColors.TextSecondary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (settings.excludedApps.isNotEmpty()) {
                        Text(
                            text = if (isRu) "Сбросить" else "Reset",
                            color = AppColors.SkyBlue,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(role = Role.Button) {
                                    haptics.tap()
                                    if (vpnState == VpnConnectionState.CONNECTED) {
                                        hasPendingTunnelChange = true
                                    }
                                    scope.launch { settingsRepo.setExcludedApps(emptySet()) }
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(AppColors.BgInput)
                        .border(1.dp, AppColors.BorderInput, RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = if (isRu) "Поиск приложения…" else "Search apps…",
                                color = AppColors.TextMuted,
                                fontSize = 13.5.sp
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = AppColors.TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = SolidColor(AppColors.SkyBlue),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .clickable(role = Role.Button) { searchQuery = "" },
                            contentAlignment = Alignment.Center
                        ) {
                            CloseXIcon(color = AppColors.TextSecondary, size = 14.dp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (isLoadingApps) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AppColors.SkyBlue, modifier = Modifier.size(28.dp))
                    }
                } else if (filteredApps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isRu) "Приложения не найдены" else "No matching apps found",
                            color = AppColors.TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredApps, key = { it.packageName }) { app ->
                            val isExcluded = app.packageName in settings.excludedApps
                            SplitTunnelAppRow(
                                app = app,
                                isExcluded = isExcluded,
                                onToggle = {
                                    haptics.selection()
                                    if (vpnState == VpnConnectionState.CONNECTED) {
                                        hasPendingTunnelChange = true
                                    }
                                    val updated = if (isExcluded) {
                                        settings.excludedApps - app.packageName
                                    } else {
                                        settings.excludedApps + app.packageName
                                    }
                                    scope.launch {
                                        settingsRepo.setExcludedApps(updated)
                                        if (!isExcluded && !settings.splitTunnelingEnabled) {
                                            settingsRepo.updateSplitTunneling(true)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            ByMeBottomNav(
                currentTab = BottomNavTab.Settings,
                onTabSelected = { tab ->
                    when (tab) {
                        BottomNavTab.Home -> onNavigateToHome()
                        BottomNavTab.Settings -> onNavigateToSettings()
                        BottomNavTab.Account -> onNavigateToAccount()
                    }
                }
            )
        }
    }
}

@Composable
private fun SplitTunnelAppRow(
    app: InstalledAppItem,
    isExcluded: Boolean,
    onToggle: () -> Unit
) {
    val context = LocalContext.current
    val iconBitmap by produceState<ImageBitmap?>(initialValue = null, key1 = app.packageName) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val drawable = context.packageManager.getApplicationIcon(app.packageName)
                drawableToBitmap(drawable).asImageBitmap()
            }.getOrNull()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(if (isExcluded) AppColors.BgSurfaceElevated else AppColors.BgSurface)
            .border(
                width = 1.dp,
                color = if (isExcluded) AppColors.BrandMint else AppColors.BorderDefault,
                shape = RoundedCornerShape(15.dp)
            )
            .clickable(role = Role.Checkbox, onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val bmp = iconBitmap
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = app.label,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AppColors.BgSurfaceAccent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = app.label.take(1).uppercase(),
                    color = AppColors.SkyBlue,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.label,
                color = AppColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = app.packageName,
                color = AppColors.TextMuted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(if (isExcluded) AppColors.BrandMint else Color.Transparent)
                .border(
                    width = 1.4.dp,
                    color = if (isExcluded) AppColors.BrandMint else AppColors.TextMuted,
                    shape = RoundedCornerShape(7.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isExcluded) {
                CheckmarkIcon(color = AppColors.BrandMintDark, size = 14.dp)
            }
        }
    }
}

private suspend fun loadLauncherApps(context: Context): List<InstalledAppItem> =
    withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val activities = runCatching { pm.queryIntentActivities(intent, 0) }.getOrDefault(emptyList())
        val selfPkg = context.packageName
        activities
            .asSequence()
            .mapNotNull { info ->
                val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                if (pkg == selfPkg) return@mapNotNull null
                val label = runCatching { info.loadLabel(pm).toString() }.getOrNull()
                    ?.takeIf { it.isNotBlank() } ?: pkg
                InstalledAppItem(packageName = pkg, label = label)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

private fun drawableToBitmap(drawable: Drawable): Bitmap {
    if (drawable is BitmapDrawable && drawable.bitmap != null) {
        return drawable.bitmap
    }
    val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 96
    val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 96
    val bitmap = Bitmap.createBitmap(width.coerceAtMost(144), height.coerceAtMost(144), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}
