package com.fpf.smartscan.data.images

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "image_tag")
data class ImageTag(
    @PrimaryKey
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long? = null,
    val cohesionScore: Float? = null,
    )
