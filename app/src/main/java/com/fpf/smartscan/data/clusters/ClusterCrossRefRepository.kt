package com.fpf.smartscan.data.clusters

import com.fpf.smartscan.media.MediaType
import kotlinx.coroutines.flow.Flow

class ClusterCrossRefRepository(private val dao: ClusterCrossRefDao) {
    suspend fun getAllCrossRefs(): List<ClusterCrossRef> = dao.getAll()
    suspend fun getByType(mediaType: MediaType): List<ClusterCrossRef> = dao.getByType(mediaType)
    fun getClustersWithCount(): Flow<List<ClusterMetadataWithCount>> = dao.getClustersWithCount()
    suspend fun upsertClusterCrossRefs(crossRefs: List<ClusterCrossRef>) = dao.upsert(crossRefs)
    suspend fun deleteByClusterIds(ids: List<Long>) = dao.deleteByClusterIds(ids)
    suspend fun deleteByMediaIds(ids: List<Long>) = dao.deleteByMediaIds(ids)
    suspend fun clear() = dao.clear()
}