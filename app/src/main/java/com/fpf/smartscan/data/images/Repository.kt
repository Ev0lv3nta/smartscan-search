package com.fpf.smartscan.data.images

class ImageTagRepository(private val dao: ImageTagDao) {
    suspend fun getTags(): List<String> {
        return dao.getTags()
    }

    suspend fun getImageIds(tag: String): List<Long> {
        return dao.getImageIds(tag)
    }

    suspend fun addTags(tags: List<ImageTag>) {
        dao.add(tags)
    }

    suspend fun deleteByImageIds(ids: List<Long>) {
        dao.delete(ids)
    }

    suspend fun deleteByTags(tags: List<String>) {
        dao.delete(tags)
    }

    suspend fun clear() {
        dao.clear()
    }

}
