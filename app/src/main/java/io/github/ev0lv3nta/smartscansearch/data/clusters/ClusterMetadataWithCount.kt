package io.github.ev0lv3nta.smartscansearch.data.clusters

data class ClusterMetadataWithCount (
    val clusterId: Long,
    val prototypeSize: Int,
    val meanSimilarity: Float,
    val stdSimilarity: Float,
    val label: String?,
    val count: Int
)