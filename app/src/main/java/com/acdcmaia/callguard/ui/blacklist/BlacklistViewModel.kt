package com.acdcmaia.callguard.ui.blacklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.acdcmaia.callguard.CallGuardApp
import com.acdcmaia.callguard.data.CallRepository
import com.acdcmaia.callguard.data.db.BlacklistPattern
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class BlacklistViewModel(private val repo: CallRepository) : ViewModel() {

    val patterns: Flow<List<BlacklistPattern>> = repo.blacklistPatterns

    fun addPattern(pattern: String, label: String) {
        if (!isValidPattern(pattern)) return
        viewModelScope.launch {
            repo.addPattern(BlacklistPattern(pattern = pattern, label = label))
        }
    }

    fun deletePattern(pattern: BlacklistPattern) {
        viewModelScope.launch { repo.deletePattern(pattern) }
    }

    fun updatePattern(original: BlacklistPattern, newPattern: String, newLabel: String) {
        if (!isValidPattern(newPattern)) return
        viewModelScope.launch {
            repo.updatePattern(original.copy(pattern = newPattern, label = newLabel))
        }
    }

    fun isValidPattern(pattern: String): Boolean =
        pattern.length >= 4 && pattern.all { it.isDigit() }

    companion object {
        fun factory(app: CallGuardApp) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                BlacklistViewModel(app.callRepository) as T
        }
    }
}
