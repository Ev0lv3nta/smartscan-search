package com.fpf.smartscan.data.videos.tags

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.fpf.smartscan.data.MediaTagCrossRef

@Entity(
    tableName = "video_tag_crossref",
    primaryKeys = ["mediaId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = VideoTag::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tagId")]
)
data class VideoTagCrossRef(
    override val mediaId: Long,
    override val tagId: Long
): MediaTagCrossRef(mediaId, tagId)
