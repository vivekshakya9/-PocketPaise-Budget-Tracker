package com.vsdigitallabs.pocketpaise.stdbt.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vsdigitallabs.pocketpaise.stdbt.data.local.AppDatabase
import com.vsdigitallabs.pocketpaise.stdbt.data.model.Notification
import kotlinx.coroutines.flow.firstOrNull
import java.util.Calendar

class BudgetAlertWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val budgetDao = database.budgetDao()
        val transactionDao = database.transactionDao()
        val notificationDao = database.notificationDao()
        val userDao = database.userDao()

        try {
            // Find a logged in user 
            val user = userDao.getUserByIdSync(1) ?: return Result.success() // Or fetch dynamically based on prefs

            val cal = Calendar.getInstance()
            val monthYear = "${String.format("%02d", cal.get(Calendar.MONTH) + 1)}-${cal.get(Calendar.YEAR)}"
            
            val budgets = budgetDao.getBudgetsForMonth(user.id, monthYear).firstOrNull() ?: emptyList()
            
            // Current month bounds
            val startOfMonth = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
            }.timeInMillis

            val transactions = transactionDao.getTransactionsByDateRange(user.id, startOfMonth, System.currentTimeMillis()).firstOrNull() ?: emptyList()
            val expenses = transactions.filter { it.type == "EXPENSE" }
            
            budgets.forEach { budget ->
                val categoryExpenses = expenses.filter { it.category == budget.category }.sumOf { it.amount }
                val limit = budget.limitAmount
                val percentage = if (limit > 0) (categoryExpenses / limit) * 100 else 0.0

                if (percentage >= 90.0) {
                    // Alert near limit
                    notificationDao.insertNotification(
                        Notification(
                            userId = user.id,
                            title = "Budget Alert 🚨",
                            message = "You have used ${percentage.toInt()}% of your budget for ${budget.category}. Slow down on expenses!",
                            timestamp = System.currentTimeMillis(),
                            type = "ALERT"
                        )
                    )
                }
            }

            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure()
        }
    }
}
