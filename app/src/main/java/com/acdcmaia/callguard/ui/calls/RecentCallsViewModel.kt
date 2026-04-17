package com.acdcmaia.callguard.ui.calls

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.acdcmaia.callguard.CallGuardApp
import com.acdcmaia.callguard.data.db.RecentCall
import kotlinx.coroutines.flow.Flow

class RecentCallsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as CallGuardApp).callRepository
    val calls: Flow<List<RecentCall>> = repo.recentCalls
}
