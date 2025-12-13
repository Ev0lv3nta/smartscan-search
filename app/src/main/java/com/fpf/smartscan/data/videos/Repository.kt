package com.fpf.smartscan.data.videos

class VideoMetadataRepository(private val dao: VideoMetadataDao) {

    suspend fun getAllMetadata(): List<VideoMetadata> {
        return dao.getAll()
    }

    suspend fun getMetadata(ids: List<Long>): List<VideoMetadata> {
        return dao.get(ids)
    }

    suspend fun insertMetadata(metadata: VideoMetadata) {
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