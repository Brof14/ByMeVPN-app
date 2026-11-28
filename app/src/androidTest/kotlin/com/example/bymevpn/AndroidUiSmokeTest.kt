package com.example.bymevpn

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.bymevpn.components.ByMeSettingRow
import com.example.bymevpn.data.api.SubscriptionStatus
import com.example.bymevpn.screens.AuthBrandPanel
import com.example.bymevpn.screens.SubscriptionRecoveryCard
import com.example.bymevpn.components.VpnPowerButton
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Runs on an Android emulator, exercising actual Compose accessibility semantics.
 * This suite is not an end-to-end VPN, network leak or payment test.
 */
class AndroidUiSmokeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun disconnectedButtonHasAccessibleActionAndWorks() {
        var taps = 0
        composeRule.setContent {
            VpnPowerButton(
                connected = false,
                connecting = false,
                isRussian = true,
                onClick = { taps++ }
            )
        }

        composeRule.onNodeWithContentDescription("Включить VPN")
            .assertHasClickAction()
            .performClick()
        composeRule.runOnIdle { assertEquals(1, taps) }
    }

    @Test
    fun connectedButtonOffersExplicitDisconnectAction() {
        var taps = 0
        composeRule.setContent {
            VpnPowerButton(
                connected = true,
                connecting = false,
                isRussian = false,
                onClick = { taps++ }
            )
        }

        composeRule.onNodeWithContentDescription("Disconnect VPN")
            .assertHasClickAction()
            .performClick()
        composeRule.runOnIdle { assertEquals(1, taps) }
    }

    @Test
    fun authBrandHeaderUsesOnlyTheProductMarkAndWordmark() {
        composeRule.setContent {
            AuthBrandPanel(compact = false)
        }

        composeRule.onNodeWithText("ByMe").assertIsDisplayed()
        composeRule.onNodeWithText("VPN").assertIsDisplayed()
    }

    @Test
    fun subscriptionRecoveryHasNoNestedLastCheckPanel() {
        var retries = 0
        composeRule.setContent {
            SubscriptionRecoveryCard(
                isRu = true,
                isChecking = false,
                accessConfirmationRequired = false,
                status = SubscriptionStatus(
                    status = "unknown",
                    trialUnavailableReason = "subscription_verification_required"
                ),
                lastError = null,
                onRetry = { retries++ }
            )
        }

        composeRule.onNodeWithText("Не подтверждён срок подписки").assertIsDisplayed()
        composeRule.onNodeWithText("Ссылка подписки найдена, но панель не вернула подтверждённую дату окончания. Доступ не считается отменённым — повторите запрос.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Повторить проверку").assertHasClickAction().performClick()
        composeRule.runOnIdle { assertEquals(1, retries) }
    }

    @Test
    fun settingsRowDisplaysTitleAndExplanationAtDoubleFontScale() {
        composeRule.setContent {
            val deviceDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(deviceDensity.density, fontScale = 2f)
            ) {
                Box(modifier = Modifier.width(320.dp)) {
                    ByMeSettingRow(
                        title = "Защита при обрыве VPN",
                        subtitle = "Откройте системные параметры Android для защиты соединения",
                        icon = { Text("!") },
                        trailing = {}
                    )
                }
            }
        }

        composeRule.onNodeWithText("Защита при обрыве VPN").assertIsDisplayed()
        composeRule.onNodeWithText("Откройте системные параметры Android для защиты соединения").assertIsDisplayed()
    }
}
