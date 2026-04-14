package com.fpf.smartscan.data.old.images

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "image_tag")
data class ImageTag(
    @PrimaryKey
    val name: String,
    val prototypeId: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long? = null,
    val cohesionScore: Float? = null,
    val nPrototype: Int = 0
    )
