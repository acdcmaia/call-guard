package com.acdcmaia.callguard.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.acdcmaia.callguard.BuildConfig
import com.acdcmaia.callguard.CallGuardApp
import com.acdcmaia.callguard.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

enum class UpdateStatus { CHECKING, UP_TO_DATE, UPDATE_AVAILABLE, ERROR }

class SettingsViewModel(private val settings: SettingsRepository) : ViewModel() {

    val windowSeconds: Flow<Int> = settings.windowSeconds

    private val _updateStatus = MutableStateFlow(UpdateStatus.CHECKING)
    val updateStatus: StateFlow<UpdateStatus> = _updateStatus

    private var updateJob: Job? = null

    fun checkForUpdates() {
        updateJob?.cancel()
        updateJob = viewModelScope.launch {
            _updateStatus.value = UpdateStatus.CHECKING
            try {
                val latest = withContext(Dispatchers.IO) {
                    val json = URL("https://api.github.com/repos/acdcmaia/call-guard/releases/latest").readText()
                    val match = Regex("\"tag_name\"\\s*:\\s*\"v?([^\"]+)\"").find(json)
                    match?.groupValues?.get(1) ?: error("tag_name não encontrado")
                }
                _updateStatus.value = if (isNewer(latest, BuildConfig.VERSION_NAME)) UpdateStatus.UPDATE_AVAILABLE else UpdateStatus.UP_TO_DATE
            } catch (_: Exception) {
                _updateStatus.value = UpdateStatus.ERROR
            }
        }
    }

    fun setWindowSeconds(seconds: Int) {
        viewModelScope.launch { settings.setWindowSeconds(seconds) }
    }

    companion object {
        private fun isNewer(remote: String, local: String): Boolean {
            val r = remote.split(".").map { it.toIntOrNull() ?: 0 }
            val l = local.split(".").map { it.toIntOrNull() ?: 0 }
            val size = maxOf(r.size, l.size)
            for (i in 0 until size) {
                val rv = r.getOrElse(i) { 0 }
                val lv = l.getOrElse(i) { 0 }
                if (rv != lv) return rv > lv
            }
            return false
        }


        fun factory(app: CallGuardApp) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SettingsViewModel(app.settingsRepository) as T
        }
    }
}
