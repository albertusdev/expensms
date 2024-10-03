package dev.albertus.expensms.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_metadata")
data class SyncMetadata(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "sync_timestamp") val syncTimestamp: Long
)
