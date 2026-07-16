package com.vsdigitallabs.pocketpaise.stdbt.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val email: String,
    val mobileNumber: String,
    val collegeName: String,
    val city: String,
    val passwordHash: String,
    val profilePicUri: String? = null,
    val pocketMoneyLimit: Double = 5000.0,
    val currentSavings: Double = 0.0
)
