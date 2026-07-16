package com.vsdigitallabs.pocketpaise.stdbt.data.local

import androidx.room.*
import com.vsdigitallabs.pocketpaise.stdbt.data.model.SavingsGoal
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsGoalDao {
    @Query("SELECT * FROM savings_goals WHERE userId = :userId ORDER BY targetDate ASC")
    fun getSavingsGoals(userId: Long): Flow<List<SavingsGoal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: SavingsGoal): Long

    @Update
    suspend fun updateGoal(goal: SavingsGoal)

    @Delete
    suspend fun deleteGoal(goal: SavingsGoal)

    @Query("DELETE FROM savings_goals WHERE id = :id")
    suspend fun deleteGoalById(id: Long)
}
