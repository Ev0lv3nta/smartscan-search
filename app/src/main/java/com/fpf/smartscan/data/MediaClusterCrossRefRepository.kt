package com.fpf.smartscan.data

interface MediaClusterCrossRefRepository<T: MediaClusterCrossRef> {
    suspend fun getAllClusters(): Set<Long>

    suspend fun getAllMedia(): Set<Long>

    suspend fun getMediaInCluster(clusterId: Long): Set<Long>

    suspend fun getClusterToMediaIdsMap(): LinkedHashMap<Long, MutableSet<Long>>

    suspend fun addMedia(mediaClusterCrossRefs: List<T>)

    suspend fun deleteByClusterIds(ids: List<Long>)

    suspend fun deleteByMediaIds(ids: List<Long>)

    suspend fun clear()

    suspend fun count(clusterId: Long): Int

}