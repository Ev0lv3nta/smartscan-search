package com.fpf.smartscan.data.videos.tags

import com.fpf.smartscan.data.MediaTagCrossRef
import com.fpf.smartscan.data.MediaTagCrossRefRepository
import com.fpf.smartscan.model.MediaTag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class VideoTagCrossRefRepository(private val dao: VideoTagCrossRefDao): MediaTagCrossRefRepository {
    override suspend fun getAllCrossRefs(): List<MediaTagCrossRef> = dao.getAllCrossRefs()
    override suspend fun getTagsForMedia(mediaId: Long): List<Long> = dao.getTagsForVideo(mediaId)
    override suspend fun getMediaIds(tagId: Long): List<Long> = dao.getVideoIds(tagId)
    override  suspend fun getMediaIds(tagId: Long, limit: Int, offset: Int): List<Long> = dao.getVideoIds(tagId, limit, offset)
    override suspend fun upsertTagCrossRefs(crossRefs: List<MediaTagCrossRef>) = dao.upsert(crossRefs.map{it.toVideoCrossRef()})
    override suspend fun deleteByMediaIds(ids: List<Long>) = dao.deleteByMediaIds(ids)
    override suspend fun deleteByTagIds(ids: List<Long>) = dao.deleteByTagIds(ids)
    override suspend fun clear() = dao.clear()
    override suspend fun count(tagId: Long) = dao.count(tagId)
    override fun getTagCounts(): Flow<Map<MediaTag, Int>> = dao.getTagCounts().map{ list -> list.associate { it.toMediaCount() to it.count } }
}