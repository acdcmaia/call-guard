package com.acdcmaia.callguard.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.acdcmaia.callguard.CallGuardApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val settings = (app as CallGuardApp).settingsRepository
    val windowSeconds: Flow<Int> = settings.windowSeconds

    fun setWindowSeconds(seconds: Int) {
        viewModelScope.launch { settings.setWindowSeconds(seconds) }
    }
}
