package com.fpf.smartscan.data.images.clusters

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.fpf.smartscan.data.MediaClusterCrossRef
import com.fpf.smartscan.data.images.clusters.ImageClusterMetadata

@Entity(
    tableName = "image_cluster_crossref",
    primaryKeys = ["clusterId", "mediaId"],
    foreignKeys = [
        ForeignKey(
            entity = ImageClusterMetadata::class,
            parentColumns = ["clusterId"],
            childColumns = ["clusterId"],
            onDelete = ForeignKey.Companion.CASCADE
        )
    ],
    indices = [Index("mediaId")]
)

data class ImageClusterCrossRef(
    override val clusterId: Long,
    override val mediaId: Long
): MediaClusterCrossRef(clusterId, mediaId)