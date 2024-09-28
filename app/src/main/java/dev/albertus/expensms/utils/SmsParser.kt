package dev.albertus.expensms.utils

import dev.albertus.expensms.data.model.Transaction
import java.text.SimpleDateFormat
import java.util.*

object SmsParser {
    fun parseTransaction(body: String, timestamp: Long): Transaction? {
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