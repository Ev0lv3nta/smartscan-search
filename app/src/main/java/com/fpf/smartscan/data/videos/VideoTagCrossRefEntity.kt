package com.fpf.smartscan.data.videos

import androidx.room.Entity

@Entity(
    tableName = "video_tag_crossref",
    primaryKeys = ["videoId", "tag"]
)
data class VideoTagCrossRef(
    val videoId: Long,
    val tag: String
)
