package com.acdcmaia.callguard.service

import android.app.job.JobParameters
import android.app.job.JobService

class ServiceRestartJob : JobService() {
    override fun onStartJob(params: JobParameters): Boolean {
        try { CallGuardForegroundService.startFromBackground(applicationContext) } catch (_: Exception) { }
        jobFinished(params, false)
        return false
    }

    override fun onStopJob(params: JobParameters): Boolean = false

    companion object {
        const val JOB_ID = 1001
    }
}
