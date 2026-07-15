package io.github.ev0lv3nta.smartscansearch.data.tags

import io.github.ev0lv3nta.smartscansearch.media.MediaType
import kotlinx.coroutines.flow.Flow

class TagCrossRefRepository(private val dao: TagCrossRefDao) {
     suspend fun getAllCrossRefs(): List<TagCrossRef> = dao.getAllCrossRefs()
     suspend fun getTagsForMedia(mediaId: Long): List<Long> = dao.getTagsForMedia(mediaId)
     suspend fun insertTagCrossRefs(crossRefs: List<TagCrossRef>) = dao.insert(crossRefs)
     suspend fun deleteMediaMatchTag(ids: List<Long>, tagId: Long, mediaType: MediaType) = dao.deleteMediaMatchingTag(ids, mediaType, tagId)
     suspend fun clear() = dao.clear()
     fun getTagsWithCounts(): Flow<List<TagWithCount>> = dao.getTagCounts()
}