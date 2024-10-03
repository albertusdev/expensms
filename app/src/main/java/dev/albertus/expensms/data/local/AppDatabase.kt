package dev.albertus.expensms.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import dev.albertus.expensms.data.model.SyncMetadata
import dev.albertus.expensms.data.model.Transaction
import dev.albertus.expensms.utils.Converters

@Database(entities = [Transaction::class, SyncMetadata::class], version = 2, exportSchema = true)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun syncMetadataDao(): SyncMetadataDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .fallbackToDestructiveMigration()  // This will destroy and recreate the database if the version changes
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}