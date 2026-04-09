package com.fpf.smartscan.data.videos.clusters

import com.fpf.smartscan.data.MediaClusterMetadata
import com.fpf.smartscan.data.MediaClusterMetadataRepository
import com.fpf.smartscansdk.core.cluster.ClusterMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class VideoClusterMetadataRepository(private val dao: VideoClusterMetadataDao): MediaClusterMetadataRepository {
    override val allMetadata: Flow<Map<Long, ClusterMetadata>> = dao.getAllFlow().map { list ->
        list.associate { it.clusterId to it.toMetadata() }
    }
    override val allLabels: Flow<List<String>> = dao.getLabels()

    override suspend fun getAllMetadata(): Map<Long, ClusterMetadata> = dao.getAll().associate {
        it.clusterId to it.toMetadata()
    }

    override suspend fun getMetadata(id: Long): ClusterMetadata? = dao.get(id)?.toMetadata()

    override suspend fun getIdFromLabel(label: String): Long? = dao.getIdFromLabel(label)

    override suspend fun upsertMetadatas(metadatas: List<MediaClusterMetadata>) = dao.upsert(metadatas.map{it.toVideoClusterMetadata()})

    override suspend fun deleteMetadata(id: Long) = dao.delete(id)
}