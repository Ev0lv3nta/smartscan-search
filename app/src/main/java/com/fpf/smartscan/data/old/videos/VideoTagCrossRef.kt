package com.fpf.smartscan.data.old.videos

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "video_tag_crossref",
    primaryKeys = ["videoId", "tag"],
    foreignKeys = [
        ForeignKey(
            entity = VideoTag::class,
            parentColumns = ["name"],
            childColumns = ["tag"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tag")]
)
data class VideoTagCrossRef(
    val videoId: Long,
    val tag: String
)
