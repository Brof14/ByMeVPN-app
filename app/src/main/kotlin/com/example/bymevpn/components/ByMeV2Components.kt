package com.example.bymevpn.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bymevpn.data.LocaleManager
import com.example.bymevpn.theme.AppColors
import com.example.bymevpn.theme.AppGradients

enum class BottomNavTab {
    Home,
    Settings,
    Account
}

private val LogoTapEase = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

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
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (title == null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ShieldLogo(size = 42.dp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ByMe",
                        color = AppColors.TextPrimary,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.9).sp
                    )
                    Text(
                        text = "VPN",
                        color = AppColors.SkyBlue,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.9).sp
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (onBackClick != null) {
                    ByMeTopBarAction(
                        description = if (LocaleManager.isRussian()) "Назад" else "Back",
                        testTag = "topbar_back_button",
                        background = AppColors.TopBarBackBg,
                        border = AppColors.TopBarBackBorder,
                        onClick = onBackClick
                    ) {
                        ChevronLeftIcon(color = AppColors.TextSoft, size = 18.dp)
                    }
                }
                Text(
                    text = title,
                    color = AppColors.TextPrimary,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.9).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onCloseClick != null) {
                ByMeTopBarAction(
                    description = if (LocaleManager.isRussian()) "Закрыть" else "Close",
                    testTag = "topbar_close_button",
                    background = AppColors.TopBarIconBg,
                    border = AppColors.TopBarIconBorder,
                    onClick = onCloseClick
                ) {
                    CloseXIcon(color = AppColors.TextSoft, size = 20.dp)
                }
            } else {
                if (onHelpClick != null) {
                    ByMeTopBarAction(
                        description = if (LocaleManager.isRussian()) "Частые вопросы" else "FAQ",
                        testTag = "topbar_help_button",
                        background = AppColors.TopBarIconBg,
                        border = AppColors.TopBarIconBorder,
                        onClick = onHelpClick
                    ) {
                        QuestionHelpIcon(color = AppColors.TextSoft, size = 19.dp)
                    }
                }
                if (onMoreClick != null) {
                    ByMeTopBarAction(
                        description = if (LocaleManager.isRussian()) "Дополнительно" else "More",
                        testTag = "topbar_more_button",
                        background = AppColors.TopBarIconBg,
                        border = AppColors.TopBarIconBorder,
                        onClick = onMoreClick
                    ) {
                        MoreHorizontalIcon(color = AppColors.TextSoft, size = 20.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ByMeTopBarAction(
    description: String,
    testTag: String,
    background: Color,
    border: Color,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = tween(140, easing = LogoTapEase),
        label = "topbar_action_scale"
    )
    Box(
        modifier = Modifier
            .size(48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .semantics { contentDescription = description }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(background)
                .border(1.dp, border, RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center
        ) {
            icon()
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
            .navigationBarsPadding()
            .heightIn(min = 68.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(AppColors.NavBorder)
                .align(Alignment.TopCenter)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavTabItem(
                title = if (isRu) "Главная" else "Home",
                selected = currentTab == BottomNavTab.Home,
                onClick = { onTabSelected(BottomNavTab.Home) },
                icon = { color -> PowerIcon(color = color, size = 21.dp) },
                modifier = Modifier.weight(1f)
            )
            NavTabItem(
                title = if (isRu) "Настройки" else "Settings",
                selected = currentTab == BottomNavTab.Settings,
                onClick = { onTabSelected(BottomNavTab.Settings) },
                icon = { color -> SettingsGearIcon(color = color, size = 21.dp) },
                modifier = Modifier.weight(1f)
            )
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
            .selectable(
                selected = selected,
                role = Role.Tab,
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
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        if (selected) {
            Box(
                modifier = Modifier
                    .width(26.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(AppGradients.PrimaryButton)
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
    height: Dp = 54.dp,
    showArrow: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (enabled && pressed) 0.97f else 1f,
        animationSpec = tween(100, easing = LogoTapEase),
        label = "primary_btn_scale"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .heightIn(min = height)
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) AppGradients.PrimaryButton else AppGradients.DisabledButton)
            .then(
                if (!enabled) Modifier.border(1.dp, AppColors.BorderDefault, RoundedCornerShape(16.dp))
                else Modifier
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("primary_button"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (enabled) AppColors.BrandMintDark else AppColors.TextSecondary,
            fontSize = 15.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.2).sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (showArrow) 22.dp else 0.dp)
        )
        if (showArrow) {
            Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                ChevronRightIcon(
                    color = if (enabled) AppColors.BrandMintDark else AppColors.TextMuted,
                    size = 18.dp
                )
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
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (enabled && pressed) 0.97f else 1f,
        animationSpec = tween(100, easing = LogoTapEase),
        label = "secondary_btn_scale"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.SecondaryButtonBg)
            .border(1.dp, AppColors.BorderSecondaryButton, RoundedCornerShape(14.dp))
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .testTag("secondary_button"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (enabled) AppColors.SecondaryButtonText else AppColors.TextMuted,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
        )
        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
            ChevronRightIcon(
                color = if (enabled) AppColors.SkyBlue else AppColors.TextMuted,
                size = 18.dp
            )
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
        targetValue = if (checked) 19.dp else 0.dp,
        animationSpec = tween(140, easing = LogoTapEase),
        label = "toggle_anim"
    )
    val thumbColor = if (checked) AppColors.ToggleThumbOn else AppColors.ToggleThumbOff

    Box(
        modifier = modifier
            .size(48.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 25.dp)
                .clip(RoundedCornerShape(99.dp))
                .then(
                    if (checked) Modifier.background(AppGradients.PrimaryButton)
                    else Modifier.background(AppColors.ToggleTrackOff)
                )
                .border(
                    width = 1.dp,
                    color = if (checked) Color.Transparent else AppColors.BorderDefault,
                    shape = RoundedCornerShape(99.dp)
                )
                .padding(2.5.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(thumbOffset.roundToPx(), 0) }
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(thumbColor)
            )
        }
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
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.BgSurface)
            .border(1.dp, AppColors.BorderDefault, RoundedCornerShape(16.dp))
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
    height: Dp = 72.dp,
    showBottomDivider: Boolean = true
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = height)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(AppColors.BgSurfaceElevated)
                    .border(0.8.dp, AppColors.BorderInput, RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    color = AppColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.2).sp
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = subtitle,
                        color = AppColors.TextSecondary,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp
                    )
                }
            }

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
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF091D3A))
            .border(1.dp, AppColors.BorderAccent, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        InfoCircleIcon(color = AppColors.SkyBlue, size = 18.dp)
        Text(
            text = text,
            color = AppColors.TextSoft,
            fontSize = 12.5.sp,
            lineHeight = 18.sp
        )
    }
}
