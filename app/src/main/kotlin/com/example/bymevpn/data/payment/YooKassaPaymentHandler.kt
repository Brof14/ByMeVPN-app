package com.example.bymevpn.data.payment

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import com.example.bymevpn.data.api.SubscriptionStatus
import com.example.bymevpn.data.storage.SecureTokenStorage
import com.example.bymevpn.data.subscription.SubscriptionManager
import com.example.bymevpn.vpn.VpnManager
import com.example.bymevpn.data.api.ByMeApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PlanTariff(
    val code: String,
    val name: String,
    val period: String,
    val priceRub: Int,
    val pricePerMonthRub: Int,
    val discountPercent: Int,
    val isBestValue: Boolean,
    val days: Int,
    val badge: String? = null
)

/**
 * Production YooKassa (ЮKassa) integration handler for ByMeVPN.
 * Supports:
 * - СБП (Система Быстрых Платежей - instant 1-tap banking app checkout)
 * - Bank cards (МИР, Visa, Mastercard)
 * - SberPay and T-Pay
 * - Seamless return confirmation and immediate 3x-ui server unlocking
 */
object YooKassaPaymentHandler {

    private const val TAG = "YooKassaPaymentHandler"

    val TARIFFS = listOf(
        PlanTariff(
            code = "12m",
            name = "15 месяцев (12+3)",
            period = "15 месяцев",
            priceRub = 885,
            pricePerMonthRub = 59,
            discountPercent = 34,
            isBestValue = true,
            days = 450,
            badge = "МАКСИМАЛЬНАЯ ВЫГОДА -50%"
        ),
        PlanTariff(
            code = "6m",
            name = "8 месяцев (6+2)",
            period = "8 месяцев",
            priceRub = 552,
            pricePerMonthRub = 69,
            discountPercent = 22,
            isBestValue = false,
            days = 240,
            badge = "ВЫГОДНО"
        ),
        PlanTariff(
            code = "3m",
            name = "4 месяца (3+1)",
            period = "4 месяца",
            priceRub = 316,
            pricePerMonthRub = 79,
            discountPercent = 11,
            isBestValue = false,
            days = 120
        ),
        PlanTariff(
            code = "1m",
            name = "1 месяц",
            period = "1 месяц",
            priceRub = 89,
            pricePerMonthRub = 89,
            discountPercent = 0,
            isBestValue = false,
            days = 30
        )
    )

    /**
     * Launches the YooKassa checkout flow via Chrome Custom Tabs or browser.
     */
    fun openCheckout(
        context: Context,
        tariff: PlanTariff,
        provider: String = "yookassa",
        onCheckoutLaunched: () -> Unit = {}
    ) {
        val storage = SecureTokenStorage.getInstance(context)

        CoroutineScope(Dispatchers.Main).launch {
        // YooKassa hosted payment URL or Bot fallback
        val createResult = try {
            val api = ByMeApiClient(context)
            val res = withContext(Dispatchers.IO) {
                api.createPayment(
                    planCode = tariff.code,
                    amountRub = tariff.priceRub,
                    days = tariff.days,
                    devices = 2,
                    provider = provider,
                    requestId = storage.getOrCreatePaymentRequestId(tariff.code, provider)
                )
            }
            if (res.paymentUrl.isBlank() || res.invoiceId.isNullOrBlank()) {
                throw IllegalStateException("Payment reference is unavailable")
            }
            res
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create in-app payment")
            Toast.makeText(context, "Оплата временно недоступна. Попробуйте позже.", Toast.LENGTH_LONG).show()
            return@launch
        }

        val paymentUrl = createResult.paymentUrl
        if (!Uri.parse(paymentUrl).scheme.equals("https", ignoreCase = true)) {
            Toast.makeText(context, "Не удалось открыть безопасную страницу оплаты.", Toast.LENGTH_LONG).show()
            return@launch
        }
        storage.savePendingPayment(tariff.code, tariff.days, createResult.invoiceId!!, createResult.provider)

        try {
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()

            customTabsIntent.launchUrl(context, Uri.parse(paymentUrl))
            onCheckoutLaunched()
        } catch (e: Exception) {
            Log.w(TAG, "Chrome Custom Tabs failed, falling back to Intent: ${e.message}")
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(paymentUrl))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                onCheckoutLaunched()
            } catch (ex: Exception) {
                Toast.makeText(context, "Не удалось открыть страницу оплаты", Toast.LENGTH_SHORT).show()
            }
        }
        }
    }

    /**
     * Start payment with dynamic plan parameters.
     */
    fun startPayment(
        context: Context,
        planCode: String,
        planTitle: String,
        amountRub: Int,
        devices: Int = 2,
        provider: String = "yookassa",
        days: Int
    ): Boolean {
        val tariff = PlanTariff(
            code = planCode,
            name = planTitle,
            period = "$days дней",
            priceRub = amountRub,
            pricePerMonthRub = amountRub,
            discountPercent = 0,
            isBestValue = false,
            days = days
        )
        openCheckout(context, tariff, provider)
        return true
    }

    /**
     * Activates paid subscription after successful payment via YooKassa or webhook.
     */
    suspend fun confirmPayment(
        context: Context,
        planCode: String,
        days: Int
    ): Result<SubscriptionStatus> {
        val payment = try {
            val pending = SecureTokenStorage.getInstance(context).getPendingPayment()
                ?: return Result.failure(IllegalStateException("Payment reference is unavailable"))
            ByMeApiClient(context).checkPayment(pending.invoiceId, pending.provider)
        } catch (e: Exception) {
            return Result.failure(e)
        }
        if (!payment.isActive || payment.status.lowercase() !in setOf("succeeded", "paid", "completed")) {
            return Result.failure(IllegalStateException("Payment has not been confirmed by the server"))
        }
        val result = SubscriptionManager.getInstance(context).refreshStatus()
        if (result.isSuccess) {
            val storage = SecureTokenStorage.getInstance(context)
            storage.clearPendingPayment()
            VpnManager.getInstance(context).loadServers()
        }
        return result
    }

    /**
     * Checks if a pending payment is awaiting completion upon returning to the app.
     */
    suspend fun checkAndCompletePendingPayment(context: Context): Boolean {
        val storage = SecureTokenStorage.getInstance(context)
        val pending = storage.getPendingPayment() ?: return false
        val result = confirmPayment(context, pending.planCode, pending.days)
        return result.isSuccess
    }

    /**
     * Activates a 3-day free trial immediately without payment.
     */
    suspend fun activateTrial(context: Context): Result<SubscriptionStatus> {
        val subManager = SubscriptionManager.getInstance(context)
        val result = subManager.activateTrial()
        if (result.isSuccess) {
            VpnManager.getInstance(context).loadServers()
        }
        return result
    }
}
