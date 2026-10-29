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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bymevpn.data.api.ServerNode
import com.example.bymevpn.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerSelectionSheet(
    onDismissRequest: () -> Unit,
    servers: List<ServerNode>,
    selectedServer: ServerNode?,
    isAutoSelected: Boolean,
    onSelectAuto: () -> Unit,
    onSelectServer: (ServerNode) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color(0xFF10191B),
        scrimColor = Color(0x99000000),
        dragHandle = null,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 25.dp)
                .padding(top = 12.dp, bottom = 24.dp)
                .navigationBarsPadding()
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(43.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFF4B6361))
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Sheet head inline: Title + Close round button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Серверы",
                    color = AppColors.TextPrimary,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1.15).sp
                )

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1B2A2B))
                        .clickable(onClick = onDismissRequest),
                    contentAlignment = Alignment.Center
                ) {
                    CloseXIcon(color = Color(0xFF93ADAB), size = 18.dp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Выберите страну или доверьтесь автоматике.",
                color = Color(0xFF889EA0),
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Recommended card: "Автоматически"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF17322A))
                    .border(1.dp, Color(0xFF2C8265), RoundedCornerShape(18.dp))
                    .clickable {
                        onSelectAuto()
                        onDismissRequest()
                    }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF234E3F)),
                    contentAlignment = Alignment.Center
                ) {
                    SparkleStarIcon(color = Color(0xFF37EDB2), size = 20.dp)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Автоматически",
                        color = AppColors.TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = "Выберем лучшее доступное соединение",
                        color = Color(0xFF89A6A0),
                        fontSize = 11.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(if (isAutoSelected) Color(0xFF3CDFAC) else Color.Transparent)
                        .border(
                            1.dp,
                            if (isAutoSelected) Color(0xFF3CDFAC) else Color(0xFF57716F),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isAutoSelected) {
                        CheckmarkIcon(color = Color(0xFF03231C), size = 14.dp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "ДОСТУПНЫЕ СЕРВЕРЫ",
                color = AppColors.TextKicker,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Server rows
            val displayServers = if (servers.isNotEmpty()) {
                servers
            } else {
                listOf(
                    ServerNode(nodeCode = "de-1", country = "Германия", countryCode = "DE", city = "Узел 1", flag = "🇩🇪", pingMs = 28, protocol = "VLESS Reality", displayName = "Германия #1 • VLESS Reality"),
                    ServerNode(nodeCode = "de-2", country = "Германия", countryCode = "DE", city = "Узел 2", flag = "🇩🇪", pingMs = 35, protocol = "VLESS Reality", displayName = "Германия #2 • VLESS Reality"),
                    ServerNode(nodeCode = "de-3", country = "Германия", countryCode = "DE", city = "Узел 3", flag = "🇩🇪", pingMs = 42, protocol = "Hysteria 2", displayName = "Германия #3 • Hysteria 2"),
                    ServerNode(nodeCode = "nl-1", country = "Нидерланды", countryCode = "NL", city = "Узел 4", flag = "🇳🇱", pingMs = 32, protocol = "VLESS Reality", displayName = "Нидерланды #1 • VLESS Reality")
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(displayServers) { server ->
                    val isSelected = !isAutoSelected && selectedServer?.id == server.id

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(76.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF101D1F))
                            .border(
                                1.dp,
                                if (isSelected) Color(0xFF39DAA7) else Color(0xFF253639),
                                RoundedCornerShape(18.dp)
                            )
                            .clickable {
                                onSelectServer(server)
                                onDismissRequest()
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
                                        text = server.protocol,
                                        color = if (server.protocol.contains("Reality", ignoreCase = true)) Color(0xFF3CDFAC)
                                                else if (server.protocol.contains("Hysteria", ignoreCase = true)) Color(0xFFC4B0FF)
                                                else Color(0xFF78C6FF),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Text(
                                    text = if (server.pingMs > 0) "${server.pingMs} ms" else "Быстрый",
                                    color = Color(0xFF89A6A0),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(21.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color(0xFF39DAA7) else Color.Transparent)
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFF39DAA7) else Color(0xFF435D5A),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                CheckmarkIcon(color = Color(0xFF03231C), size = 13.dp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Отображаются только серверы, доступные по вашей подписке.",
                color = Color(0xFF688985),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
