package com.fpf.smartscan.data.videos.tags

import com.fpf.smartscan.model.MediaTag
import com.fpf.smartscan.data.MediaTagRepository
import kotlinx.coroutines.flow.Flow

class VideoTagRepository(private val dao: VideoTagDao): MediaTagRepository {
    override val allTags: Flow<List<MediaTag>> = dao.getAllFlow()
    override suspend fun getAllTags(): List<MediaTag> = dao.getAll()
    override suspend fun getTag(name: String): MediaTag? = dao.get(name)
    override suspend fun insertTags(mediaTags: List<MediaTag>): List<Long> = dao.insert(mediaTags.map{it.toVideoMediaTag()})

    override suspend fun updateTags(mediaTags: List<MediaTag>) = dao.update(mediaTags.map{it.toVideoMediaTag()})
    override suspend fun deleteTag(mediaTag: MediaTag) = dao.delete(mediaTag.toVideoMediaTag())
    override suspend fun deleteTagByName(name: String) = dao.deleteByName(name)

    override suspend fun clear() = dao.clear()

}