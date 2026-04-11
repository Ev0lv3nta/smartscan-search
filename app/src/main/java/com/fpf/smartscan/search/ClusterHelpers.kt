package com.fpf.smartscan.search


import com.fpf.smartscan.data.MediaClusterMetadata
import com.fpf.smartscan.data.MediaClusterMetadataRepository
import com.fpf.smartscan.data.images.clusters.ImageClusterCrossRef
import com.fpf.smartscan.data.images.clusters.ImageClusterCrossRefRepository
import com.fpf.smartscan.data.images.clusters.ImageClusterMetadata
import com.fpf.smartscan.data.images.clusters.ImageClusterMetadataRepository
import com.fpf.smartscan.data.videos.clusters.VideoClusterCrossRef
import com.fpf.smartscan.data.videos.clusters.VideoClusterCrossRefRepository
import com.fpf.smartscan.data.videos.clusters.VideoClusterMetadata
import com.fpf.smartscan.data.videos.clusters.VideoClusterMetadataRepository
import com.fpf.smartscansdk.core.cluster.Cluster
import com.fpf.smartscansdk.core.cluster.ClusterResult
import com.fpf.smartscansdk.core.cluster.IncrementalClusterer
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import com.fpf.smartscansdk.core.embeddings.StoredEmbedding
import kotlin.collections.filterNot
import kotlin.collections.mapNotNull

suspend fun clusterImages(crossRefRepository: ImageClusterCrossRefRepository, clusterStore: FileEmbeddingStore, itemStore: FileEmbeddingStore, clusterMetadataRepository: ImageClusterMetadataRepository){
    val assignedIds = crossRefRepository.getAllMedia()
    val existingClusters: Map<Long, Cluster> = getExistingClusters(clusterStore, clusterMetadataRepository)

    var itemEmbeds = if(itemStore.exists) itemStore.get() else emptyList()
    itemEmbeds = itemEmbeds.filterNot {it.id in assignedIds}

    val clusterer = IncrementalClusterer(existingClusters = existingClusters, defaultThreshold = 0.4f)
    val result = clusterer.cluster(itemEmbeds)

    // Must update clusters before updating assignments to prevent foreign key related errors
    updateImageClusters(result, clusterMetadataRepository, clusterStore)
    updateImageAssignments(result, crossRefRepository)
}

suspend fun clusterVideos(crossRefRepository: VideoClusterCrossRefRepository, clusterStore: FileEmbeddingStore, itemStore: FileEmbeddingStore, clusterMetadataRepository: VideoClusterMetadataRepository){
    val assignedIds = crossRefRepository.getAllMedia()
    val existingClusters: Map<Long, Cluster> = getExistingClusters(clusterStore, clusterMetadataRepository)

    var itemEmbeds = if(itemStore.exists) itemStore.get() else emptyList()
    itemEmbeds = itemEmbeds.filterNot {it.id in assignedIds}

    val clusterer = IncrementalClusterer(existingClusters = existingClusters, defaultThreshold = 0.4f)
    val result = clusterer.cluster(itemEmbeds)

    // Must update clusters before updating assignments to prevent foreign key related errors
    updateVideoClusters(result, clusterMetadataRepository, clusterStore)
    updateVideoAssignments(result, crossRefRepository)
}

private suspend fun <T: MediaClusterMetadata>getExistingClusters(store: FileEmbeddingStore, clusterMetadataRepository: MediaClusterMetadataRepository<T>): Map<Long, Cluster>{
    return  if (store.exists) {
        val metadataMap = clusterMetadataRepository.getAllMetadata()
        store.get().mapNotNull { cluster -> metadataMap[cluster.id]?.let { meta ->
            cluster.id to Cluster(cluster.id, cluster.embedding, meta)
        } }.toMap()
    } else emptyMap()
}

suspend fun updateImageClusters(clusterResult: ClusterResult, clusterMetadataRepository: ImageClusterMetadataRepository, store: FileEmbeddingStore){
    val clusterMetadatas = clusterResult.clusters.values.map{ ImageClusterMetadata(
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

suspend fun updateVideoClusters(clusterResult: ClusterResult, clusterMetadataRepository: VideoClusterMetadataRepository, store: FileEmbeddingStore){
    val clusterMetadatas = clusterResult.clusters.values.map{ VideoClusterMetadata(
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

suspend fun updateImageAssignments(clusterResult: ClusterResult, crossRefRepository: ImageClusterCrossRefRepository){
    val crossRefs = clusterResult.assignments.map { ImageClusterCrossRef(clusterId = it.value, mediaId = it.key) }
    crossRefRepository.addMedia(crossRefs)
}

suspend fun updateVideoAssignments(clusterResult: ClusterResult, crossRefRepository: VideoClusterCrossRefRepository){
    val crossRefs = clusterResult.assignments.map { VideoClusterCrossRef(clusterId = it.value, mediaId = it.key) }
    crossRefRepository.addMedia(crossRefs)
}
