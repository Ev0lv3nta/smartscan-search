package com.fpf.smartscan.data

import kotlinx.coroutines.flow.Flow

interface MediaTagCrossRefRepository<T: MediaTagCrossRef> {

    suspend fun getAllCrossRefs(): List<T>

    suspend fun getTagsForMedia(mediaId: Long): List<Long>

    suspend fun getMediaIds(tagId: Long): List<Long>

    suspend fun getMediaIds(tagId: Long, limit: Int, offset: Int = 0): List<Long>

    suspend fun upsertTagCrossRefs(crossRefs: List<T>)
    suspend fun upsertTagCrossRefs(tagId: Long, mediaIds: List<Long>)

    suspend fun deleteByMediaIds(ids: List<Long>)

    suspend fun deleteByTagIds(ids: List<Long>)

    suspend fun clear()

    suspend fun count(tagId: Long): Int

    fun getTagCounts():  Flow<Map<MediaTag, Int>>
}