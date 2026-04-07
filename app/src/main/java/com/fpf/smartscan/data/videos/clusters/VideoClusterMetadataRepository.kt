package com.fpf.smartscan.data.videos.clusters

class VideoClusterMetadataRepository(private val dao: VideoClusterMetadataDao) {
    val allMetadata = dao.getAllFlow()
    suspend fun getAllMetadata(): List<VideoClusterMetadata> = dao.getAll()

    suspend fun getMetadata(id: Long): VideoClusterMetadata? = dao.get(id)

    suspend fun upsertMetadatas(metadatas: List<VideoClusterMetadata>) = dao.upsert(metadatas)

    suspend fun deleteMetadata(metadata: VideoClusterMetadata) = dao.delete(metadata)
}