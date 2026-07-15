package io.github.ev0lv3nta.smartscansearch.data

import androidx.room.TypeConverter
import io.github.ev0lv3nta.smartscansearch.media.MediaType

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