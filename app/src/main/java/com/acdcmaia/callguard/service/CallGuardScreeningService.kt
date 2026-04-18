package com.acdcmaia.callguard.service

import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import com.acdcmaia.callguard.CallGuardApp
import com.acdcmaia.callguard.data.db.BlockReason
import com.acdcmaia.callguard.data.db.RecentCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "CallGuard"

class CallGuardScreeningService : CallScreeningService() {

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "CallGuardScreeningService onCreate — service bound by telecom")
    }

    override fun onScreenCall(callDetails: Call.Details) {
        val number = callDetails.handle?.schemeSpecificPart ?: run {
            Log.w(TAG, "onScreenCall: handle null, allowing")
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        Log.i(TAG, "onScreenCall: raw number='$number'")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                screenCall(callDetails, number)
            } catch (e: Exception) {
                Log.e(TAG, "Error screening call, allowing as fallback", e)
                respondToCall(callDetails, CallResponse.Builder().build())
            }
        }
    }

    private suspend fun screenCall(callDetails: Call.Details, number: String) {
        val app = application as CallGuardApp
        val repo = app.callRepository
        val settings = app.settingsRepository

        val (patterns, windowSeconds) = coroutineScope {
            val patternsDeferred = async { repo.getAllPatternsOnce() }
            val windowSecondsDeferred = async { settings.windowSeconds.first() }
            patternsDeferred.await() to windowSecondsDeferred.await()
        }

        Log.i(TAG, "Loaded ${patterns.size} blacklist pattern(s), window=${windowSeconds}s")
        patterns.forEach { Log.i(TAG, "  pattern: '${it.pattern}' label='${it.label}'") }

        val digits = number.filter { it.isDigit() }
        val matchedPattern = patterns.firstOrNull { entry ->
            val matched = digits.contains(entry.pattern)
            if (matched) Log.i(TAG, "  MATCHED '${entry.pattern}' against digits '$digits'")
            matched
        }

        if (matchedPattern != null) {
            Log.i(TAG, "BLOCK (blacklist): '$number' matched '${matchedPattern.pattern}'")
            repo.recordCall(RecentCall(
                number = number,
                timestamp = System.currentTimeMillis(),
                allowed = false,
                blockReason = BlockReason.BLACKLIST
            ))
            respondToCall(callDetails, CallResponse.Builder()
                .setDisallowCall(true)
                .setRejectCall(true)
                .build())
            return
        }

        val since = System.currentTimeMillis() - windowSeconds * 1_000L
        val recent = repo.getRecentCallsSince(number, since)

        Log.i(TAG, "Recent calls from '$number' in last ${windowSeconds}s: ${recent.size}")

        if (recent.isEmpty()) {
            Log.i(TAG, "BLOCK (first call): '$number'")
            repo.recordCall(RecentCall(
                number = number,
                timestamp = System.currentTimeMillis(),
                allowed = false,
                blockReason = BlockReason.FIRST_CALL
            ))
            respondToCall(callDetails, CallResponse.Builder()
                .setDisallowCall(true)
                .setRejectCall(true)
                .build())
        } else {
            Log.i(TAG, "ALLOW (repeat call): '$number'")
            repo.recordCall(RecentCall(
                number = number,
                timestamp = System.currentTimeMillis(),
                allowed = true
            ))
            respondToCall(callDetails, CallResponse.Builder().build())
        }
    }
}
