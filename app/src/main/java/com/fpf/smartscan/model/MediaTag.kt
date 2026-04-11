package com.fpf.smartscan.model

import com.fpf.smartscan.data.images.tags.ImageTag
import com.fpf.smartscan.data.videos.tags.VideoTag

open class MediaTag (
    open val id: Long,
    open val name: String,
    open val lastUsedAt: Long? = null
){
    fun toImageMediaTag() = ImageTag(id, name, lastUsedAt)
    fun toVideoMediaTag() = VideoTag(id,name, lastUsedAt)
}