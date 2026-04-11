package com.fpf.smartscan.data.images.tags

import com.fpf.smartscan.data.MediaTagCrossRefRepository
import com.fpf.smartscan.data.MediaTag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ImageTagCrossRefRepository(private val dao: ImageTagCrossRefDao): MediaTagCrossRefRepository<ImageTagCrossRef> {
    override suspend fun getAllCrossRefs(): List<ImageTagCrossRef> = dao.getAllCrossRefs()
    override suspend fun getTagsForMedia(mediaId: Long): List<Long> = dao.getTagsForImage(mediaId)
    override suspend fun getMediaIds(tagId: Long): List<Long> = dao.getImageIds(tagId)
    override  suspend fun getMediaIds(tagId: Long, limit: Int, offset: Int): List<Long> = dao.getImageIds(tagId, limit, offset)
    override  suspend fun upsertTagCrossRefs(crossRefs: List<ImageTagCrossRef>) = dao.upsert(crossRefs)
    override suspend fun deleteByMediaIds(ids: List<Long>) = dao.deleteByMediaIds(ids)
    override suspend fun deleteByTagIds(ids: List<Long>) = dao.deleteByTags(ids)
    override suspend fun clear() = dao.clear()
    override suspend fun count(tagId: Long) = dao.count(tagId)
    override fun getTagCounts(): Flow<Map<MediaTag, Int>> = dao.getTagCounts().map{ list -> list.associate { it.toImageTag() to it.count } }
    override suspend fun mergeTags(primaryTag: Long, tagsToMerge: List<Long>) = dao.mergeTags(primaryTag, tagsToMerge)
}