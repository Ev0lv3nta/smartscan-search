package com.fpf.smartscan.data.videos

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index


@Entity(
    tableName = "video_tag_crossref",
    primaryKeys = ["videoId", "tag"]
)
data class VideoTagCrossRef(
    val videoId: Long,
    val tag: String
)
