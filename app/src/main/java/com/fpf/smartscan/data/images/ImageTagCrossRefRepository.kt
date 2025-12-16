package com.fpf.smartscan.data.images

import androidx.room.Transaction

class ImageTagCrossRefRepository(
    private val dao: ImageTagCrossRefDao,
    private val imageTagDao: ImageTagDao
) {
    suspend fun getImageIds(tag: String): List<Long> {
        return dao.getImageIds(tag)
    }
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
