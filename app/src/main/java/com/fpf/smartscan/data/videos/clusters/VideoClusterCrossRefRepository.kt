package com.fpf.smartscan.data.videos.clusters

import androidx.room.Transaction
import com.fpf.smartscan.data.images.clusters.ImageClusterMetadataDao
import com.fpf.smartscansdk.core.cluster.Assignments
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.LinkedHashMap

class VideoClusterCrossRefRepository(private val dao: VideoClusterCrossRefDao) {
    var clusterVideoIdsMap: LinkedHashMap<Long, List<Long>> = LinkedHashMap()

    suspend fun getAllClusters(): Set<Long> = dao.getAllClusters().toSet()

    suspend fun getAllVideos(): Set<Long> = dao.getAllVideos().toSet()

    suspend fun getVideosInCluster(clusterId: Long): Set<Long> = dao.getVideosInCluster(clusterId).toSet()

    suspend fun getClusterToVideoIdsMap(): LinkedHashMap<Long, List<Long>> {
        if (clusterVideoIdsMap.isNotEmpty()) return clusterVideoIdsMap

        clusterVideoIdsMap = LinkedHashMap(
            dao.getClusterVideoPairs().groupBy({ it.first }, { it.second })
        )
        return clusterVideoIdsMap
    }

    @Transaction
    suspend fun addVideos(videoClusterCrossRefs: List<VideoClusterCrossRef>) = dao.addVideos(videoClusterCrossRefs)

    suspend fun deleteByClusterIds(ids: List<Long>) = dao.deleteByClusterIds(ids)

    suspend fun deleteByImageIds(ids: List<Long>) = dao.deleteByVideoIds(ids)

    suspend fun clear() = dao.clear()

    suspend fun count(clusterId: Long): Int = dao.count(clusterId)
}