package com.vsdigitallabs.pocketpaise.stdbt.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vsdigitallabs.pocketpaise.stdbt.data.local.AppDatabase
import com.vsdigitallabs.pocketpaise.stdbt.data.model.*
import com.vsdigitallabs.pocketpaise.stdbt.data.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val database = AppDatabase.getDatabase(applicationContext)
        val transactionDao = database.transactionDao()
        val budgetDao = database.budgetDao()
        val savingsGoalDao = database.savingsGoalDao()
        val deletedItemDao = database.deletedItemDao()

        val prefs = applicationContext.getSharedPreferences("PocketPaisePrefs", Context.MODE_PRIVATE)
        val userId = prefs.getLong("currentUserId", -1L)
        val token = prefs.getString("authToken", null)

        if (userId == -1L || token.isNullOrEmpty()) {
            return@withContext Result.success() // Not logged in, nothing to sync
        }
        
        // Ensure RetrofitClient has the token for this background process
        RetrofitClient.authToken = token

        try {
            // 1. Gather all unsynced local data
            val unsyncedTransactions = transactionDao.getUnsyncedTransactions()
            val unsyncedBudgets = budgetDao.getUnsyncedBudgets()
            val unsyncedGoals = savingsGoalDao.getUnsyncedGoals()

            val deletedItems = deletedItemDao.getAllDeletedItems()
            val deletedTransactions = deletedItems.filter { it.type == "TRANSACTION" }.map { it.uuid }
            val deletedBudgets = deletedItems.filter { it.type == "BUDGET" }.map { it.uuid }
            val deletedGoals = deletedItems.filter { it.type == "GOAL" }.map { it.uuid }

            val syncRequest = SyncData(
                transactions = unsyncedTransactions.map {
                    NetworkTransaction(
                        title = it.title, amount = it.amount, type = it.type,
                        category = it.category, timestamp = it.timestamp, notes = it.notes,
                        receiptUri = it.receiptUri, uuid = it.uuid
                    )
                },
                budgets = unsyncedBudgets.map {
                    NetworkBudget(
                        category = it.category, limitAmount = it.limitAmount,
                        monthYear = it.monthYear, uuid = it.uuid
                    )
                },
                goals = unsyncedGoals.map {
                    NetworkSavingsGoal(
                        name = it.name, targetAmount = it.targetAmount, currentAmount = it.currentAmount,
                        targetDate = it.targetDate, isCompleted = it.isCompleted, uuid = it.uuid
                    )
                },
                deleted_transactions = deletedTransactions,
                deleted_budgets = deletedBudgets,
                deleted_goals = deletedGoals
            )

            // 2. Push to server
            val pushResponse = RetrofitClient.apiService.pushSyncData(syncRequest)
            if (pushResponse.isSuccessful && pushResponse.body()?.success == true) {
                // Mark as synced locally using specific IDs
                if (unsyncedTransactions.isNotEmpty()) {
                    transactionDao.markAsSynced(unsyncedTransactions.map { it.id })
                }
                if (unsyncedBudgets.isNotEmpty()) {
                    budgetDao.markAsSynced(unsyncedBudgets.map { it.id })
                }
                if (unsyncedGoals.isNotEmpty()) {
                    savingsGoalDao.markAsSynced(unsyncedGoals.map { it.id })
                }
                
                // Clear deleted items
                deletedItemDao.clearAll()
            } else {
                return@withContext Result.retry()
            }

            // 3. Pull from server
            val pullResponse = RetrofitClient.apiService.getSyncData()
            if (pullResponse.isSuccessful && pullResponse.body()?.success == true) {
                val syncData = pullResponse.body()?.data
                if (syncData != null) {
                    // Update local DB
                    transactionDao.deleteAllSyncedForUser(userId)
                    transactionDao.insertAll(syncData.transactions.map { it.toTransaction(userId) })

                    budgetDao.deleteAllSyncedForUser(userId)
                    budgetDao.insertAll(syncData.budgets.map { it.toBudget(userId) })

                    savingsGoalDao.deleteAllSyncedForUser(userId)
                    savingsGoalDao.insertAll(syncData.goals.map { it.toSavingsGoal(userId) })
                }
            } else {
                return@withContext Result.retry()
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
