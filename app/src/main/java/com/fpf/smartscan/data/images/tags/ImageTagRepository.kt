package com.fpf.smartscan.data.images.tags

import com.fpf.smartscan.data.MediaTagRepository
import kotlinx.coroutines.flow.Flow

@Suppress("UNCHECKED_CAST")
class ImageTagRepository(private val dao: ImageTagDao): MediaTagRepository<ImageTag> {
    override val allTags: Flow<List<ImageTag>> = dao.getAllFlow()

    override suspend fun getAllTags(): List<ImageTag> = dao.getAll()

    override suspend fun getTag(name: String): ImageTag? = dao.get(name)

    override suspend fun insertTags(mediaTags: List<ImageTag>): List<Long> = dao.insert(mediaTags)

    override suspend fun updateTags(mediaTags: List<ImageTag>) = dao.update(mediaTags)

    override suspend fun deleteTag(mediaTag: ImageTag) = dao.delete(mediaTag)

    override suspend fun deleteTagByName(name: String) = dao.deleteByName(name)

    override suspend fun clear() = dao.clear()
}