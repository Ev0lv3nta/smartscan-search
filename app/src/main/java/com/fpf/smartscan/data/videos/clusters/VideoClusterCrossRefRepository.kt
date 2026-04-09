package com.fpf.smartscan.data.videos.clusters

import com.fpf.smartscan.data.MediaClusterCrossRef
import com.fpf.smartscan.data.MediaClusterCrossRefRepository
import java.util.LinkedHashMap

class VideoClusterCrossRefRepository(private val dao: VideoClusterCrossRefDao): MediaClusterCrossRefRepository {
    private var clusterVideoIdsMap:  LinkedHashMap<Long, MutableSet<Long>>  = LinkedHashMap()

    override suspend fun getAllClusters(): Set<Long> = dao.getAllClusters().toSet()

    override suspend fun getAllMedia(): Set<Long> = dao.getAllVideos().toSet()

    override suspend fun getMediaInCluster(clusterId: Long): Set<Long> = dao.getVideosInCluster(clusterId).toSet()

    override suspend fun getClusterToMediaIdsMap(): LinkedHashMap<Long, MutableSet<Long>> {
        if (clusterVideoIdsMap.isNotEmpty()) return clusterVideoIdsMap

        for (ref in dao.getAllClusterVideoCrossRefs()) {
            clusterVideoIdsMap.computeIfAbsent(ref.clusterId) { HashSet() }.add(ref.mediaId)
        }
        return clusterVideoIdsMap
    }

    override suspend fun addMedia(mediaClusterCrossRefs: List<MediaClusterCrossRef>) = dao.addVideos(mediaClusterCrossRefs.map{it.toVideoCrossRef()})

    override suspend fun deleteByClusterIds(ids: List<Long>) = dao.deleteByClusterIds(ids)

    override suspend fun deleteByMediaIds(ids: List<Long>) = dao.deleteByVideoIds(ids)

    override suspend fun clear() = dao.clear()

    override suspend fun count(clusterId: Long): Int = dao.count(clusterId)
}