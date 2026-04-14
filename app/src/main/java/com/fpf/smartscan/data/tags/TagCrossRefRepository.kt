package com.fpf.smartscan.data.tags

import kotlinx.coroutines.flow.Flow

class TagCrossRefRepository(private val dao: TagCrossRefDao) {
     suspend fun getAllCrossRefs(): List<TagCrossRef> = dao.getAllCrossRefs()
     suspend fun getTagsForMedia(mediaId: Long): List<Long> = dao.getTagsForMedia(mediaId)
     suspend fun getMediaIds(tagId: Long): List<Long> = dao.getMediaIds(tagId)
     fun getMediaIdsFlow(tagId: Long): Flow<List<Long>> = dao.getMediaIdsFlow(tagId)
      suspend fun getMediaIds(tagId: Long, limit: Int, offset: Int): List<Long> = dao.getMediaIds(tagId, limit, offset)
      suspend fun upsertTagCrossRefs(crossRefs: List<TagCrossRef>) = dao.upsert(crossRefs)
      suspend fun upsertTagCrossRefs(tagId: Long, mediaIds: List<Long>) = dao.upsert(mediaIds.map{ TagCrossRef(mediaId = it, tagId=tagId)})
     suspend fun deleteByMediaIds(ids: List<Long>) = dao.deleteByMediaIds(ids)
     suspend fun deleteMediaMatchTag(ids: List<Long>, tagId: Long) = dao.deleteMediaMatchingTag(ids, tagId)
     suspend fun deleteByTagIds(ids: List<Long>) = dao.deleteByTags(ids)
     suspend fun clear() = dao.clear()
     suspend fun count(tagId: Long) = dao.count(tagId)
     fun getTagsWithCounts(): Flow<List<TagWithCount>> = dao.getTagCounts()
}