package com.fpf.smartscan.data.videos.clusters

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.fpf.smartscan.data.MediaClusterCrossRef
import com.fpf.smartscan.data.images.clusters.ImageClusterCrossRef

@Entity(
    tableName = "video_cluster_crossref",
    primaryKeys = ["clusterId", "mediaId"],
    foreignKeys = [
        ForeignKey(
            entity = VideoClusterMetadata::class,
            parentColumns = ["clusterId"],
            childColumns = ["clusterId"],
            onDelete = ForeignKey.Companion.CASCADE
        )
    ],
    indices = [Index("mediaId")]
)

data class VideoClusterCrossRef(
    override val clusterId: Long,
    override val mediaId: Long
): MediaClusterCrossRef
