package com.fpf.smartscan.data.clusters

import com.fpf.smartscan.media.MediaType
import kotlinx.coroutines.flow.Flow

class ClusterCrossRefRepository(private val dao: ClusterCrossRefDao) {
    suspend fun getAllCrossRefs(): List<ClusterCrossRef> = dao.getAll()
    suspend fun getByType(mediaType: MediaType): List<ClusterCrossRef> = dao.getByType(mediaType)
    suspend fun getByClusterIds(ids: List<Long>):  List<ClusterCrossRef> = dao.getByClusterIds(ids)
    fun getClustersWithCount(): Flow<List<ClusterMetadataWithCount>> = dao.getClustersWithCount()
    suspend fun insertClusterCrossRefs(crossRefs: List<ClusterCrossRef>) = dao.insert(crossRefs)
    suspend fun deleteByClusterIds(ids: List<Long>) = dao.deleteByClusterIds(ids)
    suspend fun deleteByMediaIds(ids: List<Long>) = dao.deleteByMediaIds(ids)
    suspend fun clear() = dao.clear()
}