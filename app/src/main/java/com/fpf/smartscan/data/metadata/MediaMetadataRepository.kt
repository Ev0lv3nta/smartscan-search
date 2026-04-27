package com.fpf.smartscan.data.metadata

import com.fpf.smartscan.media.MediaType

class MediaMetadataRepository(
    private val dao: MediaMetadataDao
) {
    suspend fun upsert(items: List<MediaMetadata>) = dao.upsert(items)
    suspend fun upsert(item: MediaMetadata) = dao.upsert(item)
    suspend fun getAllIds(): List<Long> = dao.getAllIds()
    suspend fun getByIds(mediaIds: List<Long>): List<MediaMetadata> = dao.getByIds(mediaIds)
    suspend fun getByType(type: MediaType): List<MediaMetadata> = dao.getByType(type)

    suspend fun getByTag(tagId: Long): List<MediaMetadata> = dao.getByTag(tagId)

    suspend fun getByTag(tagId: Long, limit: Int, offset: Int): List<MediaMetadata> = dao.getByTag(tagId, limit, offset)

    suspend fun getByTagAndType(
        tagId: Long,
        type: MediaType,
        limit: Int,
        offset: Int
    ): List<MediaMetadata> = dao.getByTagAndType(tagId, type, limit, offset)

    suspend fun getByTagAndDateRange(
        tagId: Long,
        startDate: Long,
        endDate: Long,
        limit: Int,
        offset: Int
    ): List<MediaMetadata> = dao.getByTagAndDateRange(tagId, startDate, endDate, limit, offset)

    suspend fun getByTagTypeAndDateRange(
        tagId: Long,
        type: MediaType,
        startDate: Long,
        endDate: Long,
        limit: Int,
        offset: Int
    ): List<MediaMetadata> = dao.getByTagTypeAndDateRange(tagId, type, startDate, endDate, limit, offset)

    suspend fun countByTag(tagId: Long): Int = dao.countByTag(tagId)

    suspend fun countByTagAndType(tagId: Long, type: MediaType): Int =
        dao.countByTagAndType(tagId, type)

    suspend fun countByTagAndDateRange(
        tagId: Long,
        startDate: Long,
        endDate: Long
    ): Int = dao.countByTagAndDateRange(tagId, startDate, endDate)

    suspend fun countByTagTypeAndDateRange(
        tagId: Long,
        type: MediaType,
        startDate: Long,
        endDate: Long
    ): Int = dao.countByTagTypeAndDateRange(tagId, type, startDate, endDate)

    suspend fun getByCluster(clusterId: Long, limit: Int, offset: Int): List<MediaMetadata> = dao.getByCluster(clusterId, limit, offset)
    suspend fun getByCluster(clusterId: Long): List<MediaMetadata> = dao.getByCluster(clusterId)

    suspend fun getByClusterAndType(
        clusterId: Long,
        type: MediaType,
        limit: Int,
        offset: Int
    ): List<MediaMetadata> = dao.getByClusterAndType(clusterId, type, limit, offset)

    suspend fun getByClusterAndDateRange(
        clusterId: Long,
        startDate: Long,
        endDate: Long,
        limit: Int,
        offset: Int
    ): List<MediaMetadata> = dao.getByClusterAndDateRange(clusterId, startDate, endDate, limit, offset)

    suspend fun getByClusterTypeAndDateRange(
        clusterId: Long,
        type: MediaType,
        startDate: Long,
        endDate: Long,
        limit: Int,
        offset: Int
    ): List<MediaMetadata> = dao.getByClusterTypeAndDateRange(clusterId, type, startDate, endDate, limit, offset)

    suspend fun countByCluster(clusterId: Long): Int = dao.countByCluster(clusterId)

    suspend fun countByClusterAndType(clusterId: Long, type: MediaType): Int =
        dao.countByClusterAndType(clusterId, type)

    suspend fun countByClusterAndDateRange(
        clusterId: Long,
        startDate: Long,
        endDate: Long
    ): Int = dao.countByClusterAndDateRange(clusterId, startDate, endDate)

    suspend fun countByClusterTypeAndDateRange(
        clusterId: Long,
        type: MediaType,
        startDate: Long,
        endDate: Long
    ): Int = dao.countByClusterTypeAndDateRange(clusterId, type, startDate, endDate)

    suspend fun clear() = dao.clear()
}