package com.acdcmaia.callguard.data

import android.content.Context
import android.provider.CallLog
import com.acdcmaia.callguard.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

class CallLogRepository(
    private val context: Context,
    private val db: AppDatabase,
    private val contactsRepository: ContactsRepository
) {

    suspend fun getMergedHistory(): List<CallHistoryItem> = withContext(Dispatchers.IO) {
        val systemCalls = readSystemCallLog()
        val roomCalls = db.recentCallDao().getAllOnce()
        val patternLabels = db.blacklistDao().getAllOnce().associate { it.pattern to it.label }

        val matchedRoomIds = mutableSetOf<Long>()

        val systemItems = systemCalls
            .map { call ->
                val appCall = roomCalls.firstOrNull { a ->
                    a.number == call.number && abs(a.timestamp - call.timestamp) < 60_000L
                }
                if (appCall != null) matchedRoomIds.add(appCall.id)
                val label = appCall?.matchedPattern?.let { p ->
                    patternLabels[p]?.takeIf { it.isNotBlank() } ?: p
                }
                CallHistoryItem(
                    number = call.number,
                    contactName = contactsRepository.getContactName(call.number),
                    timestamp = call.timestamp,
                    callType = call.type,
                    matchedPattern = appCall?.matchedPattern,
                    matchedPatternLabel = label,
                    blockReason = appCall?.blockReason,
                    appOnly = false
                )
            }

        val roomOnlyItems = roomCalls
            .filter { it.id !in matchedRoomIds }
            .map { appCall ->
                val label = appCall.matchedPattern?.let { p ->
                    patternLabels[p]?.takeIf { it.isNotBlank() } ?: p
                }
                CallHistoryItem(
                    number = appCall.number,
                    contactName = contactsRepository.getContactName(appCall.number),
                    timestamp = appCall.timestamp,
                    callType = -1,
                    matchedPattern = appCall.matchedPattern,
                    matchedPatternLabel = label,
                    blockReason = appCall.blockReason,
                    appOnly = true
                )
            }

        (systemItems + roomOnlyItems).sortedByDescending { it.timestamp }
    }

    private fun readSystemCallLog(limit: Int = 100): List<SystemCall> {
        val calls = mutableListOf<SystemCall>()
        val cursor = context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.DATE, CallLog.Calls.TYPE),
            "${CallLog.Calls.TYPE} != ?",
            arrayOf(CallLog.Calls.OUTGOING_TYPE.toString()),
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
}
