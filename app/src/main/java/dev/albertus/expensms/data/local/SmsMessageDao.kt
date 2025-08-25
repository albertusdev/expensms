package dev.albertus.expensms.data.local

import androidx.room.*
import dev.albertus.expensms.data.model.SmsMessage
import dev.albertus.expensms.data.model.SmsStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsMessageDao {
    @Query("SELECT * FROM sms_messages WHERE status = :status ORDER BY timestamp DESC")
    fun getAllSmsMessagesFlow(status: SmsStatus = SmsStatus.ACTIVE): Flow<List<SmsMessage>>

    @Query("SELECT * FROM sms_messages WHERE status = :status ORDER BY timestamp DESC")
    suspend fun getAllSmsMessages(status: SmsStatus = SmsStatus.ACTIVE): List<SmsMessage>

    @Query("SELECT * FROM sms_messages WHERE id = :id")
    suspend fun getSmsMessageById(id: String): SmsMessage?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSmsMessage(smsMessage: SmsMessage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSmsMessages(smsMessages: List<SmsMessage>)

    @Update
    suspend fun updateSmsMessage(smsMessage: SmsMessage)

    @Query("UPDATE sms_messages SET status = :status WHERE id IN (:ids)")
    suspend fun updateSmsMessageStatus(ids: List<String>, status: SmsStatus)

    @Query("UPDATE sms_messages SET is_forwarded = :isForwarded, forwarded_at = :forwardedAt WHERE id IN (:ids)")
    suspend fun updateForwardingStatus(ids: List<String>, isForwarded: Boolean, forwardedAt: java.util.Date?)



    @Query("SELECT COUNT(*) FROM sms_messages WHERE status = :status")
    suspend fun getSmsMessageCount(status: SmsStatus = SmsStatus.ACTIVE): Int

    @Query("SELECT * FROM sms_messages WHERE bank_source = :bankSource AND status = :status ORDER BY timestamp DESC")
    fun getSmsMessagesByBank(bankSource: String, status: SmsStatus = SmsStatus.ACTIVE): Flow<List<SmsMessage>>

    @Query("SELECT * FROM sms_messages WHERE is_forwarded = :isForwarded AND status = :status ORDER BY timestamp DESC")
    fun getSmsMessagesByForwardingStatus(isForwarded: Boolean, status: SmsStatus = SmsStatus.ACTIVE): Flow<List<SmsMessage>>

    // NEW: Hash-based duplicate detection methods
    @Query("SELECT * FROM sms_messages WHERE content_hash = :contentHash LIMIT 1")
    suspend fun getSmsMessageByContentHash(contentHash: String): SmsMessage?

    @Query("SELECT content_hash FROM sms_messages")
    suspend fun getAllContentHashes(): List<String>
}
