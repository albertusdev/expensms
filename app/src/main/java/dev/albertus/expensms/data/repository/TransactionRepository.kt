package dev.albertus.expensms.data.repository

import dev.albertus.expensms.data.local.SyncMetadataDao
import dev.albertus.expensms.data.local.TransactionDao
import dev.albertus.expensms.data.model.Transaction
import dev.albertus.expensms.data.model.TransactionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val syncMetadataDao: SyncMetadataDao
) {
    fun getAllTransactions(): Flow<List<Transaction>> {
        return transactionDao.getAllTransactionsFlow()
    }

    suspend fun insertTransaction(transaction: Transaction) {
        withContext(Dispatchers.IO) {
            transactionDao.insertTransaction(transaction)
        }
    }

    suspend fun updateTransactionStatus(ids: List<String>, status: TransactionStatus) {
        withContext(Dispatchers.IO) {
            transactionDao.updateTransactionStatus(ids, status)
        }
    }

    fun getLastSyncTimestamp(): Long {
        return syncMetadataDao.getLastSyncTimestamp() ?: 0L
    }

    suspend fun updateLastSyncTimestamp(timestamp: Long) {
        withContext(Dispatchers.IO) {
            syncMetadataDao.updateLastSyncTimestamp(timestamp)
        }
    }

    fun getIgnoredTransactions(): Flow<List<Transaction>> {
        return transactionDao.getAllTransactionsFlow(status = TransactionStatus.IGNORED)
    }
}