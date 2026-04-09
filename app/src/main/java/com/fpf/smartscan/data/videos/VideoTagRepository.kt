package com.fpf.smartscan.data.videos

class VideoTagRepository(private val dao: VideoTagDao) {
    val allTags = dao.getAllFlow()
    fun getAll(): List<VideoTag> = dao.getAll()

    suspend fun getByName(name: String): VideoTag? = dao.get(name)

    suspend fun upsert(videoTag: VideoTag) {
        val existing = dao.get(videoTag.name)
        if (existing == null) {
            dao.insert(videoTag)
        }
        else {
            val updated = existing.copy(
                lastUsedAt = videoTag.lastUsedAt ?: existing.lastUsedAt,
            )
            dao.update(updated)
        }
    }

    suspend fun delete(videoTag: VideoTag) = dao.delete(videoTag)
}
