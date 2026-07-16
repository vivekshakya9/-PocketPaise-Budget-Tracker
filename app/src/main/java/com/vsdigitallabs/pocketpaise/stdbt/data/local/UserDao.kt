package com.vsdigitallabs.pocketpaise.stdbt.data.local

import androidx.room.*
import com.vsdigitallabs.pocketpaise.stdbt.data.model.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun getUserById(id: Long): Flow<User?>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserByIdSync(id: Long): User?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: User): Long

    @Update
    suspend fun updateUser(user: User)
}
