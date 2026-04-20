package com.fpf.smartscan.data.old.videos

class VideoTagRepository(private val dao: VideoTagDao) {
    suspend fun getAllTags(): List<VideoTag> = dao.getAll()
}
