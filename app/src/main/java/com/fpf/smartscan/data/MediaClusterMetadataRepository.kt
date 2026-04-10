package com.fpf.smartscan.data

import com.fpf.smartscansdk.core.cluster.ClusterMetadata
import kotlinx.coroutines.flow.Flow

interface MediaClusterMetadataRepository {
    val allMetadata: Flow<List<MediaClusterMetadata>>
    val allLabels: Flow<List<String>>

    suspend fun getAllMetadata(): Map<Long, ClusterMetadata>

    suspend fun getMetadata(id: Long): ClusterMetadata?

    suspend fun getIdFromLabel(label: String): Long?

    suspend fun upsertMetadatas(metadatas: List<MediaClusterMetadata>)

    suspend fun deleteMetadata(id: Long)
}