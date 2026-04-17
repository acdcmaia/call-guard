package com.acdcmaia.callguard.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentCallDao {
    @Query("SELECT * FROM recent_calls ORDER BY timestamp DESC LIMIT 100")
    fun getAll(): Flow<List<RecentCall>>

    @Query("SELECT * FROM recent_calls WHERE number = :number AND timestamp > :since")
    suspend fun getCallsSince(number: String, since: Long): List<RecentCall>

    @Insert
    suspend fun insert(call: RecentCall)

    @Query("DELETE FROM recent_calls WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)
}
