package com.fpf.smartscan.data.videos

import androidx.room.*

@Entity(
    tableName = "video_tag",
    primaryKeys = ["videoId", "tag"]
)
data class VideoTag(
    val videoId: Long,
    val tag: String
)
