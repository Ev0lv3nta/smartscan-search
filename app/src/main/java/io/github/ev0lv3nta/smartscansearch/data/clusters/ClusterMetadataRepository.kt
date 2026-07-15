package io.github.ev0lv3nta.smartscansearch.data.clusters

import com.fpf.smartscansdk.core.cluster.ClusterMetadata
import kotlin.collections.associate

class ClusterMetadataRepository(private val dao: ClusterMetadataDao) {
    suspend fun getAllMetadataAsMap(): Map<Long, ClusterMetadata> = dao.getAll().associate {
            it.clusterId to it.toMetadata()
        }

    suspend fun getMetadata(ids: List<Long>): List<MediaClusterMetadata> = dao.get(ids)
    suspend fun getMetadata(id: Long): MediaClusterMetadata? = dao.get(listOf(id)).firstOrNull()

    suspend fun count(minSize: Int = 1): Int = dao.count(minSize)
    suspend fun insertMetadata(metadataBatch: List<MediaClusterMetadata>) = dao.insert(metadataBatch)
    suspend fun insertMetadata(metadata: MediaClusterMetadata) = dao.insert(listOf(metadata))

    suspend fun updateMetadata(metadataBatch: List<MediaClusterMetadata>) = dao.update(metadataBatch)
    suspend fun updateMetadata(metadata: MediaClusterMetadata) = dao.update(listOf(metadata))

    suspend fun deleteMetadata(ids: List<Long>) = dao.delete(ids)
    suspend fun deleteMetadata(id: Long) = dao.delete(listOf(id))

    suspend fun clear() = dao.clear()
}