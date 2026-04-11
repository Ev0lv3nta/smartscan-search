package com.fpf.smartscan.data

import com.fpf.smartscan.data.images.tags.ImageTagCrossRef
import com.fpf.smartscan.data.videos.tags.VideoTagCrossRef

open class MediaTagCrossRef (
    open val mediaId: Long,
    open val tagId: Long
){
    fun toImageCrossRef() = ImageTagCrossRef(mediaId, tagId)
    fun toVideoCrossRef() = VideoTagCrossRef(mediaId, tagId)
}