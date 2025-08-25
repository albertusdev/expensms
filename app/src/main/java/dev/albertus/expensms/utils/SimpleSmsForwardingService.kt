package dev.albertus.expensms.utils

import android.util.Log
import dev.albertus.expensms.data.api.ApiService
import dev.albertus.expensms.data.model.SmsMessage
import dev.albertus.expensms.data.repository.SmsMessageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SimpleSmsForwardingService @Inject constructor(
    private val apiService: ApiService,
    private val smsMessageRepository: SmsMessageRepository,
    private val coroutineScope: CoroutineScope
) {
    companion object {
        private const val TAG = "SimpleSmsForwardingService"
    }
    
    fun forwardSmsMessageIfEnabled(smsMessage: SmsMessage) {
        Log.i(TAG, "=== SMS API FORWARDING CHECK ===")
        Log.i(TAG, "SMS ID: ${smsMessage.id}")
        Log.i(TAG, "SMS sender: '${smsMessage.sender}'")
        Log.i(TAG, "SMS bank source: ${smsMessage.bankSource}")
        Log.i(TAG, "SMS timestamp: ${smsMessage.timestamp}")
        Log.i(TAG, "SMS message preview: ${smsMessage.rawMessage.take(50)}...")

        // Check if SMS contains OTP-related keywords
        val otpKeywords = listOf("otp", "verification", "verify", "code", "pin", "password", "login", "signin", "authentication")
        val containsOtp = otpKeywords.any { keyword ->
            smsMessage.rawMessage.contains(keyword, ignoreCase = true)
        }

        if (containsOtp) {
            Log.w(TAG, "❌ SMS contains OTP keywords - skipping API forwarding for security")
            Log.w(TAG, "   Detected keywords: ${otpKeywords.filter { smsMessage.rawMessage.contains(it, ignoreCase = true) }}")
            Log.d(TAG, "   Raw message: ${smsMessage.rawMessage.take(100)}...")
            return
        }

        // Only forward SMS messages that match enabled sender filters
        if (smsMessage.bankSource == null) {
            Log.w(TAG, "❌ SMS does not match any enabled sender filters - skipping API forwarding")
            Log.w(TAG, "   Sender: '${smsMessage.sender}' has no bank source assigned")
            return
        }

        Log.i(TAG, "✅ SMS passed all checks - proceeding with API forwarding")
        Log.i(TAG, "   Bank source: ${smsMessage.bankSource}")
        Log.i(TAG, "   Will forward to aiccountant API")

        Log.i(TAG, "✅ SMS is eligible for API forwarding")
        
        coroutineScope.launch {
            try {
                Log.i(TAG, "Forwarding SMS to API...")
                val result = apiService.forwardSmsMessage(smsMessage)
                Log.i(TAG, "✅ SMS forwarding completed: $result")

                // Update forwarding status
                smsMessageRepository.updateForwardingStatus(
                    listOf(smsMessage.id),
                    true,
                    Date()
                )
                Log.i(TAG, "✅ Updated SMS forwarding status in database")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to forward SMS to API", e)
            }
        }
    }

    fun forwardSmsMessagesBatchIfEnabled(smsMessages: List<SmsMessage>) {
        Log.d(TAG, "=== BATCH SMS API FORWARDING CHECK ===")
        Log.d(TAG, "Total SMS messages: ${smsMessages.size}")

        // Filter out SMS messages with OTP keywords and non-bank SMS
        val validSmsMessages = smsMessages.filter { smsMessage ->
            val hasOtpKeywords = containsOtpKeywords(smsMessage.rawMessage)
            val isFromBank = smsMessage.bankSource != null
            
            if (hasOtpKeywords) {
                Log.w(TAG, "❌ SMS ${smsMessage.id} contains OTP keywords - skipping")
                false
            } else if (!isFromBank) {
                Log.d(TAG, "❌ SMS ${smsMessage.id} does not match any enabled sender filters - skipping")
                false
            } else {
                true
            }
        }

        Log.i(TAG, "Valid SMS messages for forwarding: ${validSmsMessages.size}")

        if (validSmsMessages.isEmpty()) {
            Log.w(TAG, "❌ No valid SMS messages to forward")
            return
        }

        // Split into batches of 100 (API limit)
        val batches = validSmsMessages.chunked(100)
        Log.i(TAG, "Split into ${batches.size} batch(es) of max 100 SMS messages each")

        coroutineScope.launch {
            val successfulSmsIds = mutableListOf<String>()
            
            batches.forEachIndexed { batchIndex, batch ->
                try {
                    Log.i(TAG, "Processing batch ${batchIndex + 1}/${batches.size} with ${batch.size} SMS messages")
                    val result = apiService.forwardSmsMessagesBatch(batch)
                    Log.i(TAG, "✅ Batch ${batchIndex + 1} forwarding completed: $result")
                    
                    // Log batch summary if successful
                    if (result is dev.albertus.expensms.data.api.ApiResult.Success) {
                        val summary = result.data.summary
                        Log.i(TAG, "Batch ${batchIndex + 1} summary: ${summary.stored}/${summary.total} stored, ${summary.queued} queued, ${summary.failed} failed")
                        
                        // Track successful SMS IDs for this batch
                        val batchSmsIds = batch.map { it.id }
                        successfulSmsIds.addAll(batchSmsIds)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Failed to forward batch ${batchIndex + 1} to API", e)
                }
            }
            
            // Update forwarding status for successful SMS messages
            if (successfulSmsIds.isNotEmpty()) {
                try {
                    smsMessageRepository.updateForwardingStatus(
                        successfulSmsIds, 
                        true, 
                        Date()
                    )
                    Log.i(TAG, "✅ Updated forwarding status for ${successfulSmsIds.size} SMS messages")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Failed to update forwarding status", e)
                }
            }
            
            Log.i(TAG, "=== BATCH SMS API FORWARDING COMPLETED ===")
        }
    }

    private fun containsOtpKeywords(message: String): Boolean {
        val otpKeywords = listOf(
            "otp", "one time password", "verification code", "kode verifikasi",
            "kode otp", "password sekali pakai", "sandi sekali pakai"
        )
        
        val lowerMessage = message.lowercase()
        return otpKeywords.any { keyword ->
            lowerMessage.contains(keyword)
        }
    }
}


