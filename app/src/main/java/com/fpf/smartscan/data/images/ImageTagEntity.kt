package com.fpf.smartscan.data.images

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fpf.smartscan.data.MediaTag

@Entity(tableName = "image_tag")
data class ImageTag(
    @PrimaryKey
    override val name: String,
    override val lastUsedAt: Long? = null,
    ): MediaTag(name, lastUsedAt)
