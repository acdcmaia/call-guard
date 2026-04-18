package com.acdcmaia.callguard.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val WINDOW_SECONDS = intPreferencesKey("window_seconds")

    val windowSeconds: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[WINDOW_SECONDS] ?: 120
    }

    suspend fun setWindowSeconds(seconds: Int) {
        context.dataStore.edit { it[WINDOW_SECONDS] = seconds }
    }

    companion object {
        @Volatile private var instance: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository =
            instance ?: synchronized(this) {
                instance ?: SettingsRepository(context).also { instance = it }
            }
    }
}
