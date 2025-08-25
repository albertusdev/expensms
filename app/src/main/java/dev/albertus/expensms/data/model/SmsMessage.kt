package dev.albertus.expensms.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "sms_messages")
data class SmsMessage(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "sender") val sender: String,
    @ColumnInfo(name = "raw_message") val rawMessage: String,
    @ColumnInfo(name = "timestamp") val timestamp: Date,
    @ColumnInfo(name = "status") val status: SmsStatus = SmsStatus.ACTIVE,
    @ColumnInfo(name = "is_forwarded") val isForwarded: Boolean = false,
    @ColumnInfo(name = "forwarded_at") val forwardedAt: Date? = null,
    @ColumnInfo(name = "bank_source") val bankSource: String? = null, // e.g., "OCBC", "BCA", etc.
    @ColumnInfo(name = "content_hash") val contentHash: String // NEW: Stable hash for duplicate detection
) {
    companion object {
        /**
         * Generate a stable hash for SMS duplicate detection
         * Based on: time window + sender + raw message content
         *
         * Uses a 5-second time window to handle timing differences between
         * background processing and sync operations.
         */
        fun generateContentHash(timestamp: Long, sender: String, rawMessage: String): String {
            // Normalize timestamp to 5-second windows to handle timing differences
            // This ensures SMS received within 5 seconds get the same hash
            val timeWindow = 5000L // 5 seconds
            val normalizedTimestamp = (timestamp / timeWindow) * timeWindow

            val content = "$normalizedTimestamp|$sender|$rawMessage"
            return content.hashCode().toString()
        }

        /**
         * Create SmsMessage with auto-generated content hash
         */
        fun create(
            id: String,
            sender: String,
            rawMessage: String,
            timestamp: Date,
            status: SmsStatus = SmsStatus.ACTIVE,
            isForwarded: Boolean = false,
            forwardedAt: Date? = null,
            bankSource: String? = null
        ): SmsMessage {
            val contentHash = generateContentHash(timestamp.time, sender, rawMessage)
            return SmsMessage(
                id = id,
                sender = sender,
                rawMessage = rawMessage,
                timestamp = timestamp,
                status = status,
                isForwarded = isForwarded,
                forwardedAt = forwardedAt,
                bankSource = bankSource,
                contentHash = contentHash
            )
        }
    }
}

enum class SmsStatus {
    ACTIVE,
    IGNORED
}
