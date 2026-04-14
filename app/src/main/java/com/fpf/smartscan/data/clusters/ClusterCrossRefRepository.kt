package com.fpf.smartscan.data.clusters

import java.util.LinkedHashMap

class ClusterCrossRefRepository(private val dao: ClusterCrossRefDao) {
    private var clusterToMediaIdsMap: LinkedHashMap<Long, MutableSet<Long>> = LinkedHashMap()

    suspend fun getAllClusters(): Set<Long> = dao.getAllClusters().toSet()
    suspend fun getAllMedia(): Set<Long> = dao.getAllMedia().toSet()
    suspend fun getMediaIds(clusterId: Long): Set<Long> = dao.getMediaIds(clusterId).toSet()
    suspend fun getMediaIds(clusterId: Long, limit: Int, offset: Int): Set<Long> = dao.getMediaIds(clusterId, limit, offset).toSet()

    suspend fun getClusterToMediaIdsMap(): LinkedHashMap<Long, MutableSet<Long>> {
        if (clusterToMediaIdsMap.isNotEmpty()) return clusterToMediaIdsMap

        for(ref in dao.getAllCrossRefs()){
            clusterToMediaIdsMap.computeIfAbsent(ref.clusterId) { HashSet() }.add(ref.mediaId)
        }
        return clusterToMediaIdsMap
    }

     suspend fun addMedia(mediaClusterCrossRefs: List<ClusterCrossRef>) = dao.addMedia(mediaClusterCrossRefs)

     suspend fun deleteByClusterIds(ids: List<Long>) = dao.deleteByClusterIds(ids)

    suspend fun deleteByMediaIds(ids: List<Long>) = dao.deleteByMediaIds(ids)

    suspend fun clear() = dao.clear()

     suspend fun count(clusterId: Long): Int = dao.count(clusterId)
}