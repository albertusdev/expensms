package dev.albertus.expensms.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.albertus.expensms.data.repository.TransactionRepository
import dev.albertus.expensms.utils.SmsParser

class SmsParserWorker(
    context: Context,
    params: WorkerParameters,
    private val repository: TransactionRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val sender = inputData.getString("sender") ?: return Result.failure()
        val body = inputData.getString("body") ?: return Result.failure()
        val timestamp = inputData.getLong("timestamp", System.currentTimeMillis())

        val transaction = SmsParser.parseTransaction(body, timestamp)
        if (transaction != null) {
            repository.insertTransaction(transaction)
            return Result.success()
        }
        return Result.failure()
    }
}