package com.fpf.smartscan.data

import androidx.room.TypeConverter
import com.fpf.smartscan.media.MediaType

class MediaTypeConverter {

    @TypeConverter
    fun fromMediaType(type: MediaType): Int {
        return type.code
    }

    @TypeConverter
    fun toMediaType(value: Int): MediaType {
        return MediaType.entries.firstOrNull { it.code == value }
            ?: throw IllegalArgumentException("Unknown MediaType code: $value")
    }
}