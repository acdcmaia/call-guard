package com.acdcmaia.callguard.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.acdcmaia.callguard.CallGuardApp
import com.acdcmaia.callguard.MainActivity
import com.acdcmaia.callguard.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class CallGuardForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pruned = false

    private val app: CallGuardApp
        get() = application as? CallGuardApp
            ?: throw IllegalStateException("Application deve ser CallGuardApp")

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification(0))
        observeBlockedCount()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!pruned) {
            pruned = true
            pruneOldCalls()
        }
        return START_STICKY
    }

    private fun observeBlockedCount() {
        serviceScope.launch {
            combine(
                app.database.recentCallDao().countBlockedFlow(),
                app.settingsRepository.seenBlockedCount
            ) { total, seen ->
                // -1 means first run: treat all existing as seen
                if (seen < 0) 0 else maxOf(0, total - seen.toInt())
            }.collect { newCount ->
                updateNotification(newCount)
            }
        }
    }

    private fun buildNotification(newBlockedCount: Int): Notification {
        val markSeenIntent = Intent(this, MainActivity::class.java).apply {
            action = MainActivity.ACTION_MARK_SEEN
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, markSeenIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return if (newBlockedCount > 0) {
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(
                    resources.getQuantityString(
                        R.plurals.notification_blocked, newBlockedCount, newBlockedCount
                    )
                )
                .setSmallIcon(R.drawable.ic_notification_blocked)
                .setColor(Color.rgb(211, 47, 47))
                .setColorized(true)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build()
        } else {
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.notification_active))
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(Color.rgb(56, 142, 60))
                .setColorized(true)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build()
        }
    }

    private fun updateNotification(newBlockedCount: Int) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification(newBlockedCount))
    }

    private fun pruneOldCalls() {
        val cutoff = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1_000
        serviceScope.launch { app.database.recentCallDao().deleteOlderThan(cutoff) }
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
