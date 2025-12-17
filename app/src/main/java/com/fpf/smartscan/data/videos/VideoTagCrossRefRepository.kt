package com.fpf.smartscan.data.videos

import androidx.room.Transaction
import com.fpf.smartscan.utils.stringToLong

class VideoTagCrossRefRepository(
    private val dao: VideoTagCrossRefDao,
    private val videoTagDao: VideoTagDao
) {
    suspend fun getVideoIds(tag: String): List<Long> = dao.getVideoIds(tag)
    suspend fun getVideoIds(tag: String, limit: Int, offset: Int = 0): List<Long> = dao.getVideoIds(tag, limit, offset)

    @Transaction
    suspend fun addTags(imageTagCrossRefs: List<VideoTagCrossRef>) {
        val uniqueTagNames = imageTagCrossRefs.map { it.tag }.toSet()
        for (name in uniqueTagNames){
            val existingTag = videoTagDao.get(name)
            if(existingTag == null){
                videoTagDao.insert(VideoTag(name=name, prototypeId = stringToLong(name)))
            }
        }
        dao.upsert(imageTagCrossRefs)
    }

    suspend fun deleteByIds(ids: List<Long>) = dao.deleteByIds(ids)

    suspend fun deleteByTags(tags: List<String>) = dao.deleteByTags(tags)

    suspend fun clear() = dao.clear()

    suspend fun count(tag: String) = dao.count(tag)
}
