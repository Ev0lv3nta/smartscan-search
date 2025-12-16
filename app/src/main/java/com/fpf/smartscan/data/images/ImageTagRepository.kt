package com.fpf.smartscan.data.images

import kotlinx.coroutines.flow.Flow

class ImageTagRepository(private val dao: ImageTagDao) {
    fun getAll(): Flow<List<ImageTag>> {
        return dao.getAll()
    }

    suspend fun getByName(name: String): ImageTag? {
        return dao.get(name)
    }

    suspend fun insert(imageTag: ImageTag) {
        dao.insert(imageTag)
    }

    suspend fun upsert(imageTag: ImageTag) {
        val existing = dao.get(imageTag.name)
        if (existing == null) {
            dao.insert(imageTag)
        }
        else {
            val updated = existing.copy(
                lastUsedAt = imageTag.lastUsedAt ?: existing.lastUsedAt,
                cohesionScore = imageTag.cohesionScore ?: existing.cohesionScore,
                nPrototype = if (imageTag.nPrototype != 1) imageTag.nPrototype else existing.nPrototype
            )
            dao.update(updated)
        }
    }

    suspend fun delete(imageTag: ImageTag) {
        dao.delete(imageTag)
    }
}
