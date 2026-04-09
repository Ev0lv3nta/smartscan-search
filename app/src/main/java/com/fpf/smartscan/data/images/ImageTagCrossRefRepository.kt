package com.fpf.smartscan.data.images

import com.fpf.smartscan.data.MediaTagCrossRef
import com.fpf.smartscan.data.MediaTagCrossRefRepository

class ImageTagCrossRefRepository(
    private val dao: ImageTagCrossRefDao,
    private val imageTagDao: ImageTagDao
): MediaTagCrossRefRepository {
    override suspend fun getTagToMediaIdsMap(): Map<String, List<Long>> = dao.getAllCrossRefs().groupBy({it.tag}, {it.mediaId})
    override suspend fun getTagsForMedia(mediaId: Long): List<String> = dao.getTagsForImage(mediaId)
    override suspend fun getMediaIds(tag: String): List<Long> = dao.getImageIds(tag)
    override  suspend fun getMediaIds(tag: String, limit: Int, offset: Int): List<Long> = dao.getImageIds(tag, limit, offset)

    override  suspend fun addTags(crossRefs: List<MediaTagCrossRef>) {
        val uniqueTagNames = crossRefs.map { it.tag }.toSet()
        for (name in uniqueTagNames){
            val existingTag = imageTagDao.get(name)
            if(existingTag == null){
                imageTagDao.insert(ImageTag(name=name))
            }
        }
        dao.upsert(crossRefs.map{it.toImageCrossRef()})
    }

    override suspend fun deleteByIds(ids: List<Long>) = dao.deleteByIds(ids)

    override suspend fun deleteByTags(tags: List<String>) = dao.deleteByTags(tags)

    override suspend fun clear() = dao.clear()

    override suspend fun count(tag: String) = dao.count(tag)
}
