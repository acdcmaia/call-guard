package com.acdcmaia.callguard.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class BlockReason { BLACKLIST, FIRST_CALL, HIDDEN_NUMBER }

@Entity(
    tableName = "recent_calls",
    indices = [Index(value = ["number", "timestamp"])]
)
data class RecentCall(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: String,
    val timestamp: Long,
    val blockReason: BlockReason? = null,
    val matchedPattern: String? = null,
    @ColumnInfo(defaultValue = "0") val serviceWasDisabled: Boolean = false
)
