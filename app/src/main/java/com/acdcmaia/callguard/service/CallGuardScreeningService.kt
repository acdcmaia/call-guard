package com.acdcmaia.callguard.service

import android.telecom.Call
import android.telecom.CallScreeningService
import com.acdcmaia.callguard.CallGuardApp
import com.acdcmaia.callguard.data.db.BlockReason
import com.acdcmaia.callguard.data.db.RecentCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CallGuardScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val number = callDetails.handle?.schemeSpecificPart ?: run {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val app = application as CallGuardApp
            val repo = app.callRepository
            val settings = app.settingsRepository
            val windowMinutes = settings.windowMinutes.first()

            val patterns = repo.getAllPatternsOnce()
            val isBlacklisted = patterns.any { entry ->
                runCatching { Regex(entry.pattern).containsMatchIn(number) }.getOrDefault(false)
            }

            if (isBlacklisted) {
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
                return@launch
            }

            val since = System.currentTimeMillis() - windowMinutes * 60_000L
            val recent = repo.getRecentCallsSince(number, since)

            if (recent.isEmpty()) {
                repo.recordCall(RecentCall(
                    number = number,
                    timestamp = System.currentTimeMillis(),
                    allowed = false,
                    blockReason = BlockReason.FIRST_CALL
                ))
                respondToCall(callDetails, CallResponse.Builder()
                    .setDisallowCall(true)
                    .setSilenceCall(true)
                    .build())
            } else {
                repo.recordCall(RecentCall(
                    number = number,
                    timestamp = System.currentTimeMillis(),
                    allowed = true
                ))
                respondToCall(callDetails, CallResponse.Builder().build())
            }
        }
    }
}
