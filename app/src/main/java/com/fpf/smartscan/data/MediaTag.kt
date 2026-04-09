package com.fpf.smartscan.data

import com.fpf.smartscan.data.images.ImageTag
import com.fpf.smartscan.data.videos.VideoTag

open class MediaTag (
    open val name: String,
    open val lastUsedAt: Long? = null
){
    fun toImageMediaTag() = ImageTag(name, lastUsedAt)
    fun toVideoMediaTag() = VideoTag(name, lastUsedAt)
}