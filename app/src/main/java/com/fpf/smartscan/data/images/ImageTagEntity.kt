package com.fpf.smartscan.data.images

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fpf.smartscan.search.MediaTag

@Entity(tableName = "image_tag")
data class ImageTag(
    @PrimaryKey
    override val name: String,
    override val prototypeId: Long,
    override val createdAt: Long = System.currentTimeMillis(),
    override val lastUsedAt: Long? = null,
    override val cohesionScore: Float? = null,
    override val nPrototype: Int = 0
    ): MediaTag
