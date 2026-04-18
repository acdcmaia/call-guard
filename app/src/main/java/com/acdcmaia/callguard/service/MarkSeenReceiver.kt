package com.acdcmaia.callguard.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.acdcmaia.callguard.MainActivity
import com.acdcmaia.callguard.callGuardApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MarkSeenReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION = "com.acdcmaia.callguard.ACTION_MARK_SEEN"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val app = context.callGuardApp
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val total = app.callRepository.countBlockedOnce()
                app.settingsRepository.setSeenBlockedCount(total)
            } finally {
                pendingResult.finish()
            }
        }
        context.startActivity(
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        )
    }
}
