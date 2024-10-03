package dev.albertus.expensms.utils

import dev.albertus.expensms.data.model.Transaction
import dev.albertus.expensms.data.SupportedBank
import java.util.*

interface Logger {
    fun d(tag: String, message: String)
    fun e(tag: String, message: String, throwable: Throwable? = null)
}

class AndroidLogger : Logger {
    override fun d(tag: String, message: String) {
        android.util.Log.d(tag, message)
    }

    override fun e(tag: String, message: String, throwable: Throwable?) {
        android.util.Log.e(tag, message, throwable)
    }
}


object SmsParser {
    private const val TAG = "SmsParser"
    private var logger: Logger = AndroidLogger()

    fun setLogger(newLogger: Logger) {
        logger = newLogger
    }

    fun parseTransaction(body: String, timestamp: Long): Transaction? {
        return SupportedBank.entries.firstNotNullOfOrNull { bank ->
            parseTransactionForBank(bank, body, timestamp)
        }
    }

    fun parseTransactionForBank(bank: SupportedBank, body: String, timestamp: Long): Transaction? {
        val matchResult = bank.regex.find(body)
        if (matchResult == null) {
            logger.d(TAG, "No match found for ${bank.name} regex in body: $body")
            return null
        }

        val cardNumber = matchResult.groups["cardNumber"]?.value
        val dateStr = matchResult.groups["date"]?.value
        val merchant = matchResult.groups["merchant"]?.value
        val amountStr = matchResult.groups["amount"]?.value
        val currencyStr = matchResult.groups["currency"]?.value

        if (cardNumber == null || dateStr == null || merchant == null || amountStr == null || currencyStr == null) {
            logger.d(TAG, "One or more groups not found in regex match for ${bank.name}")
            return null
        }

        val cardLastFourDigits = cardNumber.takeLast(4)
        val date = bank.parseDate(dateStr)
        if (date == null) {
            logger.d(TAG, "Failed to parse date: $dateStr for ${bank.name}")
            return null
        }

        val amount = CurrencyUtils.parse(currencyStr, amountStr)
        if (amount == null) {
            logger.d(TAG, "Failed to parse amount: $amountStr for ${bank.name}")
            return null
        }

        return Transaction(
            id = UUID.randomUUID().toString(),
            bank = bank.displayName,
            cardLastFourDigits = cardLastFourDigits,
            date = date,
            merchant = merchant.trim(),
            amount = amount.number.toDouble(),
            rawMessage = body,
            money = amount
        )
    }
}