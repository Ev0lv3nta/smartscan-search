package com.fpf.smartscan.data.tags

import com.fpf.smartscan.media.MediaType
import kotlinx.coroutines.flow.Flow

class TagCrossRefRepository(private val dao: TagCrossRefDao) {
     suspend fun getAllCrossRefs(): List<TagCrossRef> = dao.getAllCrossRefs()
     suspend fun getTagsForMedia(mediaId: Long): List<Long> = dao.getTagsForMedia(mediaId)
     suspend fun getByTag(tagId: Long): List<TagCrossRef> = dao.getByTag(tagId)
     suspend fun getByTag(tagId: Long, limit: Int, offset: Int): List<TagCrossRef> = dao.getByTag(tagId, limit, offset)
     suspend fun getByTagAndType(tagId: Long, type: MediaType): List<TagCrossRef> = dao.getByTagAndType(tagId, type)
     suspend fun getByTagAndType(tagId: Long, type: MediaType, limit: Int, offset: Int): List<TagCrossRef> = dao.getByTagAndType(tagId, type, limit, offset)
     suspend fun upsertTagCrossRefs(crossRefs: List<TagCrossRef>) = dao.upsert(crossRefs)
     suspend fun deleteByMediaIds(ids: List<Long>) = dao.deleteByMediaIds(ids)
     suspend fun deleteMediaMatchTag(ids: List<Long>, tagId: Long) = dao.deleteMediaMatchingTag(ids, tagId)
     suspend fun clear() = dao.clear()
     suspend fun count(tagId: Long) = dao.count(tagId)
     fun getTagsWithCounts(): Flow<List<TagWithCount>> = dao.getTagCounts()
}