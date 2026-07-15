package io.github.ev0lv3nta.smartscansearch.data.clusters

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import io.github.ev0lv3nta.smartscansearch.data.metadata.MediaMetadata
import io.github.ev0lv3nta.smartscansearch.media.MediaType

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
data class ClusterCrossRef(
    val mediaId: Long,
    val mediaType: MediaType,
    val clusterId: Long
)