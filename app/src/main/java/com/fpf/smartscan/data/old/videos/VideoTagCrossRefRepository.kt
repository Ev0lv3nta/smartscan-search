package com.fpf.smartscan.data.old.videos

class VideoTagCrossRefRepository(
    private val dao: VideoTagCrossRefDao,
) {
    suspend fun getAllCrossRefs(): List<VideoTagCrossRef> = dao.getAll()
}
