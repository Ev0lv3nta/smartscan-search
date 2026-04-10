package com.fpf.smartscan.data.images.tags

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fpf.smartscan.collections.MediaTag

@Entity(tableName = "image_tag")
data class ImageTag(
    @PrimaryKey
    override val name: String,
    override val lastUsedAt: Long? = null,
    ): MediaTag(name, lastUsedAt)
