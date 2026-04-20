package com.fpf.smartscan.data.old.images

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "image_tag_crossref",
    primaryKeys = ["imageId", "tag"],
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
    val imageId: Long,
    val tag: String
)
