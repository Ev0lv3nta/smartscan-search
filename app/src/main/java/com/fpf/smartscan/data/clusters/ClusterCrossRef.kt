package com.fpf.smartscan.data.clusters

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.TypeConverters
import com.fpf.smartscan.data.MediaTypeConverter
import com.fpf.smartscan.data.metadata.MediaMetadata
import com.fpf.smartscan.media.MediaType

@Entity(
    tableName = "media_cluster_crossref",
    primaryKeys = ["mediaId", "mediaType"],
    foreignKeys = [
        ForeignKey(
            entity = MediaClusterMetadata::class,
            parentColumns = ["clusterId"],
            childColumns = ["clusterId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MediaMetadata::class,
            parentColumns = ["id", "type"],
            childColumns = ["mediaId", "mediaType"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["clusterId"])
    ]
)
@TypeConverters(MediaTypeConverter::class)
data class ClusterCrossRef(
    val mediaId: Long,
    val mediaType: MediaType,
    val clusterId: Long
)