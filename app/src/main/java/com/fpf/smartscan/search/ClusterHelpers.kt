package com.fpf.smartscan.search


import android.util.Log
import com.fpf.smartscan.data.clusters.ClusterCrossRef
import com.fpf.smartscan.data.clusters.ClusterCrossRefRepository
import com.fpf.smartscan.data.clusters.MediaClusterMetadata
import com.fpf.smartscan.data.clusters.ClusterMetadataRepository
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscansdk.core.cluster.Cluster
import com.fpf.smartscansdk.core.cluster.ClusterResult
import com.fpf.smartscansdk.core.cluster.IncrementalClusterer
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import com.fpf.smartscansdk.core.embeddings.StoredEmbedding
import kotlin.collections.filterNot
import kotlin.collections.mapNotNull

suspend fun clusterMedia(crossRefRepository: ClusterCrossRefRepository, clusterStore: FileEmbeddingStore, itemStore: FileEmbeddingStore, clusterMetadataRepository: ClusterMetadataRepository, mediaType: MediaType){
    val assignedIds = crossRefRepository.getAssignments()
    val existingClusters: Map<Long, Cluster> = getExistingClusters(clusterStore, clusterMetadataRepository)

    var itemEmbeds = if(itemStore.exists) itemStore.get() else emptyList()
    itemEmbeds = itemEmbeds.filterNot {it.id in assignedIds}

    val clusterer = IncrementalClusterer(existingClusters = existingClusters, defaultThreshold = 0.4f)
    val result = clusterer.cluster(itemEmbeds)

//    Log.d("clusterMedia", "N clusters: ${result.clusters.size} | N: ${result.assignments.size}" )

    // Must update clusters before updating assignments to prevent foreign key related errors
    updateClusters(result, clusterMetadataRepository, existingClusters, clusterStore, mediaType)
    updateAssignments(result, crossRefRepository, mediaType)
}

private suspend fun getExistingClusters(store: FileEmbeddingStore, clusterMetadataRepository: ClusterMetadataRepository): Map<Long, Cluster>{
    return  if (store.exists) {
        val metadataMap = clusterMetadataRepository.getAllMetadataAsMap()
        store.get().mapNotNull { cluster -> metadataMap[cluster.id]?.let { meta ->
            cluster.id to Cluster(cluster.id, cluster.embedding, meta)
        } }.toMap()
    } else emptyMap()
}

private suspend fun updateClusters(clusterResult: ClusterResult, clusterMetadataRepository: ClusterMetadataRepository, existingClustersMap: Map<Long, Cluster>, store: FileEmbeddingStore, mediaType: MediaType){
    val (existingClusters, newClusters) = clusterResult.clusters.values.partition { it.prototypeId in existingClustersMap }
    val existingMetadata = existingClusters.map {
        MediaClusterMetadata(
            clusterId = it.prototypeId,
            prototypeSize = it.metadata.prototypeSize,
            meanSimilarity = it.metadata.meanSimilarity,
            stdSimilarity = it.metadata.stdSimilarity,
            label = it.metadata.label,
            type = mediaType
        )
    }

    val newMetadata = existingClusters.map {
        MediaClusterMetadata(
            clusterId = it.prototypeId,
            prototypeSize = it.metadata.prototypeSize,
            meanSimilarity = it.metadata.meanSimilarity,
            stdSimilarity = it.metadata.stdSimilarity,
            label = it.metadata.label,
            type = mediaType
        )
    }

    clusterMetadataRepository.updateMetadatas(existingMetadata)
    clusterMetadataRepository.insertMetadatas(newMetadata)

    val existingEmbeds = existingClusters.map { StoredEmbedding(id = it.prototypeId, embedding = it.embedding, date = System.currentTimeMillis()) }
    val newEmbeds = newClusters.map { StoredEmbedding(id = it.prototypeId, embedding = it.embedding, date = System.currentTimeMillis()) }

    store.add(newEmbeds)
    store.update(existingEmbeds)
}


private suspend fun updateAssignments(clusterResult: ClusterResult, crossRefRepository: ClusterCrossRefRepository, mediaType: MediaType){
    val crossRefs = clusterResult.assignments.map { ClusterCrossRef(clusterId = it.value, mediaId = it.key, type = mediaType) }
    crossRefRepository.upsertClusterCrossRefs(crossRefs)
}
