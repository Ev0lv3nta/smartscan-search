package com.fpf.smartscan.data.clusters

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.fpf.smartscan.data.MediaTypeConverter
import com.fpf.smartscan.data.metadata.MediaMetadata

@Entity(
    tableName = "media_cluster_crossref",
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
    indices = [Index("clusterId")]
)
@TypeConverters(MediaTypeConverter::class)
data class ClusterCrossRef(
    @PrimaryKey
    val mediaId: Long,
    val clusterId: Long,
)