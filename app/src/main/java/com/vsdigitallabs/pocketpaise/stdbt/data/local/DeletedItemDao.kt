package com.vsdigitallabs.pocketpaise.stdbt.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DeletedItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: com.vsdigitallabs.pocketpaise.stdbt.data.model.DeletedItem)

    @Query("SELECT * FROM deleted_items WHERE type = :type")
    suspend fun getDeletedItemsByType(type: String): List<com.vsdigitallabs.pocketpaise.stdbt.data.model.DeletedItem>

    @Query("SELECT * FROM deleted_items")
    suspend fun getAllDeletedItems(): List<com.vsdigitallabs.pocketpaise.stdbt.data.model.DeletedItem>

    @Query("DELETE FROM deleted_items WHERE uuid IN (:uuids)")
    suspend fun deleteByUuids(uuids: List<String>)

    @Query("DELETE FROM deleted_items")
    suspend fun clearAll()
}
