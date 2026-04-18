package com.acdcmaia.callguard.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.acdcmaia.callguard.CallGuardApp
import com.acdcmaia.callguard.data.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class SettingsViewModel(private val settings: SettingsRepository) : ViewModel() {

    val windowSeconds: Flow<Int> = settings.windowSeconds

    fun setWindowSeconds(seconds: Int) {
        viewModelScope.launch { settings.setWindowSeconds(seconds) }
    }

    companion object {
        fun factory(app: CallGuardApp) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SettingsViewModel(app.settingsRepository) as T
        }
    }
}
