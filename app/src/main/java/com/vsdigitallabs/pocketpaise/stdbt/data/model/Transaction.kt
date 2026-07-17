package com.vsdigitallabs.pocketpaise.stdbt.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val userId: Long,
    val title: String,
    val amount: Double,
    val type: String, // "INCOME" or "EXPENSE"
    val category: String, // Food, Travel, Recharge, etc.
    val timestamp: Long,
    val notes: String = "",
    val receiptUri: String? = null,
    val serverId: Long = 0L,
    val isSynced: Boolean = false
)
