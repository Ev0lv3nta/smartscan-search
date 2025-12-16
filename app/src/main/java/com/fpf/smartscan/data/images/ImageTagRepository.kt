package com.fpf.smartscan.data.images

import kotlinx.coroutines.flow.Flow

class ImageTagRepository(private val dao: ImageTagDao) {
    fun getAll(): Flow<List<ImageTag>> {
        return dao.getAll()
    }

    suspend fun getByName(name: String): ImageTag? {
        return dao.get(name)
    }

    suspend fun upsert(imageTag: ImageTag) {
        dao.upsert(imageTag)
    }

    suspend fun delete(imageTag: ImageTag) {
        dao.delete(imageTag)
    }
}
