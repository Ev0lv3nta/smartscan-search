package com.fpf.smartscan.data.images.clusters

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.fpf.smartscan.data.MediaClusterMetadata
import com.fpf.smartscansdk.core.cluster.ClusterMetadata

@Entity(
    tableName = "image_cluster_metadata",
    indices = [Index(value = ["label"], unique = true)]
)
data class ImageClusterMetadata (
    @PrimaryKey
    override val clusterId: Long,
    override val prototypeSize: Int,
    override val meanSimilarity: Float = 0f,
    override val stdSimilarity: Float = 0f,
    override val label: String? = null,
    ): MediaClusterMetadata


fun ImageClusterMetadata.toMetadata() = ClusterMetadata(
    prototypeSize = prototypeSize,
    meanSimilarity = meanSimilarity,
    stdSimilarity = stdSimilarity,
    label = label
)