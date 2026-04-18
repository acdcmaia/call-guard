package com.acdcmaia.callguard.ui.blacklist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.acdcmaia.callguard.CallGuardApp
import com.acdcmaia.callguard.data.db.BlacklistPattern
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class BlacklistViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as CallGuardApp).callRepository
    val patterns: Flow<List<BlacklistPattern>> = repo.blacklistPatterns

    fun addPattern(pattern: String, label: String) {
        if (pattern.isBlank()) return
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
        pattern.isNotBlank() && pattern.all { it.isDigit() }
}
