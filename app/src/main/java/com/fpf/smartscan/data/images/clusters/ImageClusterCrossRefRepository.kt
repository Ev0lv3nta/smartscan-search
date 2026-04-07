package com.fpf.smartscan.data.images.clusters

import androidx.room.Transaction
import com.fpf.smartscan.data.images.clusters.ImageClusterMetadataDao
import com.fpf.smartscansdk.core.cluster.Assignments
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ImageClusterCrossRefRepository(private val dao: ImageClusterCrossRefDao) {
    val allCluster: Flow<List<Long>> = dao.getAllClustersFlow()
    suspend fun getAllClusters(): List<Long> = dao.getAllClusters()

    val allImages = dao.getAllImagesFlow()
    suspend fun getAllImages(): List<Long> = dao.getAllImages()

    fun getImagesInClusterFlow(clusterId: Long): Flow<List<Long>> = dao.getImagesInClusterFlow(clusterId)
    suspend fun getImagesInCluster(clusterId: Long): List<Long> = dao.getImagesInCluster(clusterId)

    @Transaction
    suspend fun addImages(imageClusterCrossRefs: List<ImageClusterCrossRef>) = dao.addImages(imageClusterCrossRefs)

    suspend fun deleteByClusterIds(ids: List<Long>) = dao.deleteByClusterIds(ids)

    suspend fun deleteByImageIds(ids: List<Long>) = dao.deleteByImageIds(ids)

    suspend fun clear() = dao.clear()

    suspend fun count(clusterId: Long): Int = dao.count(clusterId)
}