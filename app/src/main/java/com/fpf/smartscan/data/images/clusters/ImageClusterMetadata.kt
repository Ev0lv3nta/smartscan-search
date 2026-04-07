package com.fpf.smartscan.data.images.clusters

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fpf.smartscansdk.core.cluster.ClusterMetadata

@Entity(tableName = "image_cluster_metadata")
data class ImageClusterMetadata (
    @PrimaryKey
    val clusterId: Long,
    val prototypeSize: Int,
    val meanSimilarity: Float = 0f,
    val stdSimilarity: Float = 0f,
    val label: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    ){
    fun toMetadata() = ClusterMetadata(
        prototypeSize=prototypeSize,
        meanSimilarity=meanSimilarity,
        stdSimilarity=stdSimilarity,
        label=label
    )
}