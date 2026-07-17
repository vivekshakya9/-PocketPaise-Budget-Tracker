package com.vsdigitallabs.pocketpaise.stdbt.data.model

data class SyncData(
    val transactions: List<Transaction> = emptyList(),
    val budgets: List<Budget> = emptyList(),
    val goals: List<SavingsGoal> = emptyList()
)
