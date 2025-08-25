package dev.albertus.expensms.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.albertus.expensms.data.model.SmsMessage
import dev.albertus.expensms.data.repository.SenderFilterRepository
import dev.albertus.expensms.data.repository.SmsMessageRepository
import dev.albertus.expensms.utils.SimpleSmsForwardingService
import dev.albertus.expensms.utils.NotificationService
import java.util.*

class SimpleSmsWorker(
    context: Context,
    params: WorkerParameters,
    private val smsMessageRepository: SmsMessageRepository,
    private val senderFilterRepository: SenderFilterRepository,
    private val smsForwardingService: SimpleSmsForwardingService,
    private val notificationService: NotificationService
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "SimpleSmsWorker"
    }

    override suspend fun doWork(): Result {
        Log.i(TAG, "=== SIMPLE SMS WORKER STARTED ===")

        val sender = inputData.getString("sender") ?: run {
            Log.e(TAG, "No sender provided in input data")
            return Result.failure()
        }
        val body = inputData.getString("body") ?: run {
            Log.e(TAG, "No body provided in input data")
            return Result.failure()
        }
        val timestamp = inputData.getLong("timestamp", System.currentTimeMillis())
        val isBackgroundProcessing = inputData.getBoolean("is_background", true)

        Log.i(TAG, "Processing SMS from WorkManager:")
        Log.i(TAG, "  Sender: '$sender'")
        Log.i(TAG, "  Timestamp: ${Date(timestamp)}")
        Log.i(TAG, "  Body: ${body.take(100)}${if (body.length > 100) "..." else ""}")
        Log.i(TAG, "  Background processing: $isBackgroundProcessing")

        // Get all enabled filters for detailed logging
        val enabledFilters = senderFilterRepository.getEnabledSenderFilters()
        Log.i(TAG, "=== SENDER FILTER MATCHING ===")
        Log.i(TAG, "Total enabled filters: ${enabledFilters.size}")

        enabledFilters.forEachIndexed { index, filter ->
            val matches = sender.contains(filter.filterPattern, ignoreCase = true)
            Log.i(TAG, "Filter ${index + 1}: '${filter.name}' (pattern: '${filter.filterPattern}') -> ${if (matches) "✅ MATCH" else "❌ NO MATCH"}")
            if (matches) {
                Log.i(TAG, "  ✅ MATCH DETAILS:")
                Log.i(TAG, "    Sender: '$sender'")
                Log.i(TAG, "    Pattern: '${filter.filterPattern}'")
                Log.i(TAG, "    Case insensitive check: ${sender.lowercase()} contains ${filter.filterPattern.lowercase()}")
            }
        }

        // Determine bank source from configurable sender filters
        val matchedFilter = senderFilterRepository.matchesSender(sender)
        val bankSource = matchedFilter?.name

        Log.i(TAG, "=== FINAL MATCHING RESULT ===")
        Log.i(TAG, "  Matched sender filter: ${matchedFilter?.name ?: "None"}")
        Log.i(TAG, "  Filter pattern: ${matchedFilter?.filterPattern ?: "N/A"}")
        Log.i(TAG, "  Detected bank source: $bankSource")

        // Create SMS message with content hash for duplicate detection
        val smsMessage = SmsMessage.create(
            id = UUID.randomUUID().toString(),
            sender = sender,
            rawMessage = body,
            timestamp = Date(timestamp),
            bankSource = bankSource
        )

        val timeWindow = 5000L
        val normalizedTimestamp = (timestamp / timeWindow) * timeWindow
        Log.i(TAG, "=== HASH GENERATION ===")
        Log.i(TAG, "Original timestamp: ${Date(timestamp)} ($timestamp)")
        Log.i(TAG, "Normalized timestamp (5s window): ${Date(normalizedTimestamp)} ($normalizedTimestamp)")
        Log.i(TAG, "Generated content hash: ${smsMessage.contentHash}")

        try {
            // Store SMS message in database
            smsMessageRepository.insertSmsMessage(smsMessage)
            Log.i(TAG, "✅ SMS message saved to database")

            // Update last sync timestamp to prevent duplicates during next sync
            smsMessageRepository.updateLastSyncTimestamp(timestamp)
            Log.i(TAG, "✅ Updated last sync timestamp to: ${Date(timestamp)} to prevent duplicates")

            // Show enhanced notification for background processing
            if (isBackgroundProcessing) {
                notificationService.showSmsProcessingNotification(
                    sender = sender,
                    messagePreview = body,
                    bankSource = bankSource,
                    isBackground = true
                )
            }

            // Automatically forward to API if it matches an enabled sender filter
            if (bankSource != null) {
                Log.i(TAG, "SMS matches enabled filter '${matchedFilter?.name}' - attempting automatic forwarding...")

                try {
                    smsForwardingService.forwardSmsMessageIfEnabled(smsMessage)

                    // Show enhanced success notification for background processing
                    if (isBackgroundProcessing) {
                        notificationService.showSmsForwardingNotification(
                            sender = sender,
                            success = true,
                            bankSource = bankSource
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to forward SMS", e)

                    // Show enhanced error notification for background processing
                    if (isBackgroundProcessing) {
                        notificationService.showSmsForwardingNotification(
                            sender = sender,
                            success = false,
                            errorMessage = e.message,
                            bankSource = bankSource
                        )
                    }
                }
            } else {
                Log.d(TAG, "SMS does not match any enabled sender filters - skipping API forwarding")
            }

            Log.i(TAG, "=== SIMPLE SMS WORKER COMPLETED SUCCESSFULLY ===")
            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to process SMS", e)
            Log.i(TAG, "=== SIMPLE SMS WORKER FAILED ===")
            return Result.failure()
        }
    }
}
