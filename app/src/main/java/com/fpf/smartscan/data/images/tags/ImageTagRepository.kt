package com.fpf.smartscan.data.images.tags

import com.fpf.smartscan.collections.MediaTag
import com.fpf.smartscan.data.MediaTagRepository
import kotlinx.coroutines.flow.Flow

class ImageTagRepository(private val dao: ImageTagDao): MediaTagRepository {
    override val allTags: Flow<List<MediaTag>> = dao.getAllFlow()

    override suspend fun getAllTags(): List<MediaTag> = dao.getAll()

    override suspend fun getTag(name: String): MediaTag? = dao.get(name)

    override suspend fun insertTags(mediaTags: List<MediaTag>) = dao.insert(mediaTags.map{it.toImageMediaTag()})

    override suspend fun updateTags(mediaTags: List<MediaTag>) = dao.update(mediaTags.map{it.toImageMediaTag()})

    override suspend fun deleteTag(mediaTag: MediaTag) = dao.delete(mediaTag.toImageMediaTag())

    override suspend fun clear() = dao.clear()
}