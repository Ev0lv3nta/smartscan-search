package com.fpf.smartscan.data.videos.clusters

import androidx.room.Transaction
import com.fpf.smartscan.data.images.clusters.ImageClusterMetadataDao
import com.fpf.smartscansdk.core.cluster.Assignments
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class VideoClusterCrossRefRepository(private val dao: VideoClusterCrossRefDao) {
    val allClusters: Flow<List<Long>> = dao.getAllClustersFlow()
    suspend fun getAllClusters(): List<Long> = dao.getAllClusters()

    val allVideos: Flow<List<Long>> = dao.getAllVideosFlow()
    suspend fun getAllVideos(): List<Long> = dao.getAllVideos()

    fun getVideosInClusterFlow(clusterId: Long): Flow<List<Long>> = dao.getVideosInClusterFlow(clusterId)
    suspend fun getVideosInCluster(clusterId: Long): List<Long> = dao.getVideosInCluster(clusterId)

    @Transaction
    suspend fun addVideos(videoClusterCrossRefs: List<VideoClusterCrossRef>) = dao.addVideos(videoClusterCrossRefs)

    suspend fun deleteByClusterIds(ids: List<Long>) = dao.deleteByClusterIds(ids)

    suspend fun deleteByImageIds(ids: List<Long>) = dao.deleteByVideoIds(ids)

    suspend fun clear() = dao.clear()

    suspend fun count(clusterId: Long): Int = dao.count(clusterId)
}