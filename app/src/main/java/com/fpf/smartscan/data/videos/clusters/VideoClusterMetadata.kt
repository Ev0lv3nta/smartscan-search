package com.fpf.smartscan.data.videos.clusters

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "video_cluster_metadata")
data class VideoClusterMetadata (
    @PrimaryKey
    val clusterId: Long,
    val prototypeSize: Int,
    val meanSimilarity: Float = 0f,
    val stdSimilarity: Float = 0f,
    val label: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    )