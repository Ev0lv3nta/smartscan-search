package com.fpf.smartscan.data.images.tags

import com.fpf.smartscan.data.MediaTagCrossRefRepository
import com.fpf.smartscan.data.TagWithCount
import kotlinx.coroutines.flow.Flow

class ImageTagCrossRefRepository(private val dao: ImageTagCrossRefDao): MediaTagCrossRefRepository<ImageTagCrossRef> {
    override suspend fun getAllCrossRefs(): List<ImageTagCrossRef> = dao.getAllCrossRefs()
    override suspend fun getTagsForMedia(mediaId: Long): List<Long> = dao.getTagsForImage(mediaId)
    override suspend fun getMediaIds(tagId: Long): List<Long> = dao.getImageIds(tagId)
    override  suspend fun getMediaIds(tagId: Long, limit: Int, offset: Int): List<Long> = dao.getImageIds(tagId, limit, offset)
    override  suspend fun upsertTagCrossRefs(crossRefs: List<ImageTagCrossRef>) = dao.upsert(crossRefs)
    override  suspend fun upsertTagCrossRefs(tagId: Long, mediaIds: List<Long>) = dao.upsert(mediaIds.map{ ImageTagCrossRef(mediaId = it, tagId=tagId)})
    override suspend fun deleteByMediaIds(ids: List<Long>) = dao.deleteByMediaIds(ids)
    override suspend fun deleteByTagIds(ids: List<Long>) = dao.deleteByTags(ids)
    override suspend fun clear() = dao.clear()
    override suspend fun count(tagId: Long) = dao.count(tagId)
    override fun getTagsWithCounts(): Flow<List<TagWithCount>> = dao.getTagCounts()
}