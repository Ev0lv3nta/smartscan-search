package com.fpf.smartscan.data.videos

import com.fpf.smartscan.data.MediaTagCrossRef
import com.fpf.smartscan.data.MediaTagCrossRefRepository

class VideoTagCrossRefRepository(
    private val dao: VideoTagCrossRefDao,
    private val videoTagDao: VideoTagDao
): MediaTagCrossRefRepository {
    override suspend fun getTagToMediaIdsMap(): Map<String, List<Long>> = dao.getAllCrossRefs().groupBy({it.tag}, {it.mediaId})
    override suspend fun getTagsForMedia(mediaId: Long): List<String> = dao.getTagsForVideo(mediaId)
    override suspend fun getMediaIds(tag: String): List<Long> = dao.getVideoIds(tag)
    override  suspend fun getMediaIds(tag: String, limit: Int, offset: Int): List<Long> = dao.getVideoIds(tag, limit, offset)

    override  suspend fun addTags(crossRefs: List<MediaTagCrossRef>) {
        val uniqueTagNames = crossRefs.map { it.tag }.toSet()
        for (name in uniqueTagNames){
            val existingTag = videoTagDao.get(name)
            if(existingTag == null){
                videoTagDao.insert(VideoTag(name=name))
            }
        }
        dao.upsert(crossRefs.map{it.toVideoCrossRef()})
    }

    override suspend fun deleteByIds(ids: List<Long>) = dao.deleteByIds(ids)

    override suspend fun deleteByTags(tags: List<String>) = dao.deleteByTags(tags)

    override suspend fun clear() = dao.clear()

    override suspend fun count(tag: String) = dao.count(tag)
}
