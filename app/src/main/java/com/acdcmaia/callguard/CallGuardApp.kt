package com.acdcmaia.callguard

import android.app.Application
import android.content.Context
import com.acdcmaia.callguard.data.CallLogRepository
import com.acdcmaia.callguard.data.CallRepository
import com.acdcmaia.callguard.data.ContactsRepository
import com.acdcmaia.callguard.data.SettingsRepository
import com.acdcmaia.callguard.data.db.AppDatabase

val Context.callGuardApp: CallGuardApp
    get() = applicationContext as? CallGuardApp
        ?: throw IllegalStateException("Application deve ser CallGuardApp")

class CallGuardApp : Application() {
    val database by lazy { AppDatabase.getInstance(this) }
    val callRepository by lazy { CallRepository(database) }
    val callLogRepository by lazy { CallLogRepository(this, database, contactsRepository) }
    val settingsRepository by lazy { SettingsRepository(this) }
    val contactsRepository by lazy { ContactsRepository(this) }
}
