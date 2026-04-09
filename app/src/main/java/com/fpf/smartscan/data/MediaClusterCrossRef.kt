package com.fpf.smartscan.data

import com.fpf.smartscan.data.images.clusters.ImageClusterCrossRef
import com.fpf.smartscan.data.videos.clusters.VideoClusterCrossRef

open class MediaClusterCrossRef(
    open val clusterId: Long,
    open val mediaId: Long
){
    fun toImageCrossRef() = ImageClusterCrossRef(clusterId, mediaId)
    fun toVideoCrossRef() = VideoClusterCrossRef(clusterId, mediaId)

}