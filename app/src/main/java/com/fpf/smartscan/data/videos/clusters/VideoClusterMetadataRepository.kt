package com.fpf.smartscan.data.videos.clusters

import com.fpf.smartscansdk.core.cluster.ClusterMetadata

class VideoClusterMetadataRepository(private val dao: VideoClusterMetadataDao) {
    val allMetadata = dao.getAllFlow()
    suspend fun getAllMetadata(): Map<Long, ClusterMetadata> = dao.getAll().associate {
        it.clusterId to it.toMetadata()
    }

    suspend fun getMetadata(id: Long): ClusterMetadata? = dao.get(id)?.toMetadata()

    suspend fun upsertMetadatas(metadatas: List<VideoClusterMetadata>) = dao.upsert(metadatas)

    suspend fun deleteMetadata(id: Long) = dao.delete(id)
}