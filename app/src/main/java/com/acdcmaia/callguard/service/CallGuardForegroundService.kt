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
import com.acdcmaia.callguard.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CallGuardForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pruned = false

    private val app: CallGuardApp
        get() = application as? CallGuardApp
            ?: throw IllegalStateException("Application deve ser CallGuardApp")

    private val openAppPendingIntent: PendingIntent by lazy {
        PendingIntent.getActivity(
            this, 0,
            Intent(this, com.acdcmaia.callguard.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification(0L))
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
                app.callRepository.countBlockedFlow(),
                app.settingsRepository.seenBlockedCount
            ) { total, seen ->
                maxOf(0L, total - maxOf(0L, seen))
            }.collect { newCount ->
                updateNotification(newCount)
            }
        }
    }

    private fun buildNotification(newBlockedCount: Long): Notification {
        return if (newBlockedCount > 0L) {
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(
                    resources.getQuantityString(
                        R.plurals.notification_blocked,
                        newBlockedCount.toInt(),
                        newBlockedCount.toInt()
                    )
                )
                .setSmallIcon(R.drawable.ic_notification_blocked)
                .setColor(Color.rgb(211, 47, 47))
                .setColorized(true)
                .setContentIntent(openAppPendingIntent)
                .setOngoing(true)
                .setSilent(true)
                .build()
        } else {
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.notification_active))
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(Color.rgb(56, 142, 60))
                .setColorized(true)
                .setContentIntent(openAppPendingIntent)
                .setOngoing(true)
                .setSilent(true)
                .build()
        }
    }

    private fun updateNotification(newBlockedCount: Long) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification(newBlockedCount))
    }

    private fun pruneOldCalls() {
        val cutoff = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1_000
        serviceScope.launch {
            app.callRepository.pruneCallsBefore(cutoff)
            val newTotal = app.callRepository.countBlockedOnce()
            val seen = app.settingsRepository.seenBlockedCount.first()
            if (seen > newTotal) {
                app.settingsRepository.setSeenBlockedCount(newTotal)
            }
        }
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
            NotificationManager.IMPORTANCE_DEFAULT
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "callguard_status"
        private const val NOTIFICATION_ID = 1

        fun start(context: Context) {
            context.startForegroundService(Intent(context, CallGuardForegroundService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, CallGuardForegroundService::class.java))
        }
    }
}
