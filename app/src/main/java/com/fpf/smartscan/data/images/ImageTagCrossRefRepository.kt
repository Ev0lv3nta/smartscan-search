package com.fpf.smartscan.data.images

import androidx.room.Transaction

class ImageTagCrossRefRepository(
    private val dao: ImageTagCrossRefDao,
    private val imageTagDao: ImageTagDao
) {
    suspend fun getImageIds(tag: String): List<Long> = dao.getImageIds(tag)
    suspend fun getImageIds(tag: String, limit: Int): List<Long> = dao.getImageIds(tag, limit)

    @Transaction
    suspend fun addTags(imageTagCrossRefs: List<ImageTagCrossRef>) {
        val uniqueTagNames = imageTagCrossRefs.map { it.tag }.toSet()
        for (name in uniqueTagNames){
            val existingTag = imageTagDao.get(name)
            if(existingTag == null){
                imageTagDao.insert(ImageTag(name=name))
            }
        }
        dao.upsert(imageTagCrossRefs)
    }

    suspend fun deleteByIds(ids: List<Long>) = dao.deleteByIds(ids)

    suspend fun deleteByTags(tags: List<String>) = dao.deleteByTags(tags)

    suspend fun clear() = dao.clear()

    suspend fun count(tag: String): Int = dao.count(tag)

}
