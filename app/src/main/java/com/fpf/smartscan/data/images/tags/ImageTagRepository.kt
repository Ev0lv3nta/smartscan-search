package com.fpf.smartscan.data.images.tags

import com.fpf.smartscan.data.MediaTagRepository
import kotlinx.coroutines.flow.Flow

class ImageTagRepository(private val dao: ImageTagDao): MediaTagRepository<ImageTag> {
    override val allTags: Flow<List<ImageTag>> = dao.getAllFlow()

    override suspend fun getAllTags(): List<ImageTag> = dao.getAll()

    override suspend fun getTagsByName(names: List<String>): List<ImageTag> = dao.getByNames(names)

    override suspend fun getTagsById(ids: List<Long>): List<ImageTag> = dao.getByIds(ids)

    override suspend fun insertTags(mediaTags: List<ImageTag>): List<Long> = dao.insert(mediaTags)

    override suspend fun updateTags(mediaTags: List<ImageTag>) = dao.update(mediaTags)

    override suspend fun deleteTags(mediaTags: List<ImageTag>) = dao.delete(mediaTags)

    override suspend fun deleteTagsByName(names: List<String>) = dao.deleteByNames(names)

    override suspend fun deleteTagsById(ids: List<Long>) = dao.deleteByIds(ids)

    override suspend fun clear() = dao.clear()
}