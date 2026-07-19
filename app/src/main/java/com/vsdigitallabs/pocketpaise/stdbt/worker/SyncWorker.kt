package com.vsdigitallabs.pocketpaise.stdbt.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vsdigitallabs.pocketpaise.stdbt.data.local.AppDatabase
import com.vsdigitallabs.pocketpaise.stdbt.data.model.SyncData
import com.vsdigitallabs.pocketpaise.stdbt.data.network.RetrofitClient
import kotlinx.coroutines.flow.firstOrNull

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val transactionDao = database.transactionDao()
        val budgetDao = database.budgetDao()
        val savingsGoalDao = database.savingsGoalDao()
        val userDao = database.userDao()

        val prefs = applicationContext.getSharedPreferences("pocketpaise_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("authToken", null)
        if (token != null) {
            RetrofitClient.authToken = token
        } else {
            return Result.failure()
        }

        try {
            // Find a user ID (for simple app, we can just grab the first logged-in user)
            val user = userDao.getUserByIdSync(1) // Assuming ID 1 or we need a way to pass userId
            
            // To be safe, let's just get all unsynced items across all users, 
            // since our backend groups them by user token anyway.
            val unsyncedTx = transactionDao.getUnsyncedTransactions()
            val unsyncedBudgets = budgetDao.getUnsyncedBudgets()
            val unsyncedGoals = savingsGoalDao.getUnsyncedGoals()

            if (unsyncedTx.isEmpty() && unsyncedBudgets.isEmpty() && unsyncedGoals.isEmpty()) {
                return Result.success()
            }

            val syncData = SyncData(
                transactions = unsyncedTx.map { com.vsdigitallabs.pocketpaise.stdbt.data.model.NetworkTransaction(it.serverId, it.title, it.amount, it.type, it.category, it.timestamp, it.notes, it.receiptUri, it.uuid) },
                budgets = unsyncedBudgets.map { com.vsdigitallabs.pocketpaise.stdbt.data.model.NetworkBudget(it.serverId, it.category, it.limitAmount, it.monthYear, it.uuid) },
                goals = unsyncedGoals.map { com.vsdigitallabs.pocketpaise.stdbt.data.model.NetworkSavingsGoal(it.serverId, it.name, it.targetAmount, it.currentAmount, it.targetDate, it.isCompleted, it.uuid) }
            )

            val response = RetrofitClient.apiService.pushSyncData(syncData)

            if (response.isSuccessful && response.body()?.success == true) {
                // Mark as synced
                transactionDao.markAllAsSynced()
                budgetDao.markAllAsSynced()
                savingsGoalDao.markAllAsSynced()
                return Result.success()
            } else {
                return Result.retry()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }
}
