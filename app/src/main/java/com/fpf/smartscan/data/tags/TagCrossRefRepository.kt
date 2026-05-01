package com.fpf.smartscan.data.tags

import kotlinx.coroutines.flow.Flow

class TagCrossRefRepository(private val dao: TagCrossRefDao) {
     suspend fun getAllCrossRefs(): List<TagCrossRef> = dao.getAllCrossRefs()
     suspend fun getTagsForMedia(mediaId: Long): List<Long> = dao.getTagsForMedia(mediaId)
     suspend fun upsertTagCrossRefs(crossRefs: List<TagCrossRef>) = dao.upsert(crossRefs)
     suspend fun deleteByMediaIds(ids: List<Long>) = dao.deleteByMediaIds(ids)
     suspend fun deleteMediaMatchTag(ids: List<Long>, tagId: Long) = dao.deleteMediaMatchingTag(ids, tagId)
     suspend fun clear() = dao.clear()
     fun getTagsWithCounts(): Flow<List<TagWithCount>> = dao.getTagCounts()
}