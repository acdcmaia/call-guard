package com.acdcmaia.callguard

import android.app.role.RoleManager
import android.os.PowerManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.acdcmaia.callguard.service.CallGuardForegroundService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainViewModel(private val app: CallGuardApp) : ViewModel() {

    private val _hasRole = MutableStateFlow(false)
    val hasRole: StateFlow<Boolean> = _hasRole

    private val _isBatteryUnrestricted = MutableStateFlow(false)
    val isBatteryUnrestricted: StateFlow<Boolean> = _isBatteryUnrestricted

    fun checkRole() {
        val rm = app.getSystemService(RoleManager::class.java)
        _hasRole.value = rm.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
        if (_hasRole.value) CallGuardForegroundService.start(app)
        else CallGuardForegroundService.stop(app)
    }

    fun checkBatteryOptimization() {
        val pm = app.getSystemService(PowerManager::class.java)
        _isBatteryUnrestricted.value = pm.isIgnoringBatteryOptimizations(app.packageName)
    }

    companion object {
        fun factory(app: CallGuardApp) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                MainViewModel(app) as T
        }
    }
}
