package com.acdcmaia.callguard.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class BlockReason { BLACKLIST, FIRST_CALL }

@Entity(tableName = "recent_calls")
data class RecentCall(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: String,
    val timestamp: Long,
    val allowed: Boolean,
    val blockReason: BlockReason? = null,
    val matchedPattern: String? = null
)
