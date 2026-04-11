package com.fpf.smartscan.data

import com.fpf.smartscan.model.MediaTag
import kotlinx.coroutines.flow.Flow


interface MediaTagCrossRefRepository {

    suspend fun getAllCrossRefs(): List<MediaTagCrossRef>

    suspend fun getTagsForMedia(mediaId: Long): List<Long>

    suspend fun getMediaIds(tagId: Long): List<Long>

    suspend fun getMediaIds(tagId: Long, limit: Int, offset: Int = 0): List<Long>

    suspend fun upsertTagCrossRefs(crossRefs: List<MediaTagCrossRef>)

    suspend fun deleteByMediaIds(ids: List<Long>)

    suspend fun deleteByTagIds(ids: List<Long>)

    suspend fun clear()

    suspend fun count(tagId: Long): Int

    fun getTagCounts():  Flow<Map<MediaTag, Int>>
}