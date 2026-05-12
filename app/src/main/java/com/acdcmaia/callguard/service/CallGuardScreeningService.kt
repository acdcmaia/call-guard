package com.acdcmaia.callguard.service

import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import com.acdcmaia.callguard.BuildConfig
import com.acdcmaia.callguard.CallGuardApp
import com.acdcmaia.callguard.data.db.BlockReason
import com.acdcmaia.callguard.data.db.RecentCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val TAG = "CallGuard"

class CallGuardScreeningService : CallScreeningService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val screenMutex = Mutex()

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Log.i(TAG, "CallGuardScreeningService onCreate — service bound by telecom")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onScreenCall(callDetails: Call.Details) {
        if (callDetails.callDirection == Call.Details.DIRECTION_OUTGOING) {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        val number = callDetails.handle?.schemeSpecificPart ?: run {
            if (BuildConfig.DEBUG) Log.w(TAG, "onScreenCall: handle null, allowing")
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        if (BuildConfig.DEBUG) Log.i(TAG, "onScreenCall: received call")

        serviceScope.launch {
            try {
                screenCall(callDetails, number)
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.e(TAG, "Error screening call, allowing as fallback", e)
                respondToCall(callDetails, CallResponse.Builder().build())
            }
            // Garante que o FGS está ativo após processar a chamada.
            // Cobre o cenário em que o ultra modo de bateria matou o serviço de notificação:
            // o telecom acorda este serviço mesmo assim, e usamos essa execução para restaurar
            // o ícone e a contagem correta de chamadas bloqueadas.
            CallGuardForegroundService.startFromBackground(this@CallGuardScreeningService)
        }
    }

    private val app: CallGuardApp
        get() = application as? CallGuardApp
            ?: throw IllegalStateException("Application deve ser CallGuardApp")

    private suspend fun screenCall(callDetails: Call.Details, number: String) = screenMutex.withLock {
        val repo = app.callRepository
        val settings = app.settingsRepository
        val contacts = app.contactsRepository

        val (patterns, windowSeconds, isContact) = coroutineScope {
            val patternsDeferred = async { repo.getAllPatternsOnce() }
            val windowSecondsDeferred = async { settings.windowSeconds.first() }
            val isContactDeferred = async { contacts.isContact(number) }
            Triple(patternsDeferred.await(), windowSecondsDeferred.await(), isContactDeferred.await())
        }

        if (BuildConfig.DEBUG) Log.i(TAG, "Loaded ${patterns.size} pattern(s), window=${windowSeconds}s, isContact=$isContact")

        if (isContact) {
            if (BuildConfig.DEBUG) Log.i(TAG, "ALLOW (contact)")
            repo.recordCall(RecentCall(
                number = number,
                timestamp = System.currentTimeMillis()
            ))
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        if (BuildConfig.DEBUG) patterns.forEach { Log.i(TAG, "  pattern: '${it.pattern}' label='${it.label}'") }

        val digits = number.filter { it.isDigit() }
        val matchedPattern = patterns.firstOrNull { entry ->
            val matched = digits.contains(entry.pattern)
            if (BuildConfig.DEBUG && matched) Log.i(TAG, "  MATCHED pattern '${entry.pattern}'")
            matched
        }

        if (matchedPattern != null) {
            if (BuildConfig.DEBUG) Log.i(TAG, "BLOCK (blacklist): matched '${matchedPattern.pattern}'")
            repo.recordCall(RecentCall(
                number = number,
                timestamp = System.currentTimeMillis(),
                blockReason = BlockReason.BLACKLIST,
                matchedPattern = matchedPattern.pattern
            ))
            respondToCall(callDetails, CallResponse.Builder()
                .setDisallowCall(true)
                .setRejectCall(true)
                .build())
            return
        }

        val since = System.currentTimeMillis() - windowSeconds * 1_000L
        val recent = repo.getRecentCallsSince(number, since)

        if (BuildConfig.DEBUG) Log.i(TAG, "Recent calls in last ${windowSeconds}s: ${recent.size}")

        if (recent.isEmpty()) {
            if (BuildConfig.DEBUG) Log.i(TAG, "BLOCK (first call)")
            repo.recordCall(RecentCall(
                number = number,
                timestamp = System.currentTimeMillis(),
                blockReason = BlockReason.FIRST_CALL
            ))
            respondToCall(callDetails, CallResponse.Builder()
                .setDisallowCall(true)
                .setRejectCall(true)
                .build())
        } else {
            if (BuildConfig.DEBUG) Log.i(TAG, "ALLOW (repeat call)")
            repo.recordCall(RecentCall(
                number = number,
                timestamp = System.currentTimeMillis()
            ))
            respondToCall(callDetails, CallResponse.Builder().build())
        }
    }
}
