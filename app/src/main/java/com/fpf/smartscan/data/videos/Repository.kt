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

    suspend fun addTag(id: Long, tag: String ){
        val results = getMetadata(listOf(id))
        if (results.isEmpty()) return

        val currentMetadata = results[0]
        insertMetadata(currentMetadata.copy(tags = currentMetadata.tags + tag))
    }

    suspend fun deleteMetadata(ids: List<Long>) {
        dao.delete(ids)
    }

    suspend fun deleteAllMetadata() {
        dao.deleteAll()
    }
}