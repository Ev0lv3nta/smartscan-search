package com.fpf.smartscan.data.clusters

import kotlinx.coroutines.flow.Flow

class ClusterCrossRefRepository(private val dao: ClusterCrossRefDao) {
    private var clusterToMediaIdsMap: MutableMap<Long, MutableSet<Long>> = mutableMapOf()
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

        for(ref in dao.getAll()){
            assignments[ref.mediaId] = ref.clusterId
        }
        return assignments
    }
    suspend fun upsertClusterCrossRefs(crossRefs: List<ClusterCrossRef>) = dao.upsert(crossRefs)

    suspend fun deleteByClusterIds(ids: List<Long>) = dao.deleteByClusterIds(ids)
    suspend fun deleteByMediaIds(ids: List<Long>) = dao.deleteByMediaIds(ids)

    suspend fun clear() = dao.clear()

}