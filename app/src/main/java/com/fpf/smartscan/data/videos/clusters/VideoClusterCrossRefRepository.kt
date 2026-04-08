package com.fpf.smartscan.data.videos.clusters

import androidx.room.Transaction
import java.util.LinkedHashMap

class VideoClusterCrossRefRepository(private val dao: VideoClusterCrossRefDao) {
    var clusterVideoIdsMap:  LinkedHashMap<Long, MutableSet<Long>>  = LinkedHashMap()

    suspend fun getAllClusters(): Set<Long> = dao.getAllClusters().toSet()

    suspend fun getAllVideos(): Set<Long> = dao.getAllVideos().toSet()

    suspend fun getVideosInCluster(clusterId: Long): Set<Long> = dao.getVideosInCluster(clusterId).toSet()

    suspend fun getClusterToVideoIdsMap(): LinkedHashMap<Long, MutableSet<Long>> {
        if (clusterVideoIdsMap.isNotEmpty()) return clusterVideoIdsMap

        for (ref in dao.getAllClusterVideoCrossRefs()) {
            clusterVideoIdsMap.computeIfAbsent(ref.clusterId) { HashSet() }.add(ref.videoId)
        }
        return clusterVideoIdsMap
    }

    @Transaction
    suspend fun addVideos(videoClusterCrossRefs: List<VideoClusterCrossRef>) = dao.addVideos(videoClusterCrossRefs)

    suspend fun deleteByClusterIds(ids: List<Long>) = dao.deleteByClusterIds(ids)

    suspend fun deleteByImageIds(ids: List<Long>) = dao.deleteByVideoIds(ids)

    suspend fun clear() = dao.clear()

    suspend fun count(clusterId: Long): Int = dao.count(clusterId)
}