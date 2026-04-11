package com.fpf.smartscan.data.images.tags

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.fpf.smartscan.data.MediaTagCrossRef

@Entity(
    tableName = "image_tag_crossref",
    primaryKeys = ["mediaId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = ImageTag::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tagId")]
)
data class ImageTagCrossRef(
    override val mediaId: Long,
    override val tagId: Long
): MediaTagCrossRef(mediaId, tagId)
