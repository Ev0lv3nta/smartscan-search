package com.fpf.smartscan.data.clusters

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "media_cluster_crossref",
    primaryKeys = ["clusterId", "mediaId"],
    foreignKeys = [
        ForeignKey(
            entity = MediaClusterMetadata::class,
            parentColumns = ["clusterId"],
            childColumns = ["clusterId"],
            onDelete = ForeignKey.Companion.CASCADE
        )
    ],
    indices = [Index("mediaId")]
)

data class ClusterCrossRef(
    val clusterId: Long,
    val mediaId: Long
)