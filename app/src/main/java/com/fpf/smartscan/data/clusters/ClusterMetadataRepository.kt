package com.fpf.smartscan.data.clusters


import com.fpf.smartscan.media.MediaType
import com.fpf.smartscansdk.core.cluster.ClusterMetadata
import kotlinx.coroutines.flow.Flow
import kotlin.collections.associate

class ClusterMetadataRepository(private val dao: ClusterMetadataDao) {
    fun getAllMetadataFlow(): Flow<List<MediaClusterMetadata>> = dao.getAllFlow()
    fun getMetadataByTypeFlow(type: MediaType, minSize: Int = 1): Flow<List<MediaClusterMetadata>> = dao.getByTypeFlow(type, minSize)

    fun getAllLabelFlow(): Flow<List<String>> = dao.getLabels()

    suspend fun getAllMetadataAsMap(): Map<Long, ClusterMetadata> = dao.getAll().associate {
            it.clusterId to it.toMetadata()
        }

    suspend fun getMetadatas(ids: List<Long>): List<MediaClusterMetadata> = dao.get(ids)

    suspend fun getIdFromLabel(label: String): Long? = dao.getIdFromLabel(label)
    suspend fun count(minSize: Int = 1): Int = dao.count(minSize)

    suspend fun countSingletons(): Int = dao.countSingletons()


    suspend fun insertMetadatas(metadatas: List<MediaClusterMetadata>) = dao.insert(metadatas)

    suspend fun updateMetadatas(metadatas: List<MediaClusterMetadata>) = dao.update(metadatas)

     suspend fun deleteMetadatas(ids: List<Long>) = dao.delete(ids)

    suspend fun clear() = dao.clear()
}