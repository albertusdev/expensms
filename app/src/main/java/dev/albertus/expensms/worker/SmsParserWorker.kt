package dev.albertus.expensms.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.albertus.expensms.data.repository.TransactionRepository
import dev.albertus.expensms.utils.SmsForwardingService
import dev.albertus.expensms.utils.SmsParser

class SmsParserWorker(
    context: Context,
    params: WorkerParameters,
    private val repository: TransactionRepository,
    private val smsForwardingService: SmsForwardingService
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "SmsParserWorker"
    }

    override suspend fun doWork(): Result {
        Log.i(TAG, "=== SMS PARSER WORKER STARTED ===")

        val sender = inputData.getString("sender") ?: run {
            Log.e(TAG, "No sender provided in input data")
            return Result.failure()
        }
        val body = inputData.getString("body") ?: run {
            Log.e(TAG, "No body provided in input data")
            return Result.failure()
        }
        val timestamp = inputData.getLong("timestamp", System.currentTimeMillis())

        Log.i(TAG, "Processing SMS from WorkManager:")
        Log.i(TAG, "  Sender: '$sender'")
        Log.i(TAG, "  Timestamp: ${java.util.Date(timestamp)}")
        Log.i(TAG, "  Body: ${body.take(100)}${if (body.length > 100) "..." else ""}")

        val transaction = SmsParser.parseTransaction(body, timestamp, sender)
        if (transaction != null) {
            Log.i(TAG, "✅ Transaction parsed successfully: ${transaction.bank} - ${transaction.merchant} - ${transaction.amount}")
            repository.insertTransaction(transaction)
            Log.i(TAG, "✅ Transaction saved to database")

            // Forward to API if enabled
            Log.i(TAG, "Attempting to forward to API...")
            smsForwardingService.forwardTransactionIfEnabled(transaction)
            Log.i(TAG, "=== SMS PARSER WORKER COMPLETED SUCCESSFULLY ===")
            return Result.success()
        } else {
            Log.w(TAG, "❌ Failed to parse SMS - no matching parser found")
            Log.i(TAG, "=== SMS PARSER WORKER COMPLETED (NO MATCH) ===")
            return Result.failure()
        }
    }
}