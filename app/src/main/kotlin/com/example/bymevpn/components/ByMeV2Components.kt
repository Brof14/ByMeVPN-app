package com.example.bymevpn.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bymevpn.theme.AppColors
import com.example.bymevpn.data.LocaleManager
import androidx.compose.runtime.collectAsState
import com.example.bymevpn.theme.AppGradients

enum class BottomNavTab {
    Home,
    Settings,
    Account
}

@Composable
fun ByMeTopBar(
    title: String? = null,
    onBackClick: (() -> Unit)? = null,
    onHelpClick: (() -> Unit)? = null,
    onMoreClick: (() -> Unit)? = null,
    onCloseClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 26.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Leading content: Brand Logo or Back Button + Title
        if (title == null) {
            // Brand Logo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ShieldLogo(size = 30.dp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ByMe",
                        color = AppColors.TextPrimary,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.95).sp
                    )
                    Text(
                        text = "VPN",
                        color = AppColors.BrandMintAccent,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.95).sp
                    )
                }
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (onBackClick != null) {
                    Box(
                        modifier = Modifier
                            .size(37.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(AppColors.TopBarBackBg)
                            .border(1.dp, AppColors.TopBarBackBorder, RoundedCornerShape(14.dp))
                            .clickable(onClick = onBackClick)
                            .testTag("topbar_back_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        ChevronLeftIcon(color = Color(0xFFC5D8D5), size = 18.dp)
                    }
                }
                Text(
                    text = title,
                    color = AppColors.TextPrimary,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-1.15).sp
                )
            }
        }

        // Trailing content: Actions
        Row(
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onCloseClick != null) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(AppColors.TopBarIconBg)
                        .border(1.dp, AppColors.TopBarIconBorder, RoundedCornerShape(14.dp))
                        .clickable(onClick = onCloseClick)
                        .testTag("topbar_close_button"),
                    contentAlignment = Alignment.Center
                ) {
                    CloseXIcon(color = Color(0xFF93ADAB), size = 20.dp)
                }
            } else {
                if (onHelpClick != null) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(AppColors.TopBarIconBg)
                            .border(1.dp, AppColors.TopBarIconBorder, RoundedCornerShape(14.dp))
                            .clickable(onClick = onHelpClick)
                            .testTag("topbar_help_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        QuestionHelpIcon(color = Color(0xFFC5D8D5), size = 19.dp)
                    }
                }
                if (onMoreClick != null) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(AppColors.TopBarIconBg)
                            .border(1.dp, AppColors.TopBarIconBorder, RoundedCornerShape(14.dp))
                            .clickable(onClick = onMoreClick)
                            .testTag("topbar_more_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        MoreHorizontalIcon(color = Color(0xFFC5D8D5), size = 20.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun ByMeBottomNav(
    currentTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val language by LocaleManager.currentLanguage.collectAsState()
    val isRu = LocaleManager.isRussian(language = language)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.NavBackground)
            .border(width = 1.dp, color = AppColors.NavBorder)
            .navigationBarsPadding()
            .height(72.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home tab
            NavTabItem(
                title = if (isRu) "Главная" else "Home",
                selected = currentTab == BottomNavTab.Home,
                onClick = { onTabSelected(BottomNavTab.Home) },
                icon = { color -> PowerIcon(color = color, size = 21.dp) },
                modifier = Modifier.weight(1f)
            )

            // Settings tab
            NavTabItem(
                title = if (isRu) "Настройки" else "Settings",
                selected = currentTab == BottomNavTab.Settings,
                onClick = { onTabSelected(BottomNavTab.Settings) },
                icon = { color -> SettingsGearIcon(color = color, size = 21.dp) },
                modifier = Modifier.weight(1f)
            )

            // Account tab
            NavTabItem(
                title = if (isRu) "Аккаунт" else "Account",
                selected = currentTab == BottomNavTab.Account,
                onClick = { onTabSelected(BottomNavTab.Account) },
                icon = { color -> PersonProfileIcon(color = color, size = 21.dp) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun NavTabItem(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor = if (selected) AppColors.NavItemActive else AppColors.NavItemInactive

    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 10.dp)
            .testTag("nav_tab_${title.lowercase()}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        icon(contentColor)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            color = contentColor,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        if (selected) {
            Box(
                modifier = Modifier
                    .width(25.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(AppColors.NavItemActive)
            )
        } else {
            Spacer(modifier = Modifier.height(3.dp))
        }
    }
}

@Composable
fun ByMePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 55.dp,
    showArrow: Boolean = true
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(17.dp))
            .background(if (enabled) AppGradients.PrimaryButton else androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Color(0xFF25364A), Color(0xFF253B4D))))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp)
            .testTag("primary_button"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (enabled) AppColors.BrandMintDark else AppColors.TextSecondary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.2).sp
        )
        if (showArrow) {
            Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                ChevronRightIcon(color = if (enabled) AppColors.BrandMintDark else AppColors.TextMuted, size = 18.dp)
            }
        }
    }
}

@Composable
fun ByMeSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.SecondaryButtonBg)
            .border(1.dp, AppColors.BorderSecondaryButton, RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp)
            .testTag("secondary_button"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = AppColors.SecondaryButtonText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
            ChevronRightIcon(color = AppColors.SecondaryButtonText, size = 18.dp)
        }
    }
}

@Composable
fun ByMeToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 2.dp,
        label = "toggle_anim"
    )
    val trackBg = if (checked) AppColors.ToggleTrackOn else AppColors.ToggleTrackOff
    val thumbColor = if (checked) AppColors.ToggleThumbOn else AppColors.ToggleThumbOff

    Box(
        modifier = modifier
            .size(width = 43.dp, height = 25.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(trackBg)
            .clickable { onCheckedChange(!checked) }
            .padding(2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(19.dp)
                .clip(CircleShape)
                .background(thumbColor)
        )
    }
}

@Composable
fun ByMeCardGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(19.dp))
            .background(AppColors.BgSurface)
            .border(1.dp, AppColors.BorderDefault, RoundedCornerShape(19.dp))
    ) {
        content()
    }
}

@Composable
fun ByMeSettingRow(
    title: String,
    subtitle: String? = null,
    icon: @Composable () -> Unit,
    trailing: @Composable () -> Unit,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    height: Dp = 76.dp,
    showBottomDivider: Boolean = true
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 15.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(height),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon in rounded box
            Box(
                modifier = Modifier
                    .size(33.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(Color(0xFF172A29)),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }

            // Copy
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    color = Color(0xFFEAF1EF),
                    fontSize = 13.6.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.2).sp
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = subtitle,
                        color = Color(0xFF88A09F),
                        fontSize = 11.4.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            // Trailing
            trailing()
        }

        if (showBottomDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(AppColors.BorderSubtle)
            )
        }
    }
}

@Composable
fun ByMeInfoBox(
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(Color(0xFF102420))
            .border(1.dp, Color(0xFF21463B), RoundedCornerShape(15.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        InfoCircleIcon(color = Color(0xFF86AD9F), size = 19.dp)
        Text(
            text = text,
            color = Color(0xFF86AD9F),
            fontSize = 11.5.sp,
            lineHeight = 16.sp
        )
    }
}
