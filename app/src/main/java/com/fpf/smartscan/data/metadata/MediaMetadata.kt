package com.fpf.smartscan.data.metadata


import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.fpf.smartscan.data.MediaTypeConverter
import com.fpf.smartscan.media.MediaType

@Entity(
    tableName = "media_metadata",
    indices = [
        Index(value = ["dateAdded"]),
        Index(value = ["type"]),
        Index(value = ["type", "dateAdded"])
    ])
@TypeConverters(MediaTypeConverter::class)
data class MediaMetadata(
    @PrimaryKey
    val id: Long,
    val type: MediaType,
    val dateAdded: Long
)
