package com.fpf.smartscan.data.images

class ImageMetadataRepository(private val dao: ImageMetadataDao) {

    suspend fun getAllMetadata(): List<ImageMetadata> {
        return dao.getAll()
    }

    suspend fun getMetadata(ids: List<Long>): List<ImageMetadata> {
        return dao.get(ids)
    }

    suspend fun insertMetadata(metadata: ImageMetadata) {
        dao.insert(metadata)
    }

    suspend fun deleteMetadata(id: Long) {
        dao.delete(id)
    }

    suspend fun deleteMetadata(ids: List<Long>) {
        dao.delete(ids)
    }

    suspend fun deleteAllMetadata() {
        dao.deleteAll()
    }
}
