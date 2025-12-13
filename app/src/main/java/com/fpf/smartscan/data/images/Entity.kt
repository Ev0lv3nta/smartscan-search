package com.fpf.smartscan.data.images

import androidx.room.*

@Entity(tableName = "image_metadata")
data class ImageMetadata(
    @PrimaryKey
    val id: Long,     // MediaStore ID
    val tags: List<String>,
)