package com.fpf.smartscan.data.images.clusters

import com.fpf.smartscansdk.core.cluster.ClusterMetadata

class ImageClusterMetadataRepository(private val dao: ImageClusterMetadataDao) {
    val allMetadata = dao.getAllFlow()
    suspend fun getAllMetadata(): Map<Long, ClusterMetadata> = dao.getAll().associate {
            it.clusterId to it.toMetadata()
        }

    suspend fun getMetadata(id: Long): ClusterMetadata? = dao.get(id)?.toMetadata()

    suspend fun upsertMetadatas(metadatas: List<ImageClusterMetadata>) = dao.upsert(metadatas)

    suspend fun deleteMetadata(id: Long) = dao.delete(id)
}