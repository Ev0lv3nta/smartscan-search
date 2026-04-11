package com.fpf.smartscan.data.videos.tags

import com.fpf.smartscan.data.MediaTag
import com.fpf.smartscan.data.MediaTagRepository
import kotlinx.coroutines.flow.Flow

class VideoTagRepository(private val dao: VideoTagDao): MediaTagRepository<VideoTag> {
    override val allTags: Flow<List<VideoTag>> = dao.getAllFlow()
    override suspend fun getAllTags(): List<VideoTag> = dao.getAll()
    override suspend fun getTag(name: String): VideoTag? = dao.get(name)
    override suspend fun insertTags(mediaTags: List<VideoTag>): List<Long> = dao.insert(mediaTags)

    override suspend fun updateTags(mediaTags: List<VideoTag>) = dao.update(mediaTags)
    override suspend fun deleteTag(mediaTag: VideoTag) = dao.delete(mediaTag)
    override suspend fun deleteTagByName(name: String) = dao.deleteByName(name)

    override suspend fun clear() = dao.clear()

}