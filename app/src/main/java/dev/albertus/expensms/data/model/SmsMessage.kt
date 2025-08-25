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
    @ColumnInfo(name = "bank_source") val bankSource: String? = null // e.g., "OCBC", "BCA", etc.
)

enum class SmsStatus {
    ACTIVE,
    IGNORED
}
