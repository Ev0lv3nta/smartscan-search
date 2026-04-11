package com.fpf.smartscan.data.images.tags

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.fpf.smartscan.model.MediaTag

@Entity(tableName = "image_tag", indices = [Index(value = ["name"], unique = true)])
data class ImageTag(
    @PrimaryKey(autoGenerate = true)
    override val id: Long = 0,
    override val name: String,
    override val lastUsedAt: Long? = null,
    ): MediaTag(id, name, lastUsedAt)
