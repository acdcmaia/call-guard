package com.acdcmaia.callguard.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager

class PowerSaveReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == PowerManager.ACTION_POWER_SAVE_MODE_CHANGED) {
            val pm = context.getSystemService(PowerManager::class.java)
            if (!pm.isPowerSaveMode) {
                try {
                    CallGuardForegroundService.startFromBackground(context)
                } catch (_: Exception) { }
            }
        }
    }
}
