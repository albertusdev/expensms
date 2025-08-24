package dev.albertus.expensms.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction as RoomTransaction
import dev.albertus.expensms.data.model.ApiLog
import dev.albertus.expensms.data.model.ApiLogWithTransaction
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
}
