package com.fpf.smartscan.data.images

import androidx.room.*

@Entity(
    tableName = "image_tag",
    primaryKeys = ["imageId", "tag"]
)
data class ImageTag(
    val imageId: Long,
    val tag: String
)
