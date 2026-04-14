package com.fpf.smartscan.data.old.images

class ImageTagCrossRefRepository(
    private val dao: ImageTagCrossRefDao,
) {
    suspend fun getAllCrossRefs(): List<ImageTagCrossRef> = dao.getAll()
}
