package com.acdcmaia.callguard.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.acdcmaia.callguard.CallGuardApp
import com.acdcmaia.callguard.MainActivity
import com.acdcmaia.callguard.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class CallGuardForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pruned = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(openApp)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        if (!pruned) {
            pruned = true
            pruneOldCalls()
        }
        return START_STICKY
    }

    private val app: CallGuardApp
        get() = application as? CallGuardApp
            ?: throw IllegalStateException("Application deve ser CallGuardApp")

    private fun pruneOldCalls() {
        val dao = app.database.recentCallDao()
        val cutoff = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1_000
        serviceScope.launch { dao.deleteOlderThan(cutoff) }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "callguard_active"
        private const val NOTIFICATION_ID = 1

        fun start(context: Context) {
            context.startForegroundService(Intent(context, CallGuardForegroundService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, CallGuardForegroundService::class.java))
        }
    }
}
