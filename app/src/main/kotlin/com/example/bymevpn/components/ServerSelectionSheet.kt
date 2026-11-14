package com.example.bymevpn.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bymevpn.data.api.ServerNode
import com.example.bymevpn.theme.AppColors
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
    onSelectAuto: () -> Unit,
    onSelectServer: (ServerNode) -> Unit,
    modifier: Modifier = Modifier
) {
    val language by com.example.bymevpn.data.LocaleManager.currentLanguage.collectAsState()
    val isRu = com.example.bymevpn.data.LocaleManager.isRussian(language = language)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val dismissSheet = {
        scope.launch {
            sheetState.hide()
            onDismissRequest()
        }
    }
    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.LaunchedEffect(Unit) {
        com.example.bymevpn.vpn.VpnManager.getInstance(context).refreshAllPings()
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color(0xFF101B2A),
        scrimColor = Color(0x99000000),
        dragHandle = { BottomSheetDefaults.DragHandle(color = AppColors.TextMuted) },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 25.dp)
                .padding(top = 12.dp, bottom = 24.dp)
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
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1.15).sp,
                    modifier = Modifier.weight(1f)
                )

                SheetActionButton(
                    description = if (isRu) "Обновить серверы" else "Refresh servers",
                    onClick = onRefresh,
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        androidx.compose.material3.CircularProgressIndicator(
                            color = AppColors.BrandMint,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        RefreshIcon(color = Color(0xFF90DDCE), size = 17.dp)
                    }
                }
                SheetActionButton(
                    description = if (isRu) "Закрыть список серверов" else "Close server list",
                    onClick = dismissSheet
                ) {
                    CloseXIcon(color = Color(0xFF93ADAB), size = 18.dp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isRu) "Выберите страну или доверьтесь автоматике." else "Choose a country or use automatic selection.",
                color = Color(0xFF889EA0),
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Recommended card: if (isRu) "Автоматически" else "Automatic"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF182D3B))
                    .border(1.dp, Color(0xFF437D90), RoundedCornerShape(18.dp))
                    .selectable(selected = isAutoSelected, role = Role.RadioButton) {
                        onSelectAuto()
                        dismissSheet()
                    }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF2C4D5D)),
                    contentAlignment = Alignment.Center
                ) {
                    SparkleStarIcon(color = Color(0xFF69D3BC), size = 20.dp)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isRu) "Автоматически" else "Automatic",
                        color = AppColors.TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = if (isRu) "Подберём сервер" else "We will choose a server",
                        color = Color(0xFF89A6A0),
                        fontSize = 11.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(if (isAutoSelected) Color(0xFF69D3BC) else Color.Transparent)
                        .border(
                            1.dp,
                            if (isAutoSelected) Color(0xFF69D3BC) else Color(0xFF57716F),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isAutoSelected) {
                        CheckmarkIcon(color = Color(0xFF0A2231), size = 14.dp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (isRu) "Доступные серверы" else "Available servers",
                color = AppColors.TextKicker,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Server rows
            val displayServers = servers
            if (displayServers.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF142235))
                        .border(1.dp, Color(0xFF294453), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = if (isLoading) {
                            if (isRu) "Получаем список серверов…" else "Loading servers…"
                        } else {
                            if (isRu) "Серверы пока недоступны" else "No servers yet"
                        },
                        color = AppColors.TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = error?.takeIf { it.isNotBlank() }
                            ?: if (isRu) "Попробуйте обновить список серверов." else "Try refreshing the server list.",
                        color = Color(0xFF889EA0),
                        fontSize = 11.5.sp
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(displayServers, key = { it.id }) { server ->
                    val isSelected = !isAutoSelected && selectedServer?.id == server.id

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(76.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF111D2D))
                            .border(
                                1.dp,
                                if (isSelected) Color(0xFF69D3BC) else Color(0xFF293E4F),
                                RoundedCornerShape(18.dp)
                            )
                            .selectable(selected = isSelected, role = Role.RadioButton) {
                                onSelectServer(server)
                                dismissSheet()
                            }
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(13.dp)
                    ) {
                        CountryFlagView(
                            countryCode = server.countryCode,
                            size = 26.dp
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (server.displayName.isNotBlank()) server.displayName else server.name,
                                color = AppColors.TextPrimary,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(
                                            if (server.protocol.contains("Reality", ignoreCase = true)) Color(0x2B3CDFAC)
                                            else if (server.protocol.contains("Hysteria", ignoreCase = true)) Color(0x2B9D7BFF)
                                            else Color(0x2B4DAFFF)
                                        )
                                        .border(
                                            1.dp,
                                            if (server.protocol.contains("Reality", ignoreCase = true)) Color(0x593CDFAC)
                                            else if (server.protocol.contains("Hysteria", ignoreCase = true)) Color(0x599D7BFF)
                                            else Color(0x594DAFFF),
                                            RoundedCornerShape(5.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = server.protocolLabel,
                                        color = if (server.protocol.contains("Reality", ignoreCase = true)) Color(0xFF69D3BC)
                                                else if (server.protocol.contains("Hysteria", ignoreCase = true)) Color(0xFFC4B0FF)
                                                else Color(0xFF78C6FF),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                val pingColor = when {
                                    server.connectionCheck < 0 -> Color(0xFFFF8A65)
                                    server.pingMs == -2 -> Color(0xFFFF8A65)
                                    server.pingMs <= 0 -> Color(0xFF89A6A0)
                                    server.pingMs <= 200 -> Color(0xFF69D3BC)
                                    server.pingMs <= 350 -> Color(0xFFFFD54F)
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
                                        server.pingMs > 0 -> "${server.pingMs} ${if (isRu) "мс" else "ms"}"
                                        else -> if (isRu) "Не проверен" else "Not tested"
                                    },
                                    color = pingColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(21.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color(0xFF69D3BC) else Color.Transparent)
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFF69D3BC) else Color(0xFF435D5A),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                CheckmarkIcon(color = Color(0xFF0A2231), size = 13.dp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = if (isRu) "Отображаются только серверы, доступные по вашей подписке." else "Only servers in your subscription are shown.",
                color = Color(0xFF688985),
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
                .background(Color(0xFF1B2A2B)),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
    }
}
