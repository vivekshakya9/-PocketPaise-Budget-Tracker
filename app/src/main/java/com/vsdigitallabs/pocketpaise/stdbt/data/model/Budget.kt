package com.vsdigitallabs.pocketpaise.stdbt.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val userId: Long,
    val category: String, // Can be "GLOBAL" or specific category
    val limitAmount: Double,
    val monthYear: String // MM-YYYY format
)
