package dev.albertus.expensms.utils

import android.content.ContentResolver
import android.provider.Telephony
import dev.albertus.expensms.data.SupportedBank
import dev.albertus.expensms.data.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SmsSync @Inject constructor(
    private val contentResolver: ContentResolver,
    private val transactionRepository: TransactionRepository,
) {

    suspend fun syncSmsMessages(
        enabledBanks: List<SupportedBank>,
        progressCallback: (Float) -> Unit
    ) = withContext(Dispatchers.IO) {
        val lastSyncTimestamp = transactionRepository.getLastSyncTimestamp()
        val newMessages = readNewSmsMessages(enabledBanks, lastSyncTimestamp)
        val totalMessages = newMessages.size
        var latestTimestamp = lastSyncTimestamp

        newMessages.forEachIndexed { index, (_, body, timestamp) ->
            val transaction = SmsParser.parseTransaction(body, timestamp)
            if (transaction != null) {
                transactionRepository.insertTransaction(transaction)
            }
            latestTimestamp = maxOf(latestTimestamp, timestamp)
            progressCallback((index + 1).toFloat() / totalMessages)
        }

        if (latestTimestamp > lastSyncTimestamp) {
            transactionRepository.updateLastSyncTimestamp(latestTimestamp)
        }
    }

    private fun readNewSmsMessages(enabledBanks: List<SupportedBank>, lastSyncTimestamp: Long): List<Triple<String, String, Long>> {
        val messages = mutableListOf<Triple<String, String, Long>>()
        val uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )

        val selection = buildSelectionString(enabledBanks, lastSyncTimestamp)
        val selectionArgs = buildSelectionArgs(enabledBanks, lastSyncTimestamp)

        contentResolver.query(uri, projection, selection, selectionArgs, "${Telephony.Sms.DATE} ASC")?.use { cursor ->
            val idIndex = cursor.getColumnIndex(Telephony.Sms._ID)
            val bodyIndex = cursor.getColumnIndex(Telephony.Sms.BODY)
            val dateIndex = cursor.getColumnIndex(Telephony.Sms.DATE)

            while (cursor.moveToNext()) {
                val id = cursor.getString(idIndex)
                val body = cursor.getString(bodyIndex)
                val timestamp = cursor.getLong(dateIndex)
                messages.add(Triple(id, body, timestamp))
            }
        }

        return messages
    }

    private fun buildSelectionString(enabledBanks: List<SupportedBank>, lastSyncTimestamp: Long): String {
        val bankFilters = enabledBanks.joinToString(" OR ") { "${Telephony.Sms.ADDRESS} LIKE ?" }
        return "($bankFilters) AND ${Telephony.Sms.DATE} > ?"
    }

    private fun buildSelectionArgs(enabledBanks: List<SupportedBank>, lastSyncTimestamp: Long): Array<String> {
        val args = enabledBanks.map { "%${it.senderFilter}%" }.toMutableList()
        args.add(lastSyncTimestamp.toString())
        return args.toTypedArray()
    }
}