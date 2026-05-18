package com.acdcmaia.callguard.data

import com.acdcmaia.callguard.data.db.BlockReason

data class CallHistoryItem(
    val number: String,
    val contactName: String?,
    val timestamp: Long,
    val callType: Int,
    val matchedPattern: String?,
    val matchedPatternLabel: String?,
    val blockReason: BlockReason?,
    val appOnly: Boolean = false,
    val serviceWasDisabled: Boolean = false,
    val handledByApp: Boolean = false
)
