package dev.albertus.expensms.data.local

import androidx.room.*
import dev.albertus.expensms.data.model.SenderFilter
import kotlinx.coroutines.flow.Flow

@Dao
interface SenderFilterDao {
    @Query("SELECT * FROM sender_filters ORDER BY name ASC")
    fun getAllSenderFiltersFlow(): Flow<List<SenderFilter>>

    @Query("SELECT * FROM sender_filters WHERE is_enabled = 1 ORDER BY name ASC")
    fun getEnabledSenderFiltersFlow(): Flow<List<SenderFilter>>

    @Query("SELECT * FROM sender_filters")
    suspend fun getAllSenderFilters(): List<SenderFilter>

    @Query("SELECT * FROM sender_filters WHERE is_enabled = 1")
    suspend fun getEnabledSenderFilters(): List<SenderFilter>

    @Query("SELECT * FROM sender_filters WHERE id = :id")
    suspend fun getSenderFilterById(id: String): SenderFilter?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSenderFilter(senderFilter: SenderFilter)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSenderFilters(senderFilters: List<SenderFilter>)

    @Update
    suspend fun updateSenderFilter(senderFilter: SenderFilter)

    @Query("UPDATE sender_filters SET is_enabled = :isEnabled, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateSenderFilterEnabled(id: String, isEnabled: Boolean, updatedAt: java.util.Date)

    @Delete
    suspend fun deleteSenderFilter(senderFilter: SenderFilter)

    @Query("DELETE FROM sender_filters WHERE id = :id")
    suspend fun deleteSenderFilterById(id: String)

    @Query("DELETE FROM sender_filters")
    suspend fun deleteAllSenderFilters()

    @Query("SELECT COUNT(*) FROM sender_filters")
    suspend fun getSenderFilterCount(): Int
}
