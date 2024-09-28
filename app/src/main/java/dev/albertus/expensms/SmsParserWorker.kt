package dev.albertus.expensms

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.text.SimpleDateFormat
import java.util.*

class SmsParserWorker(
    context: Context,
    params: WorkerParameters,
    private val repository: TransactionRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val sender = inputData.getString("sender") ?: return Result.failure()
        val body = inputData.getString("body") ?: return Result.failure()
        val timestamp = inputData.getLong("timestamp", System.currentTimeMillis())

        val transaction = parseTransaction(sender, body, timestamp)
        if (transaction != null) {
            repository.insertTransaction(transaction)
            return Result.success()
        }
        return Result.failure()
    }

    private fun parseTransaction(sender: String, body: String, timestamp: Long): Transaction? {
        val regex = Regex("Anda telah trx dgn KK OCBC (\\d{4}) (\\d{2}/\\d{2}/\\d{2}) di (.+) IDR([\\d,]+\\.\\d{2})\\.")
        val matchResult = regex.find(body) ?: return null

        val (cardLastFourDigits, dateStr, merchant, amountStr) = matchResult.destructured

        val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
        val date = dateFormat.parse(dateStr) ?: Date(timestamp)

        val amount = amountStr.replace(",", "").toDoubleOrNull() ?: return null

        return Transaction(
            id = UUID.randomUUID().toString(),
            cardLastFourDigits = cardLastFourDigits,
            date = date,
            merchant = merchant.trim(),
            amount = amount,
            currency = "IDR",
            rawMessage = body
        )
    }
}