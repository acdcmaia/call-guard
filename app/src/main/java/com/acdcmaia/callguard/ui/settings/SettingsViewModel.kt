package com.acdcmaia.callguard.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.acdcmaia.callguard.CallGuardApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val settings = (app as CallGuardApp).settingsRepository
    val windowMinutes: Flow<Int> = settings.windowMinutes

    fun setWindowMinutes(minutes: Int) {
        viewModelScope.launch { settings.setWindowMinutes(minutes) }
    }
}
