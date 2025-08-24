package dev.albertus.expensms.utils

import android.content.ContentResolver
import android.provider.Telephony
import android.util.Log
import dev.albertus.expensms.data.SupportedBank
import dev.albertus.expensms.data.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SmsSync @Inject constructor(
    private val contentResolver: ContentResolver,
    private val transactionRepository: TransactionRepository,
) {
    companion object {
        private const val TAG = "SmsSync"
    }

    suspend fun syncSmsMessages(
        enabledBanks: List<SupportedBank>,
        progressCallback: (Float) -> Unit,
        fullSync: Boolean = false
    ) = withContext(Dispatchers.IO) {
        Log.i(TAG, "=== SMS SYNC STARTED ===")
        Log.i(TAG, "Full sync: $fullSync")
        Log.i(TAG, "Enabled banks: ${enabledBanks.map { it.name }}")

        if (enabledBanks.isEmpty()) {
            Log.w(TAG, "❌ No banks enabled - skipping SMS sync")
            Log.i(TAG, "=== SMS SYNC COMPLETED (NO BANKS) ===")
            return@withContext
        }

        val lastSyncTimestamp = if (fullSync) 0L else transactionRepository.getLastSyncTimestamp()
        Log.i(TAG, "Last sync timestamp: $lastSyncTimestamp (${if (lastSyncTimestamp > 0) java.util.Date(lastSyncTimestamp) else "Never synced"})")

        val messages = readSmsMessages(enabledBanks, lastSyncTimestamp)
        val totalMessages = messages.size
        Log.i(TAG, "Found $totalMessages SMS messages to process")

        var latestTimestamp = lastSyncTimestamp
        var parsedCount = 0
        var skippedCount = 0

        messages.forEachIndexed { index, (sender, body, timestamp) ->
            Log.d(TAG, "Processing SMS ${index + 1}/$totalMessages:")
            Log.d(TAG, "  Sender: '$sender'")
            Log.d(TAG, "  Date: ${java.util.Date(timestamp)}")
            Log.d(TAG, "  Body: ${body.take(100)}${if (body.length > 100) "..." else ""}")

            val transaction = SmsParser.parseTransaction(body, timestamp, sender)
            if (transaction != null) {
                Log.i(TAG, "  ✅ PARSED: ${transaction.bank} - ${transaction.merchant} - ${transaction.amount}")
                transactionRepository.insertTransaction(transaction)
                parsedCount++
            } else {
                Log.d(TAG, "  ❌ SKIPPED: No matching parser")
                skippedCount++
            }
            latestTimestamp = maxOf(latestTimestamp, timestamp)
            progressCallback((index + 1).toFloat() / totalMessages)
        }

        if (latestTimestamp > lastSyncTimestamp) {
            transactionRepository.updateLastSyncTimestamp(latestTimestamp)
            Log.i(TAG, "Updated sync timestamp to: ${java.util.Date(latestTimestamp)}")
        }

        Log.i(TAG, "=== SMS SYNC COMPLETED ===")
        Log.i(TAG, "Total processed: $totalMessages")
        Log.i(TAG, "Successfully parsed: $parsedCount")
        Log.i(TAG, "Skipped: $skippedCount")
    }

    private fun readSmsMessages(enabledBanks: List<SupportedBank>, lastSyncTimestamp: Long): List<Triple<String, String, Long>> {
        val messages = mutableListOf<Triple<String, String, Long>>()
        val uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )

        val selection = buildSelectionString(enabledBanks, lastSyncTimestamp)
        val selectionArgs = buildSelectionArgs(enabledBanks, lastSyncTimestamp)

        Log.d(TAG, "SMS Query Details:")
        Log.d(TAG, "  Selection: $selection")
        Log.d(TAG, "  Args: ${selectionArgs.contentToString()}")

        contentResolver.query(uri, projection, selection, selectionArgs, "${Telephony.Sms.DATE} ASC")?.use { cursor ->
            Log.d(TAG, "SMS query returned ${cursor.count} messages")

            val idIndex = cursor.getColumnIndex(Telephony.Sms._ID)
            val addressIndex = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIndex = cursor.getColumnIndex(Telephony.Sms.BODY)
            val dateIndex = cursor.getColumnIndex(Telephony.Sms.DATE)

            while (cursor.moveToNext()) {
                val id = cursor.getString(idIndex)
                val sender = cursor.getString(addressIndex) ?: "Unknown"
                val body = cursor.getString(bodyIndex)
                val timestamp = cursor.getLong(dateIndex)
                messages.add(Triple(sender, body, timestamp))
            }
        } ?: run {
            Log.e(TAG, "Failed to query SMS messages - cursor is null")
        }

        Log.d(TAG, "Collected ${messages.size} SMS messages for processing")
        return messages
    }

    private fun buildSelectionString(enabledBanks: List<SupportedBank>, lastSyncTimestamp: Long): String {
        if (enabledBanks.isEmpty()) {
            // Return a condition that matches nothing
            return "1 = 0"
        }

        val bankFilters = enabledBanks.joinToString(" OR ") { "${Telephony.Sms.ADDRESS} LIKE ?" }
        return if (lastSyncTimestamp > 0) {
            "($bankFilters) AND ${Telephony.Sms.DATE} > ?"
        } else {
            "($bankFilters)"
        }
    }

    private fun buildSelectionArgs(enabledBanks: List<SupportedBank>, lastSyncTimestamp: Long): Array<String> {
        if (enabledBanks.isEmpty()) {
            // Return empty args for "1 = 0" condition
            return emptyArray()
        }

        val args = enabledBanks.map { "%${it.senderFilter}%" }.toMutableList()
        if (lastSyncTimestamp > 0) {
            args.add(lastSyncTimestamp.toString())
        }
        return args.toTypedArray()
    }
}