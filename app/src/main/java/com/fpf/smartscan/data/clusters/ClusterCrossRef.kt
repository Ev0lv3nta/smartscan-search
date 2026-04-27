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
    primaryKeys = ["clusterId", "mediaId"],
    foreignKeys = [
        ForeignKey(
            entity = MediaClusterMetadata::class,
            parentColumns = ["clusterId"],
            childColumns = ["clusterId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MediaMetadata::class,
            parentColumns = ["id"],
            childColumns = ["mediaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("mediaId"), Index("type")]
)
@TypeConverters(MediaTypeConverter::class)
data class ClusterCrossRef(
    val clusterId: Long,
    val mediaId: Long,
    val type: MediaType
)