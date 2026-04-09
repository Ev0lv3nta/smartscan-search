package com.fpf.smartscan.data.images

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.fpf.smartscan.data.MediaTagCrossRef

@Entity(
    tableName = "image_tag_crossref",
    primaryKeys = ["mediaId", "tag"],
    foreignKeys = [
        ForeignKey(
            entity = ImageTag::class,
            parentColumns = ["name"],
            childColumns = ["tag"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tag")]
)
data class ImageTagCrossRef(
    override val mediaId: Long,
    override val tag: String
): MediaTagCrossRef(mediaId, tag)
