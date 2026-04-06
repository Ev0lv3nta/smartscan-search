package com.fpf.smartscan.data.videos.clusters

class VideoClusterMetadataRepository(private val dao: VideoClusterMetadataDao) {
    val allMetadata = dao.getAllFlow()
    suspend fun getAllMetadata(): List<VideoClusterMetadata> = dao.getAll()

    suspend fun getMetadata(id: Long): VideoClusterMetadata? = dao.get(id)

    suspend fun insertMetadata(metadata: VideoClusterMetadata) = dao.insert(metadata)

    suspend fun updateMetadata(metadata: VideoClusterMetadata) = dao.update(metadata)

    suspend fun deleteMetadata(metadata: VideoClusterMetadata) = dao.delete(metadata)
}