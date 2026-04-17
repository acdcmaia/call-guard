package com.acdcmaia.callguard.data

import com.acdcmaia.callguard.data.db.AppDatabase
import com.acdcmaia.callguard.data.db.BlacklistPattern
import com.acdcmaia.callguard.data.db.RecentCall
import kotlinx.coroutines.flow.Flow

class CallRepository(private val db: AppDatabase) {
    val blacklistPatterns: Flow<List<BlacklistPattern>> = db.blacklistDao().getAll()
    val recentCalls: Flow<List<RecentCall>> = db.recentCallDao().getAll()

    suspend fun addPattern(pattern: BlacklistPattern) = db.blacklistDao().insert(pattern)
    suspend fun deletePattern(pattern: BlacklistPattern) = db.blacklistDao().delete(pattern)
    suspend fun updatePattern(pattern: BlacklistPattern) = db.blacklistDao().update(pattern)

    suspend fun recordCall(call: RecentCall) = db.recentCallDao().insert(call)

    suspend fun getRecentCallsSince(number: String, since: Long): List<RecentCall> =
        db.recentCallDao().getCallsSince(number, since)

    suspend fun getAllPatternsOnce(): List<BlacklistPattern> =
        db.blacklistDao().getAllOnce()

    companion object {
        @Volatile private var instance: CallRepository? = null

        fun getInstance(db: AppDatabase): CallRepository =
            instance ?: synchronized(this) {
                instance ?: CallRepository(db).also { instance = it }
            }
    }
}
