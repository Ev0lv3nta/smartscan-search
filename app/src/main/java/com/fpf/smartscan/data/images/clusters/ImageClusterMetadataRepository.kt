package com.fpf.smartscan.data.images.clusters

import com.fpf.smartscan.data.MediaClusterMetadata
import com.fpf.smartscan.data.MediaClusterMetadataRepository
import com.fpf.smartscansdk.core.cluster.ClusterMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.collections.associate

class ImageClusterMetadataRepository(private val dao: ImageClusterMetadataDao): MediaClusterMetadataRepository {
    override val allMetadata: Flow<List<MediaClusterMetadata>> = dao.getAllFlow()


    override val allLabels: Flow<List<String>> = dao.getLabels()

    override suspend fun getAllMetadata(): Map<Long, ClusterMetadata> = dao.getAll().associate {
            it.clusterId to it.toMetadata()
        }

    override suspend fun getMetadata(id: Long): ClusterMetadata? = dao.get(id)?.toMetadata()

    override suspend fun getIdFromLabel(label: String): Long? = dao.getIdFromLabel(label)

    override suspend fun upsertMetadatas(metadatas: List<MediaClusterMetadata>) = dao.upsert(metadatas.map{it.toImageClusterMetadata()})

    override suspend fun deleteMetadata(id: Long) = dao.delete(id)
}