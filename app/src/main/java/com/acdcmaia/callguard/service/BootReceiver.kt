package com.acdcmaia.callguard.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // MIUI dispara BOOT_COMPLETED para o processo de backup durante instalação/restauração.
            // Em um boot real, este broadcast chega nos primeiros minutos de uptime.
            // Se o uptime já passou de 5 minutos, é o broadcast falso do MIUI — ignorar.
            if (SystemClock.elapsedRealtime() > 5 * 60_000L) return
            try {
                CallGuardForegroundService.startFromBackground(context)
            } catch (_: Exception) { }
        }
    }
}
