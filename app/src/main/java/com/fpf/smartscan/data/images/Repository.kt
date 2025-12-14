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

    suspend fun deleteByIds(ids: List<Long>) {
        dao.deleteByIds(ids)
    }

    suspend fun deleteByTags(tags: List<String>) {
        dao.deleteByTags(tags)
    }

    suspend fun clear() {
        dao.clear()
    }

}
