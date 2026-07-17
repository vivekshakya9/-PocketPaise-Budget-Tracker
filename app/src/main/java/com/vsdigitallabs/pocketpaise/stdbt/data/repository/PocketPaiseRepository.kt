package com.vsdigitallabs.pocketpaise.stdbt.data.repository

import com.vsdigitallabs.pocketpaise.stdbt.data.local.*
import com.vsdigitallabs.pocketpaise.stdbt.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.Calendar

class PocketPaiseRepository(
    private val userDao: UserDao,
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao,
    private val savingsGoalDao: SavingsGoalDao,
    private val notificationDao: NotificationDao
) {
    // User functions
    suspend fun getUserByEmail(email: String): User? = userDao.getUserByEmail(email)
    fun getUserById(id: Long): Flow<User?> = userDao.getUserById(id)
    suspend fun getUserByIdSync(id: Long): User? = userDao.getUserByIdSync(id)
    suspend fun insertUser(user: User): Long = userDao.insertUser(user)
    suspend fun updateUser(user: User) = userDao.updateUser(user)

    // Transactions with Balance Adjustment and Auto Budget Alert Notification!
    fun getAllTransactions(userId: Long): Flow<List<Transaction>> = transactionDao.getAllTransactions(userId)
    fun getTransactionsByType(userId: Long, type: String): Flow<List<Transaction>> = transactionDao.getTransactionsByType(userId, type)
    fun getTransactionsByCategory(userId: Long, category: String): Flow<List<Transaction>> = transactionDao.getTransactionsByCategory(userId, category)
    fun getTransactionsByDateRange(userId: Long, start: Long, end: Long): Flow<List<Transaction>> = transactionDao.getTransactionsByDateRange(userId, start, end)
    fun searchTransactions(userId: Long, query: String): Flow<List<Transaction>> = transactionDao.searchTransactions(userId, query)

    suspend fun insertTransaction(transaction: Transaction): Long {
        try {
            val payload = mapOf(
                "amount" to transaction.amount,
                "title" to transaction.title,
                "notes" to transaction.notes,
                "category" to transaction.category
            )
            if (transaction.type == "INCOME") {
                com.vsdigitallabs.pocketpaise.stdbt.data.network.RetrofitClient.apiService.createIncome(payload)
            } else {
                com.vsdigitallabs.pocketpaise.stdbt.data.network.RetrofitClient.apiService.createExpense(payload)
            }
        } catch (e: Exception) {
            e.printStackTrace() // Best effort offline-first sync
        }
        val id = transactionDao.insertTransaction(transaction)
        
        // Adjust user balance
        val user = userDao.getUserByIdSync(transaction.userId)
        if (user != null) {
            val balanceDelta = if (transaction.type == "INCOME") transaction.amount else -transaction.amount
            val updatedUser = user.copy(
                pocketMoneyLimit = user.pocketMoneyLimit + balanceDelta
            )
            userDao.updateUser(updatedUser)
        }

        // Auto Budget Alert Verification
        if (transaction.type == "EXPENSE") {
            val cal = Calendar.getInstance()
            cal.timeInMillis = transaction.timestamp
            val month = String.format("%02d", cal.get(Calendar.MONTH) + 1)
            val year = cal.get(Calendar.YEAR)
            val monthYear = "$month-$year"

            // Check specific category budget
            val catBudget = budgetDao.getBudgetByCategory(transaction.userId, transaction.category, monthYear)
            if (catBudget != null) {
                // Sum current category expenses for this month
                val allTxList = transactionDao.getAllTransactions(transaction.userId).firstOrNull() ?: emptyList()
                val monthCatExpenses = allTxList.filter {
                    val txCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                    val txMonth = String.format("%02d", txCal.get(Calendar.MONTH) + 1)
                    val txYear = txCal.get(Calendar.YEAR)
                    it.type == "EXPENSE" && it.category == transaction.category && "$txMonth-$txYear" == monthYear
                }.sumOf { it.amount }

                if (monthCatExpenses > catBudget.limitAmount) {
                    notificationDao.insertNotification(
                        Notification(
                            userId = transaction.userId,
                            title = "Budget Overrun: ${transaction.category}",
                            message = "You have exceeded your ${transaction.category} budget of ₹${catBudget.limitAmount} for $monthYear! Current spent: ₹$monthCatExpenses",
                            timestamp = System.currentTimeMillis(),
                            type = "BUDGET_ALERT"
                        )
                    )
                } else if (monthCatExpenses > catBudget.limitAmount * 0.8) {
                    notificationDao.insertNotification(
                        Notification(
                            userId = transaction.userId,
                            title = "Budget Warning: ${transaction.category}",
                            message = "You have spent 80% of your ${transaction.category} budget of ₹${catBudget.limitAmount} for $monthYear! Current spent: ₹$monthCatExpenses",
                            timestamp = System.currentTimeMillis(),
                            type = "BUDGET_ALERT"
                        )
                    )
                }
            }
        }

        return id
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        try {
            if (transaction.type == "INCOME") {
                com.vsdigitallabs.pocketpaise.stdbt.data.network.RetrofitClient.apiService.deleteIncome(transaction.id)
            } else {
                com.vsdigitallabs.pocketpaise.stdbt.data.network.RetrofitClient.apiService.deleteExpense(transaction.id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        transactionDao.deleteTransaction(transaction)
        
        // Re-adjust user balance
        val user = userDao.getUserByIdSync(transaction.userId)
        if (user != null) {
            val balanceDelta = if (transaction.type == "INCOME") -transaction.amount else transaction.amount
            val updatedUser = user.copy(
                pocketMoneyLimit = user.pocketMoneyLimit + balanceDelta
            )
            userDao.updateUser(updatedUser)
        }
    }

    suspend fun updateTransaction(oldTransaction: Transaction, newTransaction: Transaction) {
        transactionDao.updateTransaction(newTransaction)
        
        val user = userDao.getUserByIdSync(newTransaction.userId)
        if (user != null) {
            // Revert old transaction balance adjustment
            val oldDelta = if (oldTransaction.type == "INCOME") -oldTransaction.amount else oldTransaction.amount
            // Apply new transaction balance adjustment
            val newDelta = if (newTransaction.type == "INCOME") newTransaction.amount else -newTransaction.amount
            val updatedUser = user.copy(
                pocketMoneyLimit = user.pocketMoneyLimit + oldDelta + newDelta
            )
            userDao.updateUser(updatedUser)
        }
    }

    // Budgets
    fun getBudgetsForMonth(userId: Long, monthYear: String): Flow<List<Budget>> = budgetDao.getBudgetsForMonth(userId, monthYear)
    suspend fun insertBudget(budget: Budget): Long {
        try {
            val payload = mapOf(
                "limitAmount" to budget.limitAmount,
                "category" to budget.category,
                "monthYear" to budget.monthYear,
                "isMonthly" to budget.isMonthly
            )
            com.vsdigitallabs.pocketpaise.stdbt.data.network.RetrofitClient.apiService.createBudget(payload)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return budgetDao.insertBudget(budget)
    }
    suspend fun updateBudget(budget: Budget) = budgetDao.updateBudget(budget)
    suspend fun deleteBudgetById(id: Long) {
        try {
            com.vsdigitallabs.pocketpaise.stdbt.data.network.RetrofitClient.apiService.deleteBudget(id)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        budgetDao.deleteBudgetById(id)
    }

    // Savings Goals
    fun getSavingsGoals(userId: Long): Flow<List<SavingsGoal>> = savingsGoalDao.getSavingsGoals(userId)
    suspend fun insertGoal(goal: SavingsGoal): Long {
        try {
            val payload = mapOf(
                "name" to goal.name,
                "targetAmount" to goal.targetAmount,
                "currentAmount" to goal.currentAmount,
                "targetDate" to goal.targetDate,
                "isCompleted" to goal.isCompleted
            )
            com.vsdigitallabs.pocketpaise.stdbt.data.network.RetrofitClient.apiService.createGoal(payload)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return savingsGoalDao.insertGoal(goal)
    }
    suspend fun updateGoal(goal: SavingsGoal) {
        savingsGoalDao.updateGoal(goal)
        
        // If completed, trigger a notification!
        if (goal.isCompleted) {
            notificationDao.insertNotification(
                Notification(
                    userId = goal.userId,
                    title = "Savings Goal Achieved! 🎉",
                    message = "Congratulations! You have completed your goal to save ₹${goal.targetAmount} for '${goal.name}'!",
                    timestamp = System.currentTimeMillis(),
                    type = "SAVINGS_REMINDER"
                )
            )
        }
    }
    suspend fun deleteGoalById(id: Long) {
        try {
            com.vsdigitallabs.pocketpaise.stdbt.data.network.RetrofitClient.apiService.deleteGoal(id)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        savingsGoalDao.deleteGoalById(id)
    }

    // Notifications
    fun getAllNotifications(userId: Long): Flow<List<Notification>> = notificationDao.getAllNotifications(userId)
    fun getUnreadCount(userId: Long): Flow<Int> = notificationDao.getUnreadCount(userId)
    suspend fun insertNotification(notification: Notification): Long = notificationDao.insertNotification(notification)
    suspend fun markAllAsRead(userId: Long) = notificationDao.markAllAsRead(userId)
    suspend fun markAsRead(id: Long) = notificationDao.markAsRead(id)
    suspend fun deleteNotificationById(id: Long) = notificationDao.deleteNotificationById(id)
    suspend fun clearAllNotifications(userId: Long) = notificationDao.clearAllNotifications(userId)
}
