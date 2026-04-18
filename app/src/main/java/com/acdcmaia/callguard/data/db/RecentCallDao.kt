package com.acdcmaia.callguard.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentCallDao {
    @Query("SELECT * FROM recent_calls ORDER BY timestamp DESC LIMIT 100")
    fun getAll(): Flow<List<RecentCall>>

    @Query("SELECT * FROM recent_calls WHERE number = :number AND timestamp > :since LIMIT 1")
    suspend fun getCallsSince(number: String, since: Long): List<RecentCall>

    @Insert
    suspend fun insert(call: RecentCall)

    @Query("SELECT * FROM recent_calls ORDER BY timestamp DESC LIMIT 100")
    suspend fun getAllOnce(): List<RecentCall>

    @Query("DELETE FROM recent_calls WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)

    // Conta apenas bloqueadas dentro das 100 mais recentes — consistente com o histórico visível
    @Query("SELECT COUNT(*) FROM (SELECT blockReason FROM recent_calls ORDER BY timestamp DESC LIMIT 100) WHERE blockReason IS NOT NULL")
    fun countBlockedFlow(): Flow<Long>

    @Query("SELECT COUNT(*) FROM (SELECT blockReason FROM recent_calls ORDER BY timestamp DESC LIMIT 100) WHERE blockReason IS NOT NULL")
    suspend fun countBlockedOnce(): Long
}
