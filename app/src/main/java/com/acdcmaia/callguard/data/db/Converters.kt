package com.acdcmaia.callguard.data.db

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromBlockReason(value: BlockReason?): String? = value?.name

    @TypeConverter
    fun toBlockReason(value: String?): BlockReason? = value?.let { BlockReason.valueOf(it) }
}
