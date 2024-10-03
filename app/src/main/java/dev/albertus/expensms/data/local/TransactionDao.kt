package dev.albertus.expensms.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

import dev.albertus.expensms.data.model.Transaction
import dev.albertus.expensms.data.model.TransactionStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE status = :status ORDER BY date DESC")
    fun getAllTransactionsFlow(status: TransactionStatus = TransactionStatus.ACTIVE): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Query("UPDATE transactions SET status = :status WHERE id IN (:ids)")
    suspend fun updateTransactionStatus(ids: List<String>, status: TransactionStatus)
}
