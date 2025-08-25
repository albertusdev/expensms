package dev.albertus.expensms.utils

import android.content.ContentResolver
import android.provider.Telephony
import android.util.Log
import dev.albertus.expensms.data.model.SenderFilter
import dev.albertus.expensms.data.model.SmsMessage
import dev.albertus.expensms.data.repository.SenderFilterRepository
import dev.albertus.expensms.data.repository.SmsMessageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

class SimpleSmsSync @Inject constructor(
    private val contentResolver: ContentResolver,
    private val smsMessageRepository: SmsMessageRepository,
    private val senderFilterRepository: SenderFilterRepository
) {
    companion object {
        private const val TAG = "SimpleSmsSync"
    }

    suspend fun syncSmsMessages(
        progressCallback: (Float) -> Unit,
        fullSync: Boolean = false
    ) = withContext(Dispatchers.IO) {
        Log.i(TAG, "=== SMS SYNC STARTED ===")
        Log.i(TAG, "Full sync: $fullSync")

        // Get enabled sender filters
        val enabledFilters = senderFilterRepository.getEnabledSenderFilters()
        Log.i(TAG, "Enabled sender filters: ${enabledFilters.map { "${it.name} (${it.filterPattern})" }}")

        // If full sync, first update all existing SMS messages with new filter matching
        if (fullSync) {
            Log.i(TAG, "=== UPDATING EXISTING SMS WITH NEW FILTERS ===")
            updateExistingSmsWithFilters(enabledFilters, progressCallback)
        }

        if (enabledFilters.isEmpty()) {
            Log.w(TAG, "❌ No sender filters enabled - skipping new SMS sync")
            Log.i(TAG, "=== SMS SYNC COMPLETED (NO FILTERS) ===")
            return@withContext
        }

        val lastSyncTimestamp = if (fullSync) 0L else smsMessageRepository.getLastSyncTimestamp()
        Log.i(TAG, "Last sync timestamp: $lastSyncTimestamp (${if (lastSyncTimestamp > 0) Date(lastSyncTimestamp) else "Never synced"})")

        val messages = readSmsMessages(enabledFilters, lastSyncTimestamp)
        val totalMessages = messages.size
        Log.i(TAG, "Found $totalMessages SMS messages to process")

        var latestTimestamp = lastSyncTimestamp
        var storedCount = 0

        val smsMessagesToInsert = mutableListOf<SmsMessage>()
        val existingSmsMessages = if (fullSync) {
            // For full sync, get all existing SMS to avoid duplicates
            smsMessageRepository.getAllSmsMessagesSync()
        } else {
            emptyList()
        }

        Log.i(TAG, "Found ${existingSmsMessages.size} existing SMS messages")

        messages.forEachIndexed { index, (sender, body, timestamp) ->
            Log.d(TAG, "Processing SMS ${index + 1}/$totalMessages:")
            Log.d(TAG, "  Sender: '$sender'")
            Log.d(TAG, "  Date: ${Date(timestamp)}")
            Log.d(TAG, "  Body: ${body.take(100)}${if (body.length > 100) "..." else ""}")

            // Check if this SMS already exists (avoid duplicates)
            val existingSms = existingSmsMessages.find { existing ->
                existing.sender == sender &&
                existing.rawMessage == body &&
                existing.timestamp.time == timestamp
            }

            if (existingSms != null) {
                Log.d(TAG, "SMS already exists, checking for filter updates...")

                // Update existing SMS with new filter matching if needed
                val matchedFilter = enabledFilters.find { filter ->
                    sender.contains(filter.filterPattern, ignoreCase = true)
                }
                val newBankSource = matchedFilter?.name

                if (existingSms.bankSource != newBankSource) {
                    Log.i(TAG, "Updating bank source for existing SMS: ${existingSms.bankSource} -> $newBankSource")
                    val updatedSms = existingSms.copy(bankSource = newBankSource)
                    smsMessageRepository.updateSmsMessage(updatedSms)
                }

                latestTimestamp = maxOf(latestTimestamp, timestamp)
                val baseProgress = if (fullSync) 0.5f else 0f // Start at 50% if we did existing SMS update
                progressCallback(baseProgress + ((index + 1).toFloat() / totalMessages) * (1f - baseProgress))
                return@forEachIndexed
            }

            // Determine bank source from configurable sender filters
            val matchedFilter = enabledFilters.find { filter ->
                sender.contains(filter.filterPattern, ignoreCase = true)
            }
            val bankSource = matchedFilter?.name

            val smsMessage = SmsMessage(
                id = UUID.randomUUID().toString(),
                sender = sender,
                rawMessage = body,
                timestamp = Date(timestamp),
                bankSource = bankSource
            )

            smsMessagesToInsert.add(smsMessage)
            storedCount++
            latestTimestamp = maxOf(latestTimestamp, timestamp)
            val baseProgress = if (fullSync) 0.5f else 0f // Start at 50% if we did existing SMS update
            progressCallback(baseProgress + ((index + 1).toFloat() / totalMessages) * (1f - baseProgress))
        }

        // Batch insert all SMS messages
        if (smsMessagesToInsert.isNotEmpty()) {
            smsMessageRepository.insertSmsMessages(smsMessagesToInsert)
            Log.i(TAG, "✅ Inserted ${smsMessagesToInsert.size} SMS messages")
        }

        if (latestTimestamp > lastSyncTimestamp) {
            smsMessageRepository.updateLastSyncTimestamp(latestTimestamp)
            Log.i(TAG, "Updated sync timestamp to: ${Date(latestTimestamp)}")
        }

        Log.i(TAG, "=== SMS SYNC COMPLETED ===")
        Log.i(TAG, "Total processed: $totalMessages")
        Log.i(TAG, "Successfully stored: $storedCount")
    }

    /**
     * Update all existing SMS messages in our database with new filter matching
     * This ensures that when filters are added/changed, all existing SMS get updated
     */
    private suspend fun updateExistingSmsWithFilters(
        enabledFilters: List<SenderFilter>,
        progressCallback: (Float) -> Unit
    ) {
        Log.i(TAG, "=== UPDATING ALL EXISTING SMS WITH NEW FILTERS ===")

        // Get ALL existing SMS messages from our database
        val allExistingSms = smsMessageRepository.getAllSmsMessagesSync()
        Log.i(TAG, "Found ${allExistingSms.size} existing SMS messages to update")

        if (allExistingSms.isEmpty()) {
            Log.i(TAG, "No existing SMS messages to update")
            return
        }

        var updatedCount = 0

        allExistingSms.forEachIndexed { index, existingSms ->
            Log.d(TAG, "Updating SMS ${index + 1}/${allExistingSms.size}:")
            Log.d(TAG, "  ID: ${existingSms.id}")
            Log.d(TAG, "  Sender: '${existingSms.sender}'")
            Log.d(TAG, "  Current bank source: ${existingSms.bankSource}")

            // Re-apply filter matching to this SMS
            val matchedFilter = enabledFilters.find { filter ->
                existingSms.sender.contains(filter.filterPattern, ignoreCase = true)
            }
            val newBankSource = matchedFilter?.name

            Log.d(TAG, "  Matched filter: ${matchedFilter?.name ?: "None"}")
            Log.d(TAG, "  New bank source: $newBankSource")

            // Update if bank source changed
            if (existingSms.bankSource != newBankSource) {
                Log.i(TAG, "  ✅ UPDATING: ${existingSms.bankSource} -> $newBankSource")
                val updatedSms = existingSms.copy(bankSource = newBankSource)
                smsMessageRepository.updateSmsMessage(updatedSms)
                updatedCount++
            } else {
                Log.d(TAG, "  ⏭️ No change needed")
            }

            // Update progress
            progressCallback((index + 1).toFloat() / allExistingSms.size * 0.5f) // Use 50% of progress for this step
        }

        Log.i(TAG, "✅ Updated $updatedCount out of ${allExistingSms.size} existing SMS messages")
        Log.i(TAG, "=== EXISTING SMS UPDATE COMPLETED ===")
    }

    private fun readSmsMessages(enabledFilters: List<SenderFilter>, lastSyncTimestamp: Long): List<Triple<String, String, Long>> {
        val messages = mutableListOf<Triple<String, String, Long>>()
        val uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )

        val selection = buildSelectionString(enabledFilters, lastSyncTimestamp)
        val selectionArgs = buildSelectionArgs(enabledFilters, lastSyncTimestamp)

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

    private fun buildSelectionString(enabledFilters: List<SenderFilter>, lastSyncTimestamp: Long): String {
        if (enabledFilters.isEmpty()) {
            return "1 = 0"
        }

        val filterConditions = enabledFilters.joinToString(" OR ") { "${Telephony.Sms.ADDRESS} LIKE ?" }
        return if (lastSyncTimestamp > 0) {
            "($filterConditions) AND ${Telephony.Sms.DATE} > ?"
        } else {
            "($filterConditions)"
        }
    }

    private fun buildSelectionArgs(enabledFilters: List<SenderFilter>, lastSyncTimestamp: Long): Array<String> {
        if (enabledFilters.isEmpty()) {
            return emptyArray()
        }

        val args = enabledFilters.map { "%${it.filterPattern}%" }.toMutableList()
        if (lastSyncTimestamp > 0) {
            args.add(lastSyncTimestamp.toString())
        }
        return args.toTypedArray()
    }
}
