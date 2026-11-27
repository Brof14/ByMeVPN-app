package com.example.bymevpn.components

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bymevpn.data.LocaleManager
import com.example.bymevpn.data.api.ServerNode
import com.example.bymevpn.theme.AppColors
import com.example.bymevpn.theme.AppGradients
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerSelectionSheet(
    onDismissRequest: () -> Unit,
    servers: List<ServerNode>,
    selectedServer: ServerNode?,
    isAutoSelected: Boolean,
    isLoading: Boolean = false,
    error: String? = null,
    onRefresh: () -> Unit = {},
    onRefreshPing: () -> Unit = {},
    onSelectAuto: () -> Unit,
    onSelectServer: (ServerNode) -> Unit,
    modifier: Modifier = Modifier
) {
    val language by LocaleManager.currentLanguage.collectAsState()
    val isRu = LocaleManager.isRussian(language = language)
    val haptics = rememberByMeHaptics()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val dismissSheet: () -> Unit = {
        scope.launch {
            sheetState.hide()
            onDismissRequest()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = AppColors.NavBackground,
        scrimColor = Color(0xAA01050E),
        dragHandle = { BottomSheetDefaults.DragHandle(color = AppColors.BorderAccent) },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 24.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isRu) "Серверы" else "Servers",
                    color = AppColors.TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.8).sp,
                    modifier = Modifier.weight(1f)
                )

                SheetActionButton(
                    description = if (isRu) "Обновить серверы" else "Refresh servers",
                    onClick = {
                        haptics.tap()
                        onRefresh()
                    },
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = AppColors.SkyBlue,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        RefreshIcon(color = AppColors.SkyBlue, size = 17.dp)
                    }
                }
                SheetActionButton(
                    description = if (isRu) "Закрыть список серверов" else "Close server list",
                    onClick = { dismissSheet() },
                    icon = {
                        CloseXIcon(color = AppColors.TextSoft, size = 18.dp)
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.BgSurface)
                    .border(1.dp, AppColors.BorderDefault, RoundedCornerShape(12.dp))
                    .clickable(enabled = servers.none { it.pingMs == -3 || it.pingMs == -4 }) {
                        haptics.tap()
                        onRefreshPing()
                    }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .semantics {
                        contentDescription = if (isRu) "Проверить задержку серверов" else "Measure server latency"
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (servers.any { it.pingMs == -3 || it.pingMs == -4 }) {
                    CircularProgressIndicator(
                        color = AppColors.SkyBlue,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    RefreshIcon(color = AppColors.BrandMint, size = 16.dp)
                }
                Text(
                    text = if (isRu) "Проверить задержку" else "Measure latency",
                    color = AppColors.BrandMint,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isRu)
                    "Пинг измеряется напрямую до серверов без включения VPN."
                else
                    "Measures direct latency to each server without turning VPN on.",
                color = AppColors.TextMuted,
                fontSize = 11.5.sp,
                lineHeight = 16.sp
            )
            val checkingCount = servers.count { it.pingMs == -3 }
            val queuedCount = servers.count { it.pingMs == -4 }
            if (checkingCount + queuedCount > 0) {
                Text(
                    text = if (isRu) {
                        "Проверяем: $checkingCount · в очереди: $queuedCount"
                    } else {
                        "Checking: $checkingCount · queued: $queuedCount"
                    },
                    color = AppColors.TextSoft,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(AppGradients.ActivePlanCard)
                    .border(
                        width = if (isAutoSelected) 1.5.dp else 1.dp,
                        color = if (isAutoSelected) AppColors.BrandMint else AppColors.BorderAccent,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .selectable(selected = isAutoSelected, role = Role.RadioButton) {
                        haptics.selection()
                        onSelectAuto()
                        dismissSheet()
                    }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(AppColors.BgSurfaceElevated)
                        .border(1.dp, AppColors.BorderAccent, RoundedCornerShape(13.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    SparkleStarIcon(color = AppColors.BrandMintBright, size = 20.dp)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isRu) "Автоматически" else "Automatic",
                        color = AppColors.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isAutoSelected) {
                            selectedServer?.let { server ->
                                val name = server.displayName.ifBlank { server.name }
                                val ping = server.pingMs.takeIf { it > 0 }
                                if (ping != null) {
                                    if (isRu) "Выбран: $name · $ping мс" else "Selected: $name · $ping ms"
                                } else {
                                    if (isRu) "Выбран: $name" else "Selected: $name"
                                }
                            } ?: if (isRu) "Подбираем сервер…" else "Choosing a server…"
                        } else {
                            if (isRu) "Подберём оптимальный маршрут" else "Auto-pick fastest route"
                        },
                        color = AppColors.TextSecondary,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(if (isAutoSelected) AppColors.BrandMint else Color.Transparent)
                        .border(
                            1.2.dp,
                            if (isAutoSelected) AppColors.BrandMint else AppColors.TextMuted,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isAutoSelected) {
                        CheckmarkIcon(color = AppColors.BrandMintDark, size = 14.dp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (servers.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(AppColors.BgSurface)
                        .border(1.dp, AppColors.BorderDefault, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = if (isLoading) {
                            if (isRu) "Получаем список серверов…" else "Loading servers…"
                        } else {
                            if (isRu) "Серверы пока недоступны" else "No servers yet"
                        },
                        color = AppColors.TextPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = error?.takeIf { it.isNotBlank() }
                            ?: if (isRu) "Попробуйте обновить список серверов." else "Try refreshing the server list.",
                        color = AppColors.TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(servers, key = { it.id }) { server ->
                    val isSelected = !isAutoSelected && selectedServer?.id == server.id

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) AppColors.BgSurfaceElevated else AppColors.BgSurface)
                            .border(
                                if (isSelected) 1.5.dp else 1.dp,
                                if (isSelected) AppColors.BrandMint else AppColors.BorderDefault,
                                RoundedCornerShape(16.dp)
                            )
                            .selectable(selected = isSelected, role = Role.RadioButton) {
                                haptics.selection()
                                onSelectServer(server)
                                dismissSheet()
                            }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CountryFlagView(
                            countryCode = server.countryCode,
                            fallbackFlag = server.flag,
                            size = 26.dp
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (server.displayName.isNotBlank()) server.displayName else server.name,
                                color = AppColors.TextPrimary,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                val isReality = server.protocol.contains("Reality", ignoreCase = true)
                                val isHy2 = server.protocol.contains("Hysteria", ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(
                                            when {
                                                isReality -> Color(0x242EE9B8)
                                                isHy2 -> Color(0x240077FF)
                                                else -> Color(0x2400D8FF)
                                            }
                                        )
                                        .border(
                                            1.dp,
                                            when {
                                                isReality -> Color(0x552EE9B8)
                                                isHy2 -> Color(0x550077FF)
                                                else -> Color(0x5500D8FF)
                                            },
                                            RoundedCornerShape(5.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = server.protocolLabel,
                                        color = when {
                                            isReality -> AppColors.BrandMintBright
                                            isHy2 -> Color(0xFF7CB4FF)
                                            else -> AppColors.SkyBlue
                                        },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                val pingColor = when {
                                    server.connectionCheck < 0 -> Color(0xFFFF8A65)
                                    server.pingMs == -2 -> Color(0xFFFF8A65)
                                    server.pingMs == -5 -> Color(0xFFFF8A65)
                                    server.pingMs == -4 -> AppColors.TextMuted
                                    server.pingMs <= 0 -> AppColors.TextMuted
                                    server.pingMs <= 200 -> AppColors.BrandMintBright
                                    server.pingMs <= 350 -> AppColors.StatusWarning
                                    else -> Color(0xFFFF8A65)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(pingColor)
                                )
                                Text(
                                    text = when {
                                        server.connectionCheck < 0 -> if (isRu) "Не удалось подключиться" else "Connection failed"
                                        server.pingMs == -2 -> if (isRu) "Нет ответа" else "No response"
                                        server.pingMs == -5 -> if (isRu) "Не удалось измерить" else "Could not measure"
                                        server.pingMs > 0 -> "${server.pingMs} ${if (isRu) "мс" else "ms"}"
                                        server.pingMs == -3 -> if (isRu) "Проверяем…" else "Checking…"
                                        server.pingMs == -4 -> if (isRu) "В очереди" else "Queued"
                                        else -> if (isRu) "Не проверен" else "Not tested"
                                    },
                                    color = pingColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(21.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) AppColors.BrandMint else Color.Transparent)
                                .border(
                                    1.2.dp,
                                    if (isSelected) AppColors.BrandMint else AppColors.TextMuted,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                CheckmarkIcon(color = AppColors.BrandMintDark, size = 13.dp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = if (isRu) "Показываются только серверы вашей подписки." else "Only servers in your subscription are shown.",
                color = AppColors.TextMuted,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SheetActionButton(
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    icon: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .semantics { contentDescription = description }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AppColors.TopBarIconBg)
                .border(1.dp, AppColors.TopBarIconBorder, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
    }
}
