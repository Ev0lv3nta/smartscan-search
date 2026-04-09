package com.fpf.smartscan.data.images

class ImageTagRepository(private val dao: ImageTagDao) {
    val allTags = dao.getAllFlow()
    suspend fun getAll(): List<ImageTag> = dao.getAll()

    suspend fun getByName(name: String): ImageTag? = dao.get(name)

    suspend fun insert(imageTag: ImageTag) = dao.insert(imageTag)

    suspend fun upsert(imageTag: ImageTag) {
        val existing = dao.get(imageTag.name)
        if (existing == null) {
            dao.insert(imageTag)
        }
        else {
            val updated = existing.copy(
                lastUsedAt = imageTag.lastUsedAt ?: existing.lastUsedAt,
            )
            dao.update(updated)
        }
    }

    suspend fun delete(imageTag: ImageTag) = dao.delete(imageTag)
}
