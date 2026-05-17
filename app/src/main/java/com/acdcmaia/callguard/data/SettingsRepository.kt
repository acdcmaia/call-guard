package com.acdcmaia.callguard.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val WINDOW_SECONDS = intPreferencesKey("window_seconds")
    private val SEEN_BLOCKED_COUNT = longPreferencesKey("seen_blocked_count")
    private val AUTOSTART_PROMPT_SHOWN = booleanPreferencesKey("autostart_prompt_shown")
    private val AUTOSTART_CONFIGURED = booleanPreferencesKey("autostart_configured")
    private val SERVICE_ENABLED = booleanPreferencesKey("service_enabled")

    val windowSeconds: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[WINDOW_SECONDS] ?: DEFAULT_WINDOW_SECONDS
    }

    val seenBlockedCount: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[SEEN_BLOCKED_COUNT] ?: -1L
    }

    val autostartPromptShown: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[AUTOSTART_PROMPT_SHOWN] ?: false
    }

    val autostartConfigured: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[AUTOSTART_CONFIGURED] ?: false
    }

    val serviceEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[SERVICE_ENABLED] ?: true
    }

    suspend fun setWindowSeconds(seconds: Int) {
        try { context.dataStore.edit { it[WINDOW_SECONDS] = seconds } } catch (_: IOException) { }
    }

    suspend fun setSeenBlockedCount(count: Long) {
        try { context.dataStore.edit { it[SEEN_BLOCKED_COUNT] = count } } catch (_: IOException) { }
    }

    suspend fun setAutostartPromptShown() {
        try { context.dataStore.edit { it[AUTOSTART_PROMPT_SHOWN] = true } } catch (_: IOException) { }
    }

    suspend fun setAutostartConfigured() {
        try { context.dataStore.edit { it[AUTOSTART_PROMPT_SHOWN] = true; it[AUTOSTART_CONFIGURED] = true } } catch (_: IOException) { }
    }

    suspend fun setServiceEnabled(enabled: Boolean) {
        try { context.dataStore.edit { it[SERVICE_ENABLED] = enabled } } catch (_: IOException) { }
    }

    companion object {
        const val DEFAULT_WINDOW_SECONDS = 120
    }
}
