package com.fpf.smartscan.data

import com.fpf.smartscan.data.images.ImageTagCrossRef
import com.fpf.smartscan.data.videos.VideoTagCrossRef

open class MediaTagCrossRef (
    open val mediaId: Long,
    open val tag: String
){
    fun toImageCrossRef() = ImageTagCrossRef(mediaId, tag)
    fun toVideoCrossRef() = VideoTagCrossRef(mediaId, tag)
}