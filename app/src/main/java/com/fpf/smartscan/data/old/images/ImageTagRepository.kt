package com.fpf.smartscan.data.old.images

class ImageTagRepository(private val dao: ImageTagDao) {
    suspend fun getAllTags(): List<ImageTag> = dao.getAll()
}
