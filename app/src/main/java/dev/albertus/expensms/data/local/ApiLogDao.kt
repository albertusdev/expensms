package dev.albertus.expensms.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction as RoomTransaction
import dev.albertus.expensms.data.model.ApiLog
import dev.albertus.expensms.data.model.ApiLogWithTransaction
import dev.albertus.expensms.data.model.ApiLogWithSmsMessage
import dev.albertus.expensms.data.model.ApiLogWithRelations
import kotlinx.coroutines.flow.Flow

@Dao
interface ApiLogDao {

    @RoomTransaction
    @Query("SELECT * FROM api_logs ORDER BY timestamp DESC")
    fun getAllApiLogsWithTransactions(): Flow<List<ApiLogWithTransaction>>

    @RoomTransaction
    @Query("SELECT * FROM api_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentApiLogsWithTransactions(limit: Int = 100): Flow<List<ApiLogWithTransaction>>

    @Insert
    suspend fun insertApiLog(apiLog: ApiLog)

    @Query("DELETE FROM api_logs WHERE timestamp < :cutoffDate")
    suspend fun deleteOldLogs(cutoffDate: Long)

    @Query("DELETE FROM api_logs")
    suspend fun clearAllLogs()

    @Query("SELECT COUNT(*) FROM api_logs")
    suspend fun getLogCount(): Int

    // NEW: Methods for SMS message relationships
    @RoomTransaction
    @Query("SELECT * FROM api_logs WHERE sms_message_id = :smsMessageId ORDER BY timestamp DESC")
    fun getApiLogsBySmsMessageId(smsMessageId: String): Flow<List<ApiLogWithSmsMessage>>

    @RoomTransaction
    @Query("SELECT * FROM api_logs WHERE sms_message_id = :smsMessageId ORDER BY timestamp DESC")
    suspend fun getApiLogsBySmsMessageIdSync(smsMessageId: String): List<ApiLogWithSmsMessage>

    @RoomTransaction
    @Query("SELECT * FROM api_logs ORDER BY timestamp DESC")
    fun getAllApiLogsWithRelations(): Flow<List<ApiLogWithRelations>>

    @RoomTransaction
    @Query("SELECT * FROM api_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentApiLogsWithRelations(limit: Int = 100): Flow<List<ApiLogWithRelations>>
}
