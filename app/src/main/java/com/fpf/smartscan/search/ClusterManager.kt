package com.fpf.smartscan.search

import com.fpf.smartscan.data.MediaClusterCrossRef
import com.fpf.smartscan.data.MediaClusterCrossRefRepository
import com.fpf.smartscan.data.MediaClusterMetadata
import com.fpf.smartscan.data.MediaClusterMetadataRepository
import com.fpf.smartscansdk.core.cluster.Cluster
import com.fpf.smartscansdk.core.cluster.ClusterResult
import com.fpf.smartscansdk.core.cluster.IncrementalClusterer
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import com.fpf.smartscansdk.core.embeddings.StoredEmbedding
import kotlin.collections.filterNot
import kotlin.collections.mapNotNull

object ClusterManager {
    suspend fun clusterMedia(crossRefRepository: MediaClusterCrossRefRepository, clusterStore: FileEmbeddingStore, itemStore: FileEmbeddingStore, clusterMetadataRepository: MediaClusterMetadataRepository){
        val assignedIds = crossRefRepository.getAllMedia()
        val existingClusters: Map<Long, Cluster> = getExistingClusters(clusterStore, clusterMetadataRepository)

        var itemEmbeds = if(itemStore.exists) itemStore.get() else emptyList()
        itemEmbeds = itemEmbeds.filterNot {it.id in assignedIds}

        val clusterer = IncrementalClusterer(existingClusters = existingClusters, defaultThreshold = 0.4f)
        val result = clusterer.cluster(itemEmbeds)

        // Must update clusters before updating assignments to prevent foreign key related errors
        updateClusters(result, clusterMetadataRepository, clusterStore)
        updateAssignments(result, crossRefRepository)
    }

    private suspend fun getExistingClusters(store: FileEmbeddingStore, clusterMetadataRepository: MediaClusterMetadataRepository): Map<Long, Cluster>{
        return  if (store.exists) {
            val metadataMap = clusterMetadataRepository.getAllMetadata()
            store.get().mapNotNull { cluster -> metadataMap[cluster.id]?.let { meta ->
                cluster.id to Cluster(cluster.id, cluster.embedding, meta)
            } }.toMap()
        } else emptyMap()
    }

    private suspend fun updateClusters(clusterResult: ClusterResult, clusterMetadataRepository: MediaClusterMetadataRepository, store: FileEmbeddingStore){
        val clusterMetadatas = clusterResult.clusters.values.map{ MediaClusterMetadata(
            clusterId = it.prototypeId,
            prototypeSize = it.metadata.prototypeSize,
            meanSimilarity = it.metadata.meanSimilarity,
            stdSimilarity = it.metadata.stdSimilarity,
            label = it.metadata.label
        ) }
        clusterMetadataRepository.upsertMetadatas(clusterMetadatas)

        val clusterEmbeddings = clusterResult.clusters.values.map { StoredEmbedding(id = it.prototypeId, embedding = it.embedding, date = System.currentTimeMillis()) }
        store.add(clusterEmbeddings)
    }

    private suspend fun updateAssignments(clusterResult: ClusterResult, crossRefRepository: MediaClusterCrossRefRepository){
        val crossRefs = clusterResult.assignments.map { MediaClusterCrossRef(clusterId = it.value, mediaId = it.key) }
        crossRefRepository.addMedia(crossRefs)
    }
}