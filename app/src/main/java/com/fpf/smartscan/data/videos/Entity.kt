package com.fpf.smartscan.data.videos

import androidx.room.*

@Entity(tableName = "video_metadata")
data class VideoMetadata(
    @PrimaryKey
    val id: Long,     // MediaStore ID
    val tags: List<String>,
)