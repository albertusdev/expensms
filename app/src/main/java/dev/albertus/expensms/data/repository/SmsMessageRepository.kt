package dev.albertus.expensms.data.repository

import dev.albertus.expensms.data.local.SmsMessageDao
import dev.albertus.expensms.data.local.SyncMetadataDao
import dev.albertus.expensms.data.model.SmsMessage
import dev.albertus.expensms.data.model.SmsStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsMessageRepository @Inject constructor(
    private val smsMessageDao: SmsMessageDao,
    private val syncMetadataDao: SyncMetadataDao
) {
    fun getAllSmsMessages(): Flow<List<SmsMessage>> {
        return smsMessageDao.getAllSmsMessagesFlow()
    }

    suspend fun getSmsMessageById(id: String): SmsMessage? {
        return withContext(Dispatchers.IO) {
            smsMessageDao.getSmsMessageById(id)
        }
    }

    suspend fun insertSmsMessage(smsMessage: SmsMessage) {
        withContext(Dispatchers.IO) {
            smsMessageDao.insertSmsMessage(smsMessage)
        }
    }

    suspend fun insertSmsMessages(smsMessages: List<SmsMessage>) {
        withContext(Dispatchers.IO) {
            smsMessageDao.insertSmsMessages(smsMessages)
        }
    }

    suspend fun getAllSmsMessagesSync(): List<SmsMessage> {
        return smsMessageDao.getAllSmsMessages()
    }

    suspend fun updateSmsMessage(smsMessage: SmsMessage) {
        withContext(Dispatchers.IO) {
            smsMessageDao.updateSmsMessage(smsMessage)
        }
    }

    suspend fun updateSmsMessageStatus(ids: List<String>, status: SmsStatus) {
        withContext(Dispatchers.IO) {
            smsMessageDao.updateSmsMessageStatus(ids, status)
        }
    }

    suspend fun updateForwardingStatus(ids: List<String>, isForwarded: Boolean, forwardedAt: java.util.Date? = null) {
        withContext(Dispatchers.IO) {
            smsMessageDao.updateForwardingStatus(ids, isForwarded, forwardedAt)
        }
    }



    suspend fun getSmsMessageCount(): Int {
        return withContext(Dispatchers.IO) {
            smsMessageDao.getSmsMessageCount()
        }
    }

    fun getSmsMessagesByBank(bankSource: String): Flow<List<SmsMessage>> {
        return smsMessageDao.getSmsMessagesByBank(bankSource)
    }

    fun getSmsMessagesByForwardingStatus(isForwarded: Boolean): Flow<List<SmsMessage>> {
        return smsMessageDao.getSmsMessagesByForwardingStatus(isForwarded)
    }

    fun getLastSyncTimestamp(): Long {
        return syncMetadataDao.getLastSyncTimestamp() ?: 0L
    }

    suspend fun updateLastSyncTimestamp(timestamp: Long) {
        withContext(Dispatchers.IO) {
            syncMetadataDao.updateLastSyncTimestamp(timestamp)
        }
    }
}
