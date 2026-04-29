package com.fpf.smartscan.data.clusters

import kotlinx.coroutines.flow.Flow

class ClusterCrossRefRepository(private val dao: ClusterCrossRefDao) {
    private var clusterToMediaIdsMap: MutableMap<Long, MutableSet<Long>> = mutableMapOf()
    private var clusterCounts: MutableMap<Long, Int> = mutableMapOf()
    private var assignments: MutableMap<Long, Long> = mutableMapOf()

    suspend fun getAllCrossRefs(): List<ClusterCrossRef> = dao.getAll()
    fun getClustersWithCount(): Flow<List<ClusterMetadataWithCount>> = dao.getClustersWithCount()
    suspend fun getClusterToMediaIdsMap(): Map<Long, MutableSet<Long>> {
        if (clusterToMediaIdsMap.isNotEmpty()) return clusterToMediaIdsMap

        for(ref in dao.getAll()){
            clusterToMediaIdsMap.computeIfAbsent(ref.clusterId) { HashSet() }.add(ref.mediaId)
        }
        return clusterToMediaIdsMap
    }

    suspend fun getAssignments(): Map<Long, Long> {
        if (assignments.isNotEmpty()) return assignments

        val map = getClusterToMediaIdsMap()

        for ((clusterId, mediaIds) in map) {
            for (mediaId in mediaIds) {
                assignments[mediaId] = clusterId
            }
        }
        return assignments
    }

    suspend fun getClusterCounts(): Map<Long, Int> {
        if (clusterCounts.isNotEmpty()) return clusterCounts

        val map = getClusterToMediaIdsMap()

        for ((clusterId, mediaIds) in map) {
            clusterCounts[clusterId] = mediaIds.size
        }
        return clusterCounts
    }

    suspend fun upsertClusterCrossRefs(crossRefs: List<ClusterCrossRef>) = dao.upsert(crossRefs)

    suspend fun deleteByClusterIds(ids: List<Long>) = dao.deleteByClusterIds(ids)
    suspend fun deleteByMediaIds(ids: List<Long>) = dao.deleteByMediaIds(ids)

    suspend fun clear() {
        dao.clear()
        clusterCounts.clear()
        clusterToMediaIdsMap.clear()
        assignments.clear()
    }

}