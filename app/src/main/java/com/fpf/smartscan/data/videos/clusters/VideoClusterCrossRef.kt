package com.fpf.smartscan.data.videos.clusters

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "video_cluster_crossref",
    primaryKeys = ["clusterId", "videoId"],
    foreignKeys = [
        ForeignKey(
            entity = VideoClusterMetadata::class,
            parentColumns = ["clusterId"],
            childColumns = ["videoId"],
            onDelete = ForeignKey.Companion.CASCADE
        )
    ],
    indices = [Index("videoId")]
)

data class VideoClusterCrossRef(
    val clusterId: Long,
    val videoId: Long
)