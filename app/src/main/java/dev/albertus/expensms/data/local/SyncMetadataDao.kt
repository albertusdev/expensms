package dev.albertus.expensms.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.albertus.expensms.data.model.SyncMetadata

@Dao
interface SyncMetadataDao {
    @Query("SELECT sync_timestamp FROM sync_metadata WHERE id = 1")
    fun getLastSyncTimestamp(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun updateSyncMetadata(syncMetadata: SyncMetadata)

    @Query("INSERT OR REPLACE INTO sync_metadata (id, sync_timestamp) VALUES (1, :timestamp)")
    suspend fun updateLastSyncTimestamp(timestamp: Long)
}
