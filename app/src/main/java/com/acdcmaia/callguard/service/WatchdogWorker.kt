package com.acdcmaia.callguard.service

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class WatchdogWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        CallGuardForegroundService.startFromBackground(applicationContext)
        return Result.success()
    }
}
