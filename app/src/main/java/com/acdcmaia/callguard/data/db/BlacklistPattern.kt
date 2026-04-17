package com.acdcmaia.callguard.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blacklist_patterns")
data class BlacklistPattern(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pattern: String,
    val label: String = ""
)
