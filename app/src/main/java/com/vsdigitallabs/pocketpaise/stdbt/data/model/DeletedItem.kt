package com.vsdigitallabs.pocketpaise.stdbt.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "deleted_items")
data class DeletedItem(
    @PrimaryKey val uuid: String,
    val type: String, // "TRANSACTION", "BUDGET", "GOAL"
    val timestamp: Long = System.currentTimeMillis()
)
