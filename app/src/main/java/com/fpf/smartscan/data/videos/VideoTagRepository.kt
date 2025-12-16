package com.fpf.smartscan.data.videos

import kotlinx.coroutines.flow.Flow

class VideoTagRepository(private val dao: VideoTagDao) {

    fun getAll(): Flow<List<VideoTag>> {
        return dao.getAll()
    }

    suspend fun getByName(name: String): VideoTag? {
        return dao.get(name)
    }

    suspend fun upsert(videoTag: VideoTag) {
        dao.upsert(videoTag)
    }

    suspend fun delete(videoTag: VideoTag) {
        dao.delete(videoTag)
    }
}
