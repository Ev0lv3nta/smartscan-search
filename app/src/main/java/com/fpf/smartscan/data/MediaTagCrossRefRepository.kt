package com.fpf.smartscan.data


interface MediaTagCrossRefRepository {

    suspend fun getAllCrossRefs(): List<MediaTagCrossRef>
    suspend fun getTagToMediaIdsMap(): Map<String, List<Long>>

    suspend fun getTagsForMedia(mediaId: Long): List<String>

    suspend fun getMediaIds(tag: String): List<Long>

    suspend fun getMediaIds(tag: String, limit: Int, offset: Int = 0): List<Long>

    suspend fun upsertTagCrossRefs(crossRefs: List<MediaTagCrossRef>)

    suspend fun deleteByIds(ids: List<Long>)

    suspend fun deleteByTags(tags: List<String>)

    suspend fun clear()

    suspend fun count(tag: String): Int
}