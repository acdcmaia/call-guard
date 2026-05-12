package com.acdcmaia.callguard.service

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.TelephonyManager

class PhoneStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.getStringExtra(TelephonyManager.EXTRA_STATE) == TelephonyManager.EXTRA_STATE_IDLE) {
            restartService(context)
        }
    }

    private fun restartService(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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
}
