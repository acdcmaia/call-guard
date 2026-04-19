package com.acdcmaia.callguard.ui.calls

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.acdcmaia.callguard.CallGuardApp
import com.acdcmaia.callguard.data.CallHistoryItem
import com.acdcmaia.callguard.data.CallLogRepository
import com.acdcmaia.callguard.data.CallRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest

@OptIn(ExperimentalCoroutinesApi::class)
class RecentCallsViewModel(
    private val callLogRepo: CallLogRepository,
    callRepo: CallRepository
) : ViewModel() {

    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }

    val calls: StateFlow<List<CallHistoryItem>> = merge(
        callRepo.recentCalls.map { },
        refreshTrigger
    ).transformLatest {
        try {
            emit(callLogRepo.getMergedHistory())
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // não emite — mantém o último estado conhecido no StateFlow
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun loadHistory() {
        refreshTrigger.tryEmit(Unit)
    }

    companion object {
        fun factory(app: CallGuardApp) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                RecentCallsViewModel(app.callLogRepository, app.callRepository) as T
        }
    }
}
