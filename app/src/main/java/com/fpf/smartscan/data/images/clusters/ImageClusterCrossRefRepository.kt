package com.fpf.smartscan.data.images.clusters

import com.fpf.smartscan.data.MediaClusterCrossRef
import com.fpf.smartscan.data.MediaClusterCrossRefRepository
import java.util.LinkedHashMap

class ImageClusterCrossRefRepository(private val dao: ImageClusterCrossRefDao): MediaClusterCrossRefRepository {
    private var clusterImageIdsMap: LinkedHashMap<Long, MutableSet<Long>> = LinkedHashMap()

    override suspend fun getAllClusters(): Set<Long> = dao.getAllClusters().toSet()
    override suspend fun getAllMedia(): Set<Long> = dao.getAllImages().toSet()
    override suspend fun getMediaInCluster(clusterId: Long): Set<Long> = dao.getImagesInCluster(clusterId).toSet()

    override suspend fun getClusterToMediaIdsMap(): LinkedHashMap<Long, MutableSet<Long>> {
        if (clusterImageIdsMap.isNotEmpty()) return clusterImageIdsMap

        for(ref in dao.getAllClusterImageCrossRefs()){
            clusterImageIdsMap.computeIfAbsent(ref.clusterId) { HashSet() }.add(ref.mediaId)
        }
        return clusterImageIdsMap
    }

    override suspend fun addMedia(mediaClusterCrossRefs: List<MediaClusterCrossRef>) = dao.addImages(mediaClusterCrossRefs.map{it.toImageCrossRef()})

    override suspend fun deleteByClusterIds(ids: List<Long>) = dao.deleteByClusterIds(ids)

    override suspend fun deleteByMediaIds(ids: List<Long>) = dao.deleteByImageIds(ids)

    override suspend fun clear() = dao.clear()

    override suspend fun count(clusterId: Long): Int = dao.count(clusterId)
}