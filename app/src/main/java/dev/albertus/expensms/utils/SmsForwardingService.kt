package dev.albertus.expensms.utils

import android.util.Log
import dev.albertus.expensms.data.api.ApiService
import dev.albertus.expensms.data.model.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsForwardingService @Inject constructor(
    private val apiService: ApiService,
    private val coroutineScope: CoroutineScope
) {

    companion object {
        private const val TAG = "SmsForwardingService"
    }
    
    fun forwardTransactionIfEnabled(transaction: Transaction) {
        Log.d(TAG, "=== API FORWARDING CHECK ===")
        Log.d(TAG, "Transaction bank: ${transaction.bank}")

        // Check if SMS contains OTP-related keywords
        if (containsOtpKeywords(transaction.rawMessage)) {
            Log.w(TAG, "❌ SMS contains OTP keywords - skipping API forwarding for security")
            Log.d(TAG, "Raw message: ${transaction.rawMessage.take(50)}...")
            return
        }

        // Only forward OCBC transactions for now
        if (transaction.bank == "OCBC") {
            Log.i(TAG, "✅ OCBC transaction - attempting to forward to API")
            coroutineScope.launch {
                try {
                    Log.d(TAG, "Calling apiService.forwardTransaction()...")
                    val result = apiService.forwardTransaction(transaction)
                    Log.i(TAG, "✅ API forwarding completed: $result")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Failed to forward transaction to API", e)
                }
            }
        } else {
            Log.d(TAG, "❌ Not an OCBC transaction - skipping API forwarding")
        }
    }

    private fun containsOtpKeywords(message: String): Boolean {
        val otpKeywords = listOf(
            "OTP",
            "otp",
            "One Time Password",
            "one time password",
            "verification code",
            "kode verifikasi",
            "kode OTP",
            "RAHASIAKAN OTP",
            "jangan berikan",
            "jangan bagikan",
            "PIN",
            "password sementara"
        )

        return otpKeywords.any { keyword ->
            message.contains(keyword, ignoreCase = true)
        }
    }
}
