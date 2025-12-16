package com.fpf.smartscan.data.videos

import kotlinx.coroutines.flow.Flow

class VideoTagRepository(private val dao: VideoTagDao) {

    fun getAll(): Flow<List<VideoTag>> {
        return dao.getAll()
    }

    suspend fun getByName(name: String): VideoTag? {
        return dao.get(name)
    }

    suspend fun upsert(videoTag: VideoTag) {
        val existing = dao.get(videoTag.name)
        if (existing == null) {
            dao.insert(videoTag)
        }
        else {
            val updated = existing.copy(
                lastUsedAt = videoTag.lastUsedAt ?: existing.lastUsedAt,
                cohesionScore = videoTag.cohesionScore ?: existing.cohesionScore,
                nPrototype = if (videoTag.nPrototype != 1) videoTag.nPrototype else existing.nPrototype
            )
            dao.update(updated)
        }
    }

    suspend fun delete(videoTag: VideoTag) {
        dao.delete(videoTag)
    }
}
