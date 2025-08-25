package dev.albertus.expensms.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import dev.albertus.expensms.data.model.ApiLog
import dev.albertus.expensms.data.model.SenderFilter
import dev.albertus.expensms.data.model.SmsMessage
import dev.albertus.expensms.data.model.SyncMetadata
import dev.albertus.expensms.data.model.Transaction
import dev.albertus.expensms.utils.Converters

@Database(entities = [Transaction::class, SmsMessage::class, SyncMetadata::class, ApiLog::class, SenderFilter::class], version = 8, exportSchema = true)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun smsMessageDao(): SmsMessageDao
    abstract fun syncMetadataDao(): SyncMetadataDao
    abstract fun apiLogDao(): ApiLogDao
    abstract fun senderFilterDao(): SenderFilterDao

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