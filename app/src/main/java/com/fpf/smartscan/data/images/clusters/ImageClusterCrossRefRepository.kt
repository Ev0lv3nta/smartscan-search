package com.fpf.smartscan.data.images.clusters

import androidx.room.Transaction
import java.util.LinkedHashMap

class ImageClusterCrossRefRepository(private val dao: ImageClusterCrossRefDao) {
    var clusterImageIdsMap: LinkedHashMap<Long, MutableSet<Long>> = LinkedHashMap()

    suspend fun getAllClusters(): Set<Long> = dao.getAllClusters().toSet()
    suspend fun getAllImages(): Set<Long> = dao.getAllImages().toSet()

    suspend fun getImagesInCluster(clusterId: Long): Set<Long> = dao.getImagesInCluster(clusterId).toSet()

    suspend fun getClusterToImageIdsMap(): LinkedHashMap<Long, MutableSet<Long>> {
        if (clusterImageIdsMap.isNotEmpty()) return clusterImageIdsMap

        for(ref in dao.getAllClusterImageCrossRefs()){
            clusterImageIdsMap.computeIfAbsent(ref.clusterId) { HashSet() }.add(ref.imageId)
        }
        return clusterImageIdsMap
    }

    @Transaction
    suspend fun addImages(imageClusterCrossRefs: List<ImageClusterCrossRef>) = dao.addImages(imageClusterCrossRefs)

    suspend fun deleteByClusterIds(ids: List<Long>) = dao.deleteByClusterIds(ids)

    suspend fun deleteByImageIds(ids: List<Long>) = dao.deleteByImageIds(ids)

    suspend fun clear() = dao.clear()

    suspend fun count(clusterId: Long): Int = dao.count(clusterId)
}