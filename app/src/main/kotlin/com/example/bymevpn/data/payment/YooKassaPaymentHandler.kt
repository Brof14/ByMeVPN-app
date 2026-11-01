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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class PlanTariff(
    val code: String,
    val name: String,
    val period: String,
    val priceRub: Int,
    val pricePerMonthRub: Int,
    val discountPercent: Int,
    val isBestValue: Boolean,
    val days: Int,
    val badge: String? = null,
    val checkoutEnabled: Boolean = false
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

    /**
     * Launches the YooKassa checkout flow via Chrome Custom Tabs or browser.
     */
    fun openCheckout(
        context: Context,
        tariff: PlanTariff,
        provider: String = "yookassa",
        devices: Int = 2,
        onCheckoutFailed: () -> Unit = {},
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
                    provider = provider,
                    requestId = storage.getOrCreatePaymentRequestId(tariff.code, provider, devices, tariff.priceRub)
                )
            }
            if (res.paymentUrl.isBlank() || res.invoiceId.isNullOrBlank()) {
                throw IllegalStateException("Payment reference is unavailable")
            }
            res
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create in-app payment")
            Toast.makeText(context, "Оплата временно недоступна. Попробуйте позже.", Toast.LENGTH_LONG).show()
            onCheckoutFailed()
            return@launch
        }

        val paymentUrl = createResult.paymentUrl
        if (!Uri.parse(paymentUrl).scheme.equals("https", ignoreCase = true)) {
            Toast.makeText(context, "Не удалось открыть безопасную страницу оплаты.", Toast.LENGTH_LONG).show()
            onCheckoutFailed()
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
                onCheckoutFailed()
            }
        }
        }
    }

    /**
     * Start payment with dynamic plan parameters.
     */
    suspend fun startPayment(
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
        return suspendCancellableCoroutine { continuation ->
            openCheckout(
                context = context,
                tariff = tariff,
                provider = provider,
                devices = devices,
                onCheckoutFailed = { if (continuation.isActive) continuation.resume(false) },
                onCheckoutLaunched = { if (continuation.isActive) continuation.resume(true) }
            )
        }
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
        val result = SubscriptionManager.getInstance(context).refreshStatus(forceBackend = true)
        if (result.getOrNull()?.isUsable == true) {
            val storage = SecureTokenStorage.getInstance(context)
            storage.clearPendingPayment()
            VpnManager.getInstance(context).loadServers()
        }
        return if (result.getOrNull()?.isUsable == true) result
        else Result.failure(result.exceptionOrNull() ?: IllegalStateException("Payment has not activated an account subscription"))
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
