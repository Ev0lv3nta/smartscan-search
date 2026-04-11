package com.fpf.smartscan.data.images.tags

import com.fpf.smartscan.data.MediaTagCrossRef
import com.fpf.smartscan.data.MediaTagCrossRefRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ImageTagCrossRefRepository(private val dao: ImageTagCrossRefDao): MediaTagCrossRefRepository {
    override suspend fun getAllCrossRefs(): List<MediaTagCrossRef> = dao.getAllCrossRefs()
    override suspend fun getTagToMediaIdsMap(): Map<String, List<Long>> = dao.getAllCrossRefs().groupBy({it.tag}, {it.mediaId})
    override suspend fun getTagsForMedia(mediaId: Long): List<String> = dao.getTagsForImage(mediaId)
    override suspend fun getMediaIds(tag: String): List<Long> = dao.getImageIds(tag)
    override  suspend fun getMediaIds(tag: String, limit: Int, offset: Int): List<Long> = dao.getImageIds(tag, limit, offset)
    override  suspend fun upsertTagCrossRefs(crossRefs: List<MediaTagCrossRef>) = dao.upsert(crossRefs.map{it.toImageCrossRef()})
    override suspend fun deleteByIds(ids: List<Long>) = dao.deleteByIds(ids)
    override suspend fun deleteByTags(tags: List<String>) = dao.deleteByTags(tags)
    override suspend fun clear() = dao.clear()
    override suspend fun count(tag: String) = dao.count(tag)
    override fun getTagCounts(): Flow<Map<String, Int>> = dao.getTagCounts().map{ list -> list.associate { it.tag to it.count } }
}