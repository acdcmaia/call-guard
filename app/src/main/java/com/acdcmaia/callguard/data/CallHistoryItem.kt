package com.acdcmaia.callguard.data

import com.acdcmaia.callguard.data.db.BlockReason

data class CallHistoryItem(
    val number: String,
    val timestamp: Long,
    val callType: Int,
    val isBlacklisted: Boolean,
    val matchedPattern: String?,
    val matchedPatternLabel: String?,
    val interceptedByApp: Boolean,
    val blockReason: BlockReason?,
    val appOnly: Boolean = false
)
