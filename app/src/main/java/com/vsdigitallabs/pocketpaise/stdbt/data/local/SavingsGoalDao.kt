package com.vsdigitallabs.pocketpaise.stdbt.data.local

import androidx.room.*
import com.vsdigitallabs.pocketpaise.stdbt.data.model.SavingsGoal
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsGoalDao {
    @Query("SELECT * FROM savings_goals WHERE userId = :userId ORDER BY targetDate ASC")
    fun getSavingsGoals(userId: Long): Flow<List<SavingsGoal>>

    @Query("SELECT * FROM savings_goals WHERE id = :id LIMIT 1")
    suspend fun getGoalByIdSync(id: Long): SavingsGoal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: SavingsGoal): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(goals: List<SavingsGoal>)

    @Query("DELETE FROM savings_goals WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: Long)

    @Query("DELETE FROM savings_goals WHERE userId = :userId AND isSynced = 1")
    suspend fun deleteAllSyncedForUser(userId: Long)

    @Query("SELECT * FROM savings_goals WHERE isSynced = 0 AND userId = :userId")
    suspend fun getUnsyncedGoals(userId: Long): List<SavingsGoal>

    @Query("UPDATE savings_goals SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Long>)

    @Query("UPDATE savings_goals SET isSynced = 1 WHERE uuid = :uuid")
    suspend fun markSyncedByUuid(uuid: String)

    @Update
    suspend fun updateGoal(goal: SavingsGoal)

    @Delete
    suspend fun deleteGoal(goal: SavingsGoal)

    @Query("DELETE FROM savings_goals WHERE id = :id")
    suspend fun deleteGoalById(id: Long)
}
