package dev.albertus.expensms

import androidx.lifecycle.LiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TransactionRepository(private val transactionDao: TransactionDao) {
    fun getAllTransactions(): LiveData<List<Transaction>> {
        return transactionDao.getAllTransactions()
    }

    suspend fun insertTransaction(transaction: Transaction) {
        withContext(Dispatchers.IO) {
            transactionDao.insertTransaction(transaction)
        }
    }
}