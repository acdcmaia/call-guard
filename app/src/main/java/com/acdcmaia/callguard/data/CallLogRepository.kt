package com.acdcmaia.callguard.data

import android.content.Context
import android.provider.CallLog
import com.acdcmaia.callguard.data.db.AppDatabase
import com.acdcmaia.callguard.data.db.BlockReason
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

class CallLogRepository(private val context: Context, private val db: AppDatabase) {

    suspend fun getMergedHistory(): List<CallHistoryItem> = withContext(Dispatchers.IO) {
        val systemCalls = readSystemCallLog()
        val patterns = db.blacklistDao().getAllOnce()
        val roomCalls = db.recentCallDao().getAllOnce()

        val matchedRoomIds = mutableSetOf<Long>()

        val systemItems = systemCalls.map { call ->
            val digits = call.number.filter { it.isDigit() }
            val matchedPattern = patterns.firstOrNull { p -> digits.contains(p.pattern) }
            val appCall = roomCalls.firstOrNull { a ->
                a.number == call.number && abs(a.timestamp - call.timestamp) < 60_000L
            }
            if (appCall != null) matchedRoomIds.add(appCall.id)
            CallHistoryItem(
                number = call.number,
                timestamp = call.timestamp,
                callType = call.type,
                isBlacklisted = matchedPattern != null,
                matchedPattern = matchedPattern?.pattern,
                matchedPatternLabel = matchedPattern?.label?.takeIf { it.isNotBlank() }
                    ?: matchedPattern?.pattern,
                interceptedByApp = appCall != null,
                blockReason = appCall?.blockReason,
                appOnly = false
            )
        }

        val roomOnlyItems = roomCalls
            .filter { it.id !in matchedRoomIds }
            .map { appCall ->
                val digits = appCall.number.filter { it.isDigit() }
                val matchedPattern = patterns.firstOrNull { p -> digits.contains(p.pattern) }
                CallHistoryItem(
                    number = appCall.number,
                    timestamp = appCall.timestamp,
                    callType = -1,
                    isBlacklisted = matchedPattern != null,
                    matchedPattern = matchedPattern?.pattern,
                    matchedPatternLabel = matchedPattern?.label?.takeIf { it.isNotBlank() }
                        ?: matchedPattern?.pattern,
                    interceptedByApp = true,
                    blockReason = appCall.blockReason,
                    appOnly = true
                )
            }

        (systemItems + roomOnlyItems).sortedByDescending { it.timestamp }
    }

    private fun readSystemCallLog(limit: Int = 200): List<SystemCall> {
        val calls = mutableListOf<SystemCall>()
        val cursor = context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.DATE, CallLog.Calls.TYPE),
            null, null,
            "${CallLog.Calls.DATE} DESC"
        ) ?: return calls

        cursor.use {
            val numIdx = it.getColumnIndex(CallLog.Calls.NUMBER)
            val dateIdx = it.getColumnIndex(CallLog.Calls.DATE)
            val typeIdx = it.getColumnIndex(CallLog.Calls.TYPE)
            var count = 0
            while (it.moveToNext() && count < limit) {
                calls.add(SystemCall(
                    number = it.getString(numIdx) ?: "",
                    timestamp = it.getLong(dateIdx),
                    type = it.getInt(typeIdx)
                ))
                count++
            }
        }
        return calls
    }

    private data class SystemCall(val number: String, val timestamp: Long, val type: Int)

    companion object {
        @Volatile private var instance: CallLogRepository? = null

        fun getInstance(context: Context, db: AppDatabase): CallLogRepository =
            instance ?: synchronized(this) {
                instance ?: CallLogRepository(context, db).also { instance = it }
            }
    }
}
