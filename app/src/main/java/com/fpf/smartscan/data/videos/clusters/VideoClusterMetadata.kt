package com.fpf.smartscan.data.videos.clusters

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.fpf.smartscan.data.MediaClusterMetadata

@Entity(
    tableName = "video_cluster_metadata",
    indices = [Index(value = ["label"], unique = true)]
)
data class VideoClusterMetadata (
    @PrimaryKey
    override val clusterId: Long,
    override val prototypeSize: Int,
    override val meanSimilarity: Float = 0f,
    override val stdSimilarity: Float = 0f,
    override val label: String? = null,
): MediaClusterMetadata(clusterId, prototypeSize, meanSimilarity, stdSimilarity, label)