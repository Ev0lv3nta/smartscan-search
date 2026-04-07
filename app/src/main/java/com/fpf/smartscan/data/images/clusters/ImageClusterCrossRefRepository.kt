package com.fpf.smartscan.data.images.clusters

import androidx.room.Transaction
import com.fpf.smartscan.data.images.clusters.ImageClusterMetadataDao
import com.fpf.smartscansdk.core.cluster.Assignments
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.LinkedHashMap

class ImageClusterCrossRefRepository(private val dao: ImageClusterCrossRefDao) {
    var clusterImageIdsMap: LinkedHashMap<Long, List<Long>> = LinkedHashMap()

    suspend fun getAllClusters(): Set<Long> = dao.getAllClusters().toSet()
    suspend fun getAllImages(): Set<Long> = dao.getAllImages().toSet()

    suspend fun getImagesInCluster(clusterId: Long): Set<Long> = dao.getImagesInCluster(clusterId).toSet()

    suspend fun getClusterToImageIdsMap(): LinkedHashMap<Long, List<Long>> {
        if (clusterImageIdsMap.isNotEmpty()) return clusterImageIdsMap

        clusterImageIdsMap = LinkedHashMap(
                dao.getAllClusterImageCrossRefs().groupBy({ it.clusterId }, { it.imageId })
            )
        return clusterImageIdsMap
    }

    @Transaction
    suspend fun addImages(imageClusterCrossRefs: List<ImageClusterCrossRef>) = dao.addImages(imageClusterCrossRefs)

    suspend fun deleteByClusterIds(ids: List<Long>) = dao.deleteByClusterIds(ids)

    suspend fun deleteByImageIds(ids: List<Long>) = dao.deleteByImageIds(ids)

    suspend fun clear() = dao.clear()

    suspend fun count(clusterId: Long): Int = dao.count(clusterId)
}