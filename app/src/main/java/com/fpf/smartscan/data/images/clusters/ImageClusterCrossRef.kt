package com.fpf.smartscan.data.images.clusters

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.fpf.smartscan.data.images.clusters.ImageClusterMetadata

@Entity(
    tableName = "image_cluster_crossref",
    primaryKeys = ["clusterId", "imageId"],
    foreignKeys = [
        ForeignKey(
            entity = ImageClusterMetadata::class,
            parentColumns = ["clusterId"],
            childColumns = ["clusterId"],
            onDelete = ForeignKey.Companion.CASCADE
        )
    ],
    indices = [Index("imageId")]
)

data class ImageClusterCrossRef(
    val clusterId: Long,
    val imageId: Long
)