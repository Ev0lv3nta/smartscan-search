package com.fpf.smartscan.data.videos.tags

import com.fpf.smartscan.data.MediaTagRepository
import kotlinx.coroutines.flow.Flow

class VideoTagRepository(private val dao: VideoTagDao): MediaTagRepository<VideoTag> {
    override val allTags: Flow<List<VideoTag>> = dao.getAllFlow()
    override suspend fun getAllTags(): List<VideoTag> = dao.getAll()
    override suspend fun getTagsByName(names: List<String>): List<VideoTag> = dao.getByNames(names)
    override suspend fun getTagsById(ids: List<Long>): List<VideoTag> = dao.getByIds(ids)
    override suspend fun insertTags(mediaTags: List<VideoTag>): List<Long> = dao.insert(mediaTags)
    override suspend fun updateTags(mediaTags: List<VideoTag>) = dao.update(mediaTags)
    override suspend fun deleteTags(mediaTags: List<VideoTag>) = dao.delete(mediaTags)
    override suspend fun deleteTagsByName(names: List<String>) = dao.deleteByNames(names)
    override suspend fun deleteTagsById(ids: List<Long>) = dao.deleteByIds(ids)
    override suspend fun clear() = dao.clear()
}