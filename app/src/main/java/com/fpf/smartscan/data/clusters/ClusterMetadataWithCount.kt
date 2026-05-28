package com.fpf.smartscan.data.clusters

data class ClusterMetadataWithCount (
    val clusterId: Long,
    val prototypeSize: Int,
    val meanSimilarity: Float,
    val stdSimilarity: Float,
    val label: String?,
    val count: Int
)