package com.fpf.smartscan.data.videos.tags

import com.fpf.smartscan.data.MediaTagCrossRefRepository
import com.fpf.smartscan.data.TagWithCount
import kotlinx.coroutines.flow.Flow

class VideoTagCrossRefRepository(private val dao: VideoTagCrossRefDao): MediaTagCrossRefRepository<VideoTagCrossRef> {
    override suspend fun getAllCrossRefs(): List<VideoTagCrossRef> = dao.getAllCrossRefs()
    override suspend fun getTagsForMedia(mediaId: Long): List<Long> = dao.getTagsForVideo(mediaId)
    override fun getMediaIdsFlow(tagId: Long): Flow<List<Long>> = dao.getVideoIdsFlow(tagId)
    override suspend fun getMediaIds(tagId: Long): List<Long> = dao.getVideoIds(tagId)
    override  suspend fun getMediaIds(tagId: Long, limit: Int, offset: Int): List<Long> = dao.getVideoIds(tagId, limit, offset)
    override suspend fun upsertTagCrossRefs(crossRefs: List<VideoTagCrossRef>) = dao.upsert(crossRefs)
    override  suspend fun upsertTagCrossRefs(tagId: Long, mediaIds: List<Long>) = dao.upsert(mediaIds.map{ VideoTagCrossRef(mediaId = it, tagId=tagId)})
    override suspend fun deleteByMediaIds(ids: List<Long>) = dao.deleteByMediaIds(ids)
    override suspend fun deleteByTagIds(ids: List<Long>) = dao.deleteByTagIds(ids)
    override suspend fun clear() = dao.clear()
    override suspend fun count(tagId: Long) = dao.count(tagId)
    override fun getTagsWithCounts(): Flow<List<TagWithCount>> = dao.getTagsWithCounts()
}