package com.fpf.smartscan.data

import com.fpf.smartscansdk.core.cluster.ClusterMetadata
import kotlinx.coroutines.flow.Flow

interface MediaClusterMetadataRepository {
    val allMetadata: Flow<Map<Long, ClusterMetadata>>

    suspend fun getAllMetadata(): Map<Long, ClusterMetadata>

    suspend fun getMetadata(id: Long): ClusterMetadata?

    suspend fun upsertMetadatas(metadatas: List<MediaClusterMetadata>)

    suspend fun deleteMetadata(id: Long)
}