package com.fpf.smartscan.data.videos

import com.fpf.smartscan.data.MediaTag
import com.fpf.smartscan.data.MediaTagRepository
import kotlinx.coroutines.flow.Flow

class VideoTagRepository(private val dao: VideoTagDao): MediaTagRepository {
    override val allTags: Flow<List<MediaTag>> = dao.getAllFlow()

    override suspend fun getAllTags(): List<MediaTag> = dao.getAll()

    override suspend fun getTag(name: String): MediaTag? = dao.get(name)

    override suspend fun insertTag(mediaTag: MediaTag) = dao.insert(mediaTag.toVideoMediaTag())

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

    override suspend fun deleteTag(mediaTag: MediaTag) = dao.delete(mediaTag.toVideoMediaTag())
}
