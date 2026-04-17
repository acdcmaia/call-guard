package com.acdcmaia.callguard.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BlacklistDao {
    @Query("SELECT * FROM blacklist_patterns ORDER BY id DESC")
    fun getAll(): Flow<List<BlacklistPattern>>

    @Query("SELECT * FROM blacklist_patterns")
    suspend fun getAllOnce(): List<BlacklistPattern>

    @Insert
    suspend fun insert(pattern: BlacklistPattern)

    @Delete
    suspend fun delete(pattern: BlacklistPattern)

    @Update
    suspend fun update(pattern: BlacklistPattern)
}
