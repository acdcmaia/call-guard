package com.acdcmaia.callguard.service

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager

class PowerSaveReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pm = context.getSystemService(PowerManager::class.java)
        val shouldRestart = when (intent.action) {
            PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> !pm.isPowerSaveMode
            PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED -> !pm.isDeviceIdleMode
            // Broadcast proprietário do MIUI para mudança de modo de economia de bateria.
            // mode 0 = desativado, 1 = economia normal, 2 = ultra economia
            ACTION_MIUI_POWER_SAVE -> intent.getIntExtra(EXTRA_MIUI_MODE, 0) == 0
            else -> false
        }
        if (shouldRestart) restartService(context)
    }

    private fun restartService(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+: startForegroundService pode ser bloqueado a partir de receivers;
            // expedited JobService é o caminho oficial para iniciar FGS do background.
            val job = JobInfo.Builder(
                ServiceRestartJob.JOB_ID,
                ComponentName(context, ServiceRestartJob::class.java)
            ).setExpedited(true)
             .setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE)
             .build()
            context.getSystemService(JobScheduler::class.java).schedule(job)
        } else {
            try { CallGuardForegroundService.startFromBackground(context) } catch (_: Exception) { }
        }
    }

    companion object {
        private const val ACTION_MIUI_POWER_SAVE = "miui.intent.action.POWER_SAVE_MODE_CHANGED"
        private const val EXTRA_MIUI_MODE = "miui.intent.extra.POWER_SAVE_MODE"
    }
}
