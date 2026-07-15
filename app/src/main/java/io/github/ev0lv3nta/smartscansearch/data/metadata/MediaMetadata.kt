package io.github.ev0lv3nta.smartscansearch.data.metadata


import androidx.room.Entity
import androidx.room.Index
import io.github.ev0lv3nta.smartscansearch.media.MediaType

@Entity(
    primaryKeys = ["id", "type"],
    tableName = "media_metadata",
    indices = [
        Index(value = ["dateAdded"]),
        Index(value = ["type", "dateAdded"])
    ])
data class MediaMetadata(
    val id: Long,
    val type: MediaType,
    val dateAdded: Long,
    val description: String? = null
)
