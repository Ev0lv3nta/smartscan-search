package com.fpf.smartscan.data.clusters

import com.fpf.smartscan.media.MediaType
import java.util.LinkedHashMap

class ClusterCrossRefRepository(private val dao: ClusterCrossRefDao) {
    private var clusterToMediaIdsMap: MutableMap<Long, MutableSet<Long>> = mutableMapOf()
    private var assignments: MutableMap<Long, Long> = mutableMapOf()


    suspend fun getAllCrossRefs(): List<ClusterCrossRef> = dao.getAll()
    suspend fun getByClusterId(clusterId: Long): List<ClusterCrossRef> = dao.getByClusterId(clusterId)
    suspend fun getByClusterId(clusterId: Long, limit: Int, offset: Int): List<ClusterCrossRef> = dao.getByClusterId(clusterId, limit, offset)
    suspend fun getByClusterIdAndType(clusterId: Long, type: MediaType): List<ClusterCrossRef> = dao.getByClusterIdAndType(clusterId, type)
    suspend fun getByClusterIdAndType(clusterId: Long, type: MediaType, limit: Int, offset: Int): List<ClusterCrossRef> = dao.getByClusterIdAndType(clusterId, type, limit, offset)

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

     suspend fun count(clusterId: Long): Int = dao.count(clusterId)
}