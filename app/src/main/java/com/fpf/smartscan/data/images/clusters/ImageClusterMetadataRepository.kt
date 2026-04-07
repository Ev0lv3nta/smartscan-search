package com.fpf.smartscan.data.images.clusters

class ImageClusterMetadataRepository(private val dao: ImageClusterMetadataDao) {
    val allMetadata = dao.getAllFlow()
    suspend fun getAllMetadata(): List<ImageClusterMetadata> = dao.getAll()

    suspend fun getMetadata(id: Long): ImageClusterMetadata? = dao.get(id)

    suspend fun upsertMetadatas(metadatas: List<ImageClusterMetadata>) = dao.upsert(metadatas)

    suspend fun deleteMetadata(metadata: ImageClusterMetadata) = dao.delete(metadata)
}