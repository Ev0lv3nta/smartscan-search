package com.fpf.smartscan.data.videos

import androidx.room.Transaction

class VideoTagCrossRefRepository(
    private val dao: VideoTagCrossRefDao,
    private val videoTagDao: VideoTagDao
) {
    suspend fun getVideoIds(tag: String): List<Long> {
        return dao.getVideoIds(tag)
    }
    @Transaction
    suspend fun addTags(imageTagCrossRefs: List<VideoTagCrossRef>) {
        val uniqueTagNames = imageTagCrossRefs.map { it.tag }.toSet()
        for (name in uniqueTagNames){
            val existingTag = videoTagDao.get(name)
            if(existingTag == null){
                videoTagDao.upsert(VideoTag(name=name))
            }
        }
        dao.upsert(imageTagCrossRefs)
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
