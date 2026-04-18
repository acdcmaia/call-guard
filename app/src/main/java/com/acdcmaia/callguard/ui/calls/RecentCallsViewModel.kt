package com.acdcmaia.callguard.ui.calls

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.acdcmaia.callguard.CallGuardApp
import com.acdcmaia.callguard.data.CallHistoryItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RecentCallsViewModel(app: Application) : AndroidViewModel(app) {
    private val callLogRepo = (app as CallGuardApp).callLogRepository
    private val callRepo = (app as CallGuardApp).callRepository

    private val _calls = MutableStateFlow<List<CallHistoryItem>>(emptyList())
    val calls: StateFlow<List<CallHistoryItem>> = _calls

    init {
        viewModelScope.launch {
            callRepo.recentCalls.collect { refresh() }
        }
    }

    fun loadHistory() {
        viewModelScope.launch { refresh() }
    }

    private suspend fun refresh() {
        _calls.value = callLogRepo.getMergedHistory()
    }
}
