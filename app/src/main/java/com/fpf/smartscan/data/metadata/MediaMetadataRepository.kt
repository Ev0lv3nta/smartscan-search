package com.fpf.smartscan.data.metadata

import com.fpf.smartscan.media.MediaType

class MediaMetadataRepository(
    private val dao: MediaMetadataDao
) {
    suspend fun insert(items: List<MediaMetadata>) = dao.insert(items)

    suspend fun insert(item: MediaMetadata) = dao.insert(item)

    suspend fun update(items: List<MediaMetadata>) = dao.update(items)

    suspend fun update(item: MediaMetadata) = dao.update(item)

    suspend fun getAllIds(): List<Long> = dao.getAllIds()

    suspend fun getByIds(mediaIds: List<Long>): List<MediaMetadata> = dao.getByIds(mediaIds)
    suspend fun getByType(type: MediaType): List<MediaMetadata> = dao.getByType(type)

    suspend fun getByTag(tagId: Long): List<MediaMetadata> = dao.getByTag(tagId)

    suspend fun getByTag(tagId: Long, limit: Int, offset: Int): List<MediaMetadata> = dao.getByTag(tagId, limit, offset)

    suspend fun getByTag(tagId: Long, type: MediaType, limit: Int, offset: Int): List<MediaMetadata> = dao.getByTag(tagId, type, limit, offset)
    suspend fun getByTag(tagId: Long, type: MediaType): List<MediaMetadata> = dao.getByTag(tagId, type)

    suspend fun getByTag(tagId: Long, startDate: Long?, endDate: Long?, limit: Int, offset: Int): List<MediaMetadata> = dao.getByTag(tagId, startDate, endDate, limit, offset)

    suspend fun getByTag(tagId: Long, type: MediaType, startDate: Long?, endDate: Long?, limit: Int, offset: Int): List<MediaMetadata> = dao.getByTag(tagId, type, startDate, endDate, limit, offset)

    suspend fun getByTag(tagId: Long, type: MediaType, startDate: Long?, endDate: Long?): List<MediaMetadata> = dao.getByTag(tagId, type, startDate, endDate)

    suspend fun countByTag(tagId: Long): Int = dao.countByTag(tagId)

    suspend fun countByTag(tagId: Long, type: MediaType): Int = dao.countByTag(tagId, type)

    suspend fun countByTag(tagId: Long, startDate: Long?, endDate: Long?): Int = dao.countByTag(tagId, startDate, endDate)

    suspend fun countByTag(tagId: Long, type: MediaType, startDate: Long?, endDate: Long?): Int = dao.countByTag(tagId, type, startDate, endDate)
    suspend fun getByCluster(clusterId: Long, limit: Int, offset: Int): List<MediaMetadata> = dao.getByCluster(clusterId, limit, offset)
    suspend fun getByCluster(clusterId: Long): List<MediaMetadata> = dao.getByCluster(clusterId)
    suspend fun getByCluster(clusterId: Long, type: MediaType, limit: Int, offset: Int): List<MediaMetadata> = dao.getByCluster(clusterId, type, limit, offset)

    suspend fun getByCluster(clusterId: Long, startDate: Long?, endDate: Long?, limit: Int, offset: Int): List<MediaMetadata> = dao.getByCluster(clusterId, startDate, endDate, limit, offset)

    suspend fun getByCluster(clusterId: Long, type: MediaType, startDate: Long?, endDate: Long?, limit: Int, offset: Int): List<MediaMetadata> = dao.getByCluster(clusterId, type, startDate, endDate, limit, offset)

    suspend fun countByCluster(clusterId: Long): Int = dao.countByCluster(clusterId)

    suspend fun countByCluster(clusterId: Long, type: MediaType): Int = dao.countByCluster(clusterId, type)

    suspend fun countByCluster(clusterId: Long, startDate: Long, endDate: Long): Int = dao.countByCluster(clusterId, startDate, endDate)

    suspend fun countByCluster(clusterId: Long, type: MediaType, startDate: Long?, endDate: Long?): Int = dao.countByCluster(clusterId, type, startDate, endDate)

    suspend fun deleteByTag(tagId: Long) = dao.deleteByTag(tagId)
    suspend fun deleteByCluster(clusterId: Long) = dao.deleteByCluster(clusterId)

    suspend fun deleteByMediaIds(ids: List<Long>) = dao.deleteByIds(ids)

    suspend fun clear() = dao.clear()
}