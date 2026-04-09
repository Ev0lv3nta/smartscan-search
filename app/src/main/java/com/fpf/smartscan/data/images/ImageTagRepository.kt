package com.fpf.smartscan.data.images

import com.fpf.smartscan.data.MediaTag
import com.fpf.smartscan.data.MediaTagRepository
import kotlinx.coroutines.flow.Flow

class ImageTagRepository(private val dao: ImageTagDao): MediaTagRepository {
    override val allTags: Flow<List<MediaTag>> = dao.getAllFlow()

    override suspend fun getAllTags(): List<MediaTag> = dao.getAll()

    override suspend fun getTag(name: String): MediaTag? = dao.get(name)

    override suspend fun insertTag(mediaTag: MediaTag) = dao.insert(mediaTag.toImageMediaTag())

    override suspend fun upsertTag(mediaTag: MediaTag) {
        val existing = dao.get(mediaTag.name)
        if (existing == null) {
            insertTag(mediaTag)
        }
        else {
            val updated = existing.copy(
                lastUsedAt = mediaTag.lastUsedAt ?: existing.lastUsedAt,
            )
            dao.update(updated)
        }
    }

    override suspend fun deleteTag(mediaTag: MediaTag) = dao.delete(mediaTag.toImageMediaTag())
}
