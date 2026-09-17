package com.example.bymevpn.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.bymevpn.theme.AppColors

/**
 * Three-dot indicator bar where the active step is rendered as an elongated bright pill.
 */
@Composable
fun PageIndicator(
    total: Int = 3,
    active: Int = 1,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .testTag("page_indicator")
            .semantics { contentDescription = "Page $active of $total" },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until total) {
            val isActive = i == active
            val dotWidth by animateDpAsState(
                targetValue = if (isActive) 20.dp else 6.dp,
                animationSpec = tween(300),
                label = "dot_width"
            )
            val dotColor by animateColorAsState(
                targetValue = if (isActive) AppColors.White else AppColors.SloganGrey.copy(alpha = 0.45f),
                animationSpec = tween(300),
                label = "dot_color"
            )

            Box(
                modifier = Modifier
                    .padding(horizontal = 3.5.dp)
                    .width(dotWidth)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(dotColor)
            )
        }
    }
}
