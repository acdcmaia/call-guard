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

    fun isValidRegex(pattern: String): Boolean =
        runCatching { Regex(pattern) }.isSuccess
}
