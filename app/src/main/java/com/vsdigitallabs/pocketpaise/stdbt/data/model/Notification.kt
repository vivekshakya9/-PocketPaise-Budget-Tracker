package com.vsdigitallabs.pocketpaise.stdbt.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class Notification(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val userId: Long,
    val title: String,
    val message: String,
    val timestamp: Long,
    val isRead: Boolean = false,
    val type: String // "BUDGET_ALERT", "SAVINGS_REMINDER", "GENERAL", "PROMOTION", "INFO"
)
