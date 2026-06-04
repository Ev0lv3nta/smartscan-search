package com.fpf.smartscan.data.clusters

import com.fpf.smartscan.media.MediaType
import kotlinx.coroutines.flow.Flow

class ClusterCrossRefRepository(private val dao: ClusterCrossRefDao) {
    private var clusterToMediaIdsMap: MutableMap<Long, MutableSet<Long>> = mutableMapOf()
    private var assignments: MutableMap<Long, Long> = mutableMapOf()
    private var refreshCache: Boolean = false

    suspend fun getAllCrossRefs(): List<ClusterCrossRef> = dao.getAll()
    suspend fun getByType(mediaType: MediaType): List<ClusterCrossRef> = dao.getByType(mediaType)
    suspend fun getByClusterIds(ids: List<Long>):  List<ClusterCrossRef> = dao.getByClusterIds(ids)
    fun getClustersWithCount(): Flow<List<ClusterMetadataWithCount>> = dao.getClustersWithCount()
    suspend fun insertClusterCrossRefs(crossRefs: List<ClusterCrossRef>) {
        dao.insert(crossRefs)
        refreshCache = true
    }

    suspend fun insertClusterCrossRefs(itemIds: List<Long>, clusterId: Long) {
        val crossRefs = itemIds.map { ClusterCrossRef(clusterId = clusterId, mediaId = it) }
        insertClusterCrossRefs(crossRefs)
    }

    suspend fun deleteByClusterIds(ids: List<Long>) {
        dao.deleteByClusterIds(ids)
        refreshCache = true
    }
    suspend fun deleteByMediaIds(ids: List<Long>) {
        dao.deleteByMediaIds(ids)
        refreshCache = true
    }
    suspend fun clear() {
        refreshCache = false
        assignments.clear()
        clusterToMediaIdsMap.clear()
        dao.clear()
    }
    suspend fun count() = dao.count()
    suspend fun count(clusterId: Long) = dao.countByClusterId(clusterId)

    suspend fun getClusterToMediaIdsMap(): Map<Long, MutableSet<Long>> {
        if (clusterToMediaIdsMap.isNotEmpty() && !refreshCache) return clusterToMediaIdsMap

        for(ref in getAllCrossRefs()){
            clusterToMediaIdsMap.computeIfAbsent(ref.clusterId) { HashSet() }.add(ref.mediaId)
        }
        return clusterToMediaIdsMap
    }

    suspend fun getAssignments(): Map<Long, Long> {
        if (assignments.isNotEmpty() && !refreshCache) return assignments

        val map = getClusterToMediaIdsMap()

        for ((clusterId, mediaIds) in map) {
            for (mediaId in mediaIds) {
                assignments[mediaId] = clusterId
            }
        }
        return assignments
    }

    suspend fun updateAssignments(itemIds: List<Long>, newClusterId: Long) {
        val crossRefs = itemIds.map { ClusterCrossRef(clusterId = newClusterId, mediaId = it) }
        deleteByMediaIds(itemIds) // clear old
        insertClusterCrossRefs(crossRefs)
    }
}