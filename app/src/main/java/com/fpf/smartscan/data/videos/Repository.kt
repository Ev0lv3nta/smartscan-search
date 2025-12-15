package com.fpf.smartscan.data.videos

class VideoTagRepository(private val dao: VideoTagDao) {
    val allTags = dao.getTagsFlow()

    suspend fun getTags(): List<String> {
        return dao.getTags()
    }

    suspend fun getVideoIds(tag: String): List<Long> {
        return dao.getVideoIds(tag)
    }

    suspend fun addTags(tags: List<VideoTag>) {
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
