package com.acdcmaia.callguard

import android.app.Application
import android.content.Context
import android.content.Intent
import com.acdcmaia.callguard.data.CallLogRepository
import com.acdcmaia.callguard.data.CallRepository
import com.acdcmaia.callguard.data.ContactsRepository
import com.acdcmaia.callguard.data.SettingsRepository
import com.acdcmaia.callguard.data.db.AppDatabase

val Context.callGuardApp: CallGuardApp
    get() = applicationContext as? CallGuardApp
        ?: throw IllegalStateException("Application deve ser CallGuardApp")

class CallGuardApp : Application() {
    private val database by lazy { AppDatabase.getInstance(this) }
    val callRepository by lazy { CallRepository(database) }
    val callLogRepository by lazy { CallLogRepository(this, database, contactsRepository) }
    val settingsRepository by lazy { SettingsRepository(this) }
    val contactsRepository by lazy { ContactsRepository(this) }

    override fun onCreate() {
        super.onCreate()
        installFgsRecoveryHandler()
    }

    // MIUI cria um ServiceRecord mesmo quando startForegroundService() é negado no processo de
    // backup. O timer desse registro expira no processo seguinte (MainActivity), causando
    // ForegroundServiceDidNotStartInTimeException. Capturamos e reiniciamos silenciosamente.
    private fun installFgsRecoveryHandler() {
        val original = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            if (throwable.javaClass.name.endsWith("ForegroundServiceDidNotStartInTimeException")) {
                scheduleRestart()
                android.os.Process.killProcess(android.os.Process.myPid())
            } else {
                original?.uncaughtException(Thread.currentThread(), throwable)
            }
        }
    }

    private fun scheduleRestart() {
        try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            } ?: return
            startActivity(intent)
        } catch (_: Exception) { }
    }
}
