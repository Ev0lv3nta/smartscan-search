package com.fpf.smartscan.data

import com.fpf.smartscan.data.images.clusters.ImageClusterMetadata
import com.fpf.smartscan.data.videos.clusters.VideoClusterMetadata
import com.fpf.smartscansdk.core.cluster.ClusterMetadata

open class MediaClusterMetadata(
    open val clusterId: Long,
    open val prototypeSize: Int,
    open val meanSimilarity: Float = 0f,
    open val stdSimilarity: Float = 0f,
    open val label: String? = null
) {
    fun toMetadata() = ClusterMetadata(
        prototypeSize = prototypeSize,
        meanSimilarity = meanSimilarity,
        stdSimilarity = stdSimilarity,
        label = label
    )
    fun toVideoClusterMetadata() = VideoClusterMetadata(
        clusterId = clusterId,
        prototypeSize = prototypeSize,
        meanSimilarity = meanSimilarity,
        stdSimilarity = stdSimilarity,
        label = label
    )

    fun toImageClusterMetadata() = ImageClusterMetadata(
        clusterId = clusterId,
        prototypeSize = prototypeSize,
        meanSimilarity = meanSimilarity,
        stdSimilarity = stdSimilarity,
        label = label
    )
}