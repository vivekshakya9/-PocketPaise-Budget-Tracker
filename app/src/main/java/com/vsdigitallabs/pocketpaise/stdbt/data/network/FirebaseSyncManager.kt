package com.vsdigitallabs.pocketpaise.stdbt.data.network

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.vsdigitallabs.pocketpaise.stdbt.data.model.Transaction
import com.vsdigitallabs.pocketpaise.stdbt.data.model.Budget
import com.vsdigitallabs.pocketpaise.stdbt.data.model.SavingsGoal
import kotlinx.coroutines.tasks.await

object FirebaseSyncManager {
    private val database = FirebaseDatabase.getInstance("https://pocketpaise-b7c71-default-rtdb.asia-southeast1.firebasedatabase.app")

    // --- TRANSACTIONS ---
    
    fun saveTransaction(userId: Long, transaction: Transaction) {
        val ref = database.getReference("users/$userId/transactions/${transaction.uuid}")
        val map = mapOf(
            "title" to transaction.title,
            "amount" to transaction.amount,
            "type" to transaction.type,
            "category" to transaction.category,
            "timestamp" to transaction.timestamp,
            "notes" to transaction.notes,
            "uuid" to transaction.uuid
        )
        ref.setValue(map)
    }

    fun deleteTransaction(userId: Long, uuid: String) {
        database.getReference("users/$userId/transactions/$uuid").removeValue()
    }

    suspend fun getTransactions(userId: Long): List<Transaction> {
        val snapshot = database.getReference("users/$userId/transactions").get().await()
        val list = mutableListOf<Transaction>()
        for (child in snapshot.children) {
            try {
                // Handle both Double and Long returns safely
                val title = child.child("title").getValue(String::class.java) ?: ""
                val amount = (child.child("amount").value as? Number)?.toDouble() ?: 0.0
                val type = child.child("type").getValue(String::class.java) ?: "EXPENSE"
                val category = child.child("category").getValue(String::class.java) ?: "General"
                val timestamp = (child.child("timestamp").value as? Number)?.toLong() ?: 0L
                val notes = child.child("notes").getValue(String::class.java) ?: ""
                val uuid = child.child("uuid").getValue(String::class.java) ?: child.key ?: ""

                list.add(
                    Transaction(
                        userId = userId,
                        title = title,
                        amount = amount,
                        type = type,
                        category = category,
                        timestamp = timestamp,
                        notes = notes,
                        uuid = uuid,
                        isSynced = true
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return list
    }

    // --- BUDGETS ---

    fun saveBudget(userId: Long, budget: Budget) {
        val ref = database.getReference("users/$userId/budgets/${budget.uuid}")
        val map = mapOf(
            "category" to budget.category,
            "limitAmount" to budget.limitAmount,
            "monthYear" to budget.monthYear,
            "uuid" to budget.uuid
        )
        ref.setValue(map)
    }

    fun deleteBudget(userId: Long, uuid: String) {
        database.getReference("users/$userId/budgets/$uuid").removeValue()
    }

    suspend fun getBudgets(userId: Long): List<Budget> {
        val snapshot = database.getReference("users/$userId/budgets").get().await()
        val list = mutableListOf<Budget>()
        for (child in snapshot.children) {
            try {
                val category = child.child("category").getValue(String::class.java) ?: "General"
                val limitAmount = (child.child("limitAmount").value as? Number)?.toDouble() ?: 0.0
                val monthYear = child.child("monthYear").getValue(String::class.java)
                val uuid = child.child("uuid").getValue(String::class.java) ?: child.key ?: ""

                list.add(
                    Budget(
                        userId = userId,
                        category = category,
                        limitAmount = limitAmount,
                        monthYear = monthYear,
                        uuid = uuid,
                        isSynced = true
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return list
    }

    // --- GOALS ---

    fun saveGoal(userId: Long, goal: SavingsGoal) {
        val ref = database.getReference("users/$userId/goals/${goal.uuid}")
        val map = mapOf(
            "name" to goal.name,
            "targetAmount" to goal.targetAmount,
            "currentAmount" to goal.currentAmount,
            "targetDate" to goal.targetDate,
            "isCompleted" to goal.isCompleted,
            "uuid" to goal.uuid
        )
        ref.setValue(map)
    }

    fun deleteGoal(userId: Long, uuid: String) {
        database.getReference("users/$userId/goals/$uuid").removeValue()
    }

    suspend fun getGoals(userId: Long): List<SavingsGoal> {
        val snapshot = database.getReference("users/$userId/goals").get().await()
        val list = mutableListOf<SavingsGoal>()
        for (child in snapshot.children) {
            try {
                val name = child.child("name").getValue(String::class.java) ?: ""
                val targetAmount = (child.child("targetAmount").value as? Number)?.toDouble() ?: 0.0
                val currentAmount = (child.child("currentAmount").value as? Number)?.toDouble() ?: 0.0
                val targetDate = (child.child("targetDate").value as? Number)?.toLong() ?: 0L
                val isCompleted = child.child("isCompleted").getValue(Boolean::class.java) ?: false
                val uuid = child.child("uuid").getValue(String::class.java) ?: child.key ?: ""

                list.add(
                    SavingsGoal(
                        userId = userId,
                        name = name,
                        targetAmount = targetAmount,
                        currentAmount = currentAmount,
                        targetDate = targetDate,
                        isCompleted = isCompleted,
                        uuid = uuid,
                        isSynced = true
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return list
    }
}
