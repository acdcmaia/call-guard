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
        prefs[WINDOW_SECONDS] ?: DEFAULT_WINDOW_SECONDS
    }

    suspend fun setWindowSeconds(seconds: Int) {
        context.dataStore.edit { it[WINDOW_SECONDS] = seconds }
    }

    companion object {
        const val DEFAULT_WINDOW_SECONDS = 120
    }
}
