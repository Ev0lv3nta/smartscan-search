package com.fpf.smartscan.data

import androidx.room.TypeConverter
import com.fpf.smartscan.media.MediaType

class MediaTypeConverter {

    @TypeConverter
    fun fromMediaType(type: MediaType): String {
        return type.name
    }

    @TypeConverter
    fun toMediaType(value: String): MediaType {
        return MediaType.valueOf(value)
    }
}