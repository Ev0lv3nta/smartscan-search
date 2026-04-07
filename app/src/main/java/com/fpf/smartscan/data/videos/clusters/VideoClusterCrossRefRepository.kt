package com.fpf.smartscan.data.videos.clusters

import androidx.room.Transaction
import com.fpf.smartscan.data.images.clusters.ImageClusterMetadataDao
import com.fpf.smartscansdk.core.cluster.Assignments
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class VideoClusterCrossRefRepository(private val dao: VideoClusterCrossRefDao) {
    suspend fun getAllClusters(): Set<Long> = dao.getAllClusters()

    suspend fun getAllVideos(): Set<Long> = dao.getAllVideos()

    suspend fun getVideosInCluster(clusterId: Long): Set<Long> = dao.getVideosInCluster(clusterId)

    @Transaction
    suspend fun addVideos(videoClusterCrossRefs: List<VideoClusterCrossRef>) = dao.addVideos(videoClusterCrossRefs)

    suspend fun deleteByClusterIds(ids: List<Long>) = dao.deleteByClusterIds(ids)

    suspend fun deleteByImageIds(ids: List<Long>) = dao.deleteByVideoIds(ids)

    suspend fun clear() = dao.clear()

    suspend fun count(clusterId: Long): Int = dao.count(clusterId)
}