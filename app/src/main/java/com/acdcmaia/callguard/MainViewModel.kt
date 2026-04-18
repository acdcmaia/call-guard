package com.acdcmaia.callguard

import android.app.role.RoleManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.acdcmaia.callguard.service.CallGuardForegroundService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(private val app: CallGuardApp) : ViewModel() {

    private val _hasRole = MutableStateFlow(false)
    val hasRole: StateFlow<Boolean> = _hasRole

    fun checkRole() {
        viewModelScope.launch(Dispatchers.IO) {
            val rm = app.getSystemService(RoleManager::class.java)
            val roleHeld = rm.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
            withContext(Dispatchers.Main) {
                _hasRole.value = roleHeld
                if (roleHeld) CallGuardForegroundService.start(app)
                else CallGuardForegroundService.stop(app)
            }
        }
    }

    companion object {
        fun factory(app: CallGuardApp) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                MainViewModel(app) as T
        }
    }
}
