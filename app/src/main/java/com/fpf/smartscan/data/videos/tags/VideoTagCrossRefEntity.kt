package com.fpf.smartscan.data.videos.tags

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.fpf.smartscan.data.MediaTagCrossRef

@Entity(
    tableName = "video_tag_crossref",
    primaryKeys = ["mediaId", "tag"],
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
    override val mediaId: Long,
    override val tag: String
): MediaTagCrossRef(mediaId, tag)
