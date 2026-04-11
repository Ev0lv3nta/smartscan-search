package com.fpf.smartscan.data

import com.fpf.smartscan.data.images.tags.ImageTag
import com.fpf.smartscan.data.videos.tags.VideoTag

data class TagWithCount(
    val id: Long,
    val name: String,
    val lastUsedAt: Long?,
    val count: Int
){
    fun toImageTag() = ImageTag(id, name, lastUsedAt)
    fun toVideoTag() = VideoTag(id, name, lastUsedAt)
}