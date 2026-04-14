package com.fpf.smartscan.data.clusters


import com.fpf.smartscan.media.MediaType
import com.fpf.smartscansdk.core.cluster.ClusterMetadata
import kotlinx.coroutines.flow.Flow
import kotlin.collections.associate

class ClusterMetadataRepository(private val dao: ClusterMetadataDao) {
    fun getAllMetadataFlow(): Flow<List<MediaClusterMetadata>> = dao.getAllFlow()
    fun getMetadataByTypeFlow(type: MediaType): Flow<List<MediaClusterMetadata>> = dao.getByTypeFlow(type)
    fun getAllLabelFlow(): Flow<List<String>> = dao.getLabels()

     suspend fun getAllMetadataAsMap(): Map<Long, ClusterMetadata> = dao.getAll().associate {
            it.clusterId to it.toMetadata()
        }

     suspend fun getMetadata(id: Long): ClusterMetadata? = dao.get(id)?.toMetadata()

     suspend fun getIdFromLabel(label: String): Long? = dao.getIdFromLabel(label)

     suspend fun upsertMetadatas(metadatas: List<MediaClusterMetadata>) = dao.upsert(metadatas)

     suspend fun deleteMetadata(id: Long) = dao.delete(id)
}