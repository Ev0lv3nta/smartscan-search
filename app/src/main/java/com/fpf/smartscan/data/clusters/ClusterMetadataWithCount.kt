package com.fpf.smartscan.data.clusters

import com.fpf.smartscan.media.MediaType

data class ClusterMetadataWithCount (
    val clusterId: Long,
    val prototypeSize: Int,
    val meanSimilarity: Float,
    val stdSimilarity: Float,
    val label: String?,
    val type: MediaType,
    val count: Int
)