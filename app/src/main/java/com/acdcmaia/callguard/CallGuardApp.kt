package com.acdcmaia.callguard

import android.app.Application
import com.acdcmaia.callguard.data.CallLogRepository
import com.acdcmaia.callguard.data.CallRepository
import com.acdcmaia.callguard.data.SettingsRepository
import com.acdcmaia.callguard.data.db.AppDatabase

class CallGuardApp : Application() {
    val database by lazy { AppDatabase.getInstance(this) }
    val callRepository by lazy { CallRepository.getInstance(database) }
    val callLogRepository by lazy { CallLogRepository.getInstance(this, database) }
    val settingsRepository by lazy { SettingsRepository.getInstance(this) }
}
