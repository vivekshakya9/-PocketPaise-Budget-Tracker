package com.vsdigitallabs.pocketpaise.stdbt.data.local

import androidx.room.*
import com.vsdigitallabs.pocketpaise.stdbt.data.model.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllTransactions(userId: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE userId = :userId AND type = :type ORDER BY timestamp DESC")
    fun getTransactionsByType(userId: Long, type: String): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE userId = :userId AND category = :category ORDER BY timestamp DESC")
    fun getTransactionsByCategory(userId: Long, category: String): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE userId = :userId AND timestamp >= :start AND timestamp <= :end ORDER BY timestamp DESC")
    fun getTransactionsByDateRange(userId: Long, start: Long, end: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE userId = :userId AND (title LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%') ORDER BY timestamp DESC")
    fun searchTransactions(userId: Long, query: String): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<Transaction>)

    @Query("DELETE FROM transactions WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: Long)

    @Query("DELETE FROM transactions WHERE userId = :userId AND isSynced = 1")
    suspend fun deleteAllSyncedForUser(userId: Long)

    @Query("SELECT * FROM transactions WHERE isSynced = 0")
    suspend fun getUnsyncedTransactions(): List<Transaction>

    @Query("UPDATE transactions SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Long>)

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)
}
