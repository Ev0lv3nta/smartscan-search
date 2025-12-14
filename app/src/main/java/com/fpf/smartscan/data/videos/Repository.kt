package com.fpf.smartscan.data.videos

class VideoTagRepository(private val dao: VideoTagDao) {

    suspend fun getTags(): List<String> {
        return dao.getTags()
    }

    suspend fun getVideoIds(tag: String): List<Long> {
        return dao.getVideoIds(tag)
    }

    suspend fun addTags(tags: List<VideoTag>) {
        dao.add(tags)
    }

    suspend fun deleteByVideoIds(ids: List<Long>) {
        dao.delete(ids)
    }

    suspend fun deleteByTags(tags: List<String>) {
        dao.delete(tags)
    }

    suspend fun clear() {
        dao.clear()
    }
}
