package io.github.ev0lv3nta.smartscansearch.data.clusters

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import io.github.ev0lv3nta.smartscansearch.data.MediaTypeConverter
import io.github.ev0lv3nta.smartscansearch.media.MediaType
import com.fpf.smartscansdk.core.cluster.ClusterMetadata

@Entity(
    tableName = "cluster_metadata",
    indices = [
        Index(value = ["label"], unique = true),
    ])
@TypeConverters(MediaTypeConverter::class)
data class MediaClusterMetadata (
    @PrimaryKey
    val clusterId: Long,
    val prototypeSize: Int,
    val meanSimilarity: Float = 0f,
    val stdSimilarity: Float = 0f,
    val label: String? = null,
    )


fun MediaClusterMetadata.toMetadata() = ClusterMetadata(
    prototypeSize = prototypeSize,
    meanSimilarity = meanSimilarity,
    stdSimilarity = stdSimilarity,
    label = label
)