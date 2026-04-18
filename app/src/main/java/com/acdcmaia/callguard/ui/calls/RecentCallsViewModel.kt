package com.acdcmaia.callguard.ui.calls

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.acdcmaia.callguard.CallGuardApp
import com.acdcmaia.callguard.data.CallHistoryItem
import com.acdcmaia.callguard.data.CallLogRepository
import com.acdcmaia.callguard.data.CallRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RecentCallsViewModel(
    private val callLogRepo: CallLogRepository,
    private val callRepo: CallRepository
) : ViewModel() {

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

    companion object {
        fun factory(app: CallGuardApp) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                RecentCallsViewModel(app.callLogRepository, app.callRepository) as T
        }
    }
}
