package com.fpf.smartscan.data.clusters

import java.util.LinkedHashMap

class ClusterCrossRefRepository(private val dao: ClusterCrossRefDao) {
    private var clusterToMediaIdsMap: LinkedHashMap<Long, MutableSet<Long>> = LinkedHashMap()

    suspend fun getAllClusters(): List<Long> = dao.getAllClusters()
    suspend fun getAllMedia(): List<Long> = dao.getAllMedia()
    suspend fun getMediaIds(clusterId: Long): List<Long> = dao.getMediaIds(clusterId)
    suspend fun getMediaIds(clusterId: Long, limit: Int, offset: Int): List<Long> = dao.getMediaIds(clusterId, limit, offset)

    suspend fun getClusterToMediaIdsMap(): LinkedHashMap<Long, MutableSet<Long>> {
        if (clusterToMediaIdsMap.isNotEmpty()) return clusterToMediaIdsMap

        for(ref in dao.getAllCrossRefs()){
            clusterToMediaIdsMap.computeIfAbsent(ref.clusterId) { HashSet() }.add(ref.mediaId)
        }
        return clusterToMediaIdsMap
    }

    suspend fun upsertClusterCrossRefs(crossRefs: List<ClusterCrossRef>) = dao.upsert(crossRefs)

    suspend fun upsertClusterCrossRefs(clusterId: Long, mediaIds: List<Long>) = dao.upsert(mediaIds.map{ ClusterCrossRef(mediaId = it, clusterId = clusterId)})


    suspend fun deleteByClusterIds(ids: List<Long>) = dao.deleteByClusterIds(ids)

    suspend fun deleteByMediaIds(ids: List<Long>) = dao.deleteByMediaIds(ids)

    suspend fun clear() = dao.clear()

     suspend fun count(clusterId: Long): Int = dao.count(clusterId)
}