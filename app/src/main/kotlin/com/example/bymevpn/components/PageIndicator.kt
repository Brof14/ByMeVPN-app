package com.example.bymevpn.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * 4 circular page indicator dots matching the screenshot 1:1.
 * Dot 2 (index 1) is active with white glow.
 */
@Composable
fun PageIndicator(
    total: Int = 4,
    active: Int = 1,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .testTag("page_indicator")
            .semantics { contentDescription = "Page ${active + 1} of $total" },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until total) {
            val isActive = i == active
            val dotSize = if (isActive) 7.5.dp else 5.5.dp
            val dotColor = if (isActive) Color.White else Color(0xFF19263E)

            Box(
                modifier = Modifier
                    .padding(horizontal = 4.5.dp)
                    .size(dotSize)
                    .then(
                        if (isActive) {
                            Modifier.shadow(
                                elevation = 4.dp,
                                shape = CircleShape,
                                ambientColor = Color.White.copy(alpha = 0.5f),
                                spotColor = Color.White.copy(alpha = 0.5f)
                            )
                        } else Modifier
                    )
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}
