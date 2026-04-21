package com.acdcmaia.callguard

import android.Manifest
import android.app.role.RoleManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
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

    private val _hasPermissions = MutableStateFlow(false)
    val hasPermissions: StateFlow<Boolean> = _hasPermissions

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

    fun checkPermissions() {
        val required = requiredPermissions()
        _hasPermissions.value = required.all {
            ContextCompat.checkSelfPermission(app, it) == PackageManager.PERMISSION_GRANTED
        }
        if (_hasPermissions.value) {
            app.contactsRepository.registerPermission()
        }
    }

    companion object {
        fun requiredPermissions(): Array<String> = buildList {
            add(Manifest.permission.READ_CONTACTS)
            add(Manifest.permission.READ_CALL_LOG)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()

        fun factory(app: CallGuardApp) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                MainViewModel(app) as T
        }
    }
}
