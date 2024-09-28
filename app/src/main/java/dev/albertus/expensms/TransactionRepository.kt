package dev.albertus.expensms

class TransactionRepository(private val transactionDao: TransactionDao) {
    suspend fun getAllTransactions(): List<Transaction> {
        return transactionDao.getAllTransactions()
    }

    suspend fun insertTransaction(transaction: Transaction) {
        transactionDao.insertTransaction(transaction)
    }
}