package com.fpf.smartscan.cluster

import com.fpf.smartscan.data.clusters.ClusterCrossRef
import com.fpf.smartscan.data.clusters.ClusterCrossRefRepository
import com.fpf.smartscan.data.clusters.ClusterMetadataRepository
import com.fpf.smartscan.data.clusters.MediaClusterMetadata
import com.fpf.smartscan.data.metadata.MediaMetadataRepository
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.utils.reservoirSample
import com.fpf.smartscansdk.core.cluster.Cluster
import com.fpf.smartscansdk.core.cluster.ClusterResult
import com.fpf.smartscansdk.core.cluster.IncrementalClusterer
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import com.fpf.smartscansdk.core.embeddings.StoredEmbedding
import kotlin.collections.iterator

class ClusterManager(
    private val clusterCrossRefRepository: ClusterCrossRefRepository,
    private val clusterStore: FileEmbeddingStore,
    private val clusterMetadataRepository: ClusterMetadataRepository,
    private val mediaMetadataRepository: MediaMetadataRepository,
    private val mediaType: MediaType
) {
    companion object {
        private const val LARGE_DATASET_SIZE: Int = 5000
        private const val MIN_SAMPLE_SIZE: Int = 500
        private const val MAX_SAMPLE_SIZE: Int = 5000


    }
    private var clusterToMediaIdsMap: MutableMap<Long, MutableSet<Long>> = mutableMapOf()
    private var assignments: MutableMap<Long, Long> = mutableMapOf()

    suspend fun clusterMedia(itemEmbeds: List<StoredEmbedding>) {
        val existingAssignments = getAssignments()
        val validIds = mediaMetadataRepository.getByType(mediaType).map { it.id }.toSet()
        val existingClusters: Map<Long, Cluster> = getExistingClusters()

        val filteredItems = itemEmbeds
            .filterNot { it.id in existingAssignments }
            .filter { it.id in validIds }

        val defaultThreshold = if(existingClusters.isEmpty()) {
            val sampleSize = (filteredItems.size * 0.01).toInt().coerceIn(MIN_SAMPLE_SIZE, MAX_SAMPLE_SIZE)
            getDefaultThresholdFromSample(filteredItems, sampleSize)
        } else {
            getDefaultThreshold(existingClusters)
        }
        val clusterer = IncrementalClusterer(
            existingClusters = existingClusters,
            defaultThreshold = defaultThreshold
        )

        val result = clusterer.cluster(filteredItems)

        // Must update clusters first
        updateClusters(result, existingClusters)
        updateAssignments(result, validIds)
    }

    suspend fun getExistingClusters(): Map<Long, Cluster> {
        return if (clusterStore.exists) {
            val metadataMap = clusterMetadataRepository.getAllMetadataAsMap()

            clusterStore.get().mapNotNull { cluster ->
                metadataMap[cluster.id]?.let { meta ->
                    cluster.id to Cluster(cluster.id, cluster.embedding, meta)
                }
            }.toMap()
        } else emptyMap()
    }

    suspend fun updateClusters(clusterResult: ClusterResult, existingClustersMap: Map<Long, Cluster>) {
        val (existingClusters, newClusters) =
            clusterResult.clusters.values.partition { it.prototypeId in existingClustersMap }

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

        val newMetadata = newClusters.map {
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

        val existingEmbeds = existingClusters.map {
            StoredEmbedding(
                id = it.prototypeId,
                embedding = it.embedding,
                date = System.currentTimeMillis()
            )
        }

        val newEmbeds = newClusters.map {
            StoredEmbedding(
                id = it.prototypeId,
                embedding = it.embedding,
                date = System.currentTimeMillis()
            )
        }

        clusterStore.add(newEmbeds)
        clusterStore.update(existingEmbeds)
    }

    suspend fun getClusterToMediaIdsMap(): Map<Long, MutableSet<Long>> {
        if (clusterToMediaIdsMap.isNotEmpty()) return clusterToMediaIdsMap

        for(ref in clusterCrossRefRepository.getByType(mediaType)){
            clusterToMediaIdsMap.computeIfAbsent(ref.clusterId) { HashSet() }.add(ref.mediaId)
        }
        return clusterToMediaIdsMap
    }

    suspend fun getAssignments(): Map<Long, Long> {
        if (assignments.isNotEmpty()) return assignments

        val map = getClusterToMediaIdsMap()

        for ((clusterId, mediaIds) in map) {
            for (mediaId in mediaIds) {
                assignments[mediaId] = clusterId
            }
        }
        return assignments
    }

    fun clear() {
        clusterToMediaIdsMap.clear()
        assignments.clear()
    }

    private fun getDefaultThresholdFromSample(items: List<StoredEmbedding>, n: Int): Float{
        val sample = getSample(items, n)
        val clusterer = IncrementalClusterer(
            defaultThreshold = 0.6f
        )
        val result = clusterer.cluster(sample)
        return getDefaultThreshold(result.clusters)
    }

    private fun getDefaultThreshold(clusters: Map<Long, Cluster>): Float = clusters.values.map{it.metadata.meanSimilarity - it.metadata.stdSimilarity}.average().toFloat()

    private fun getSample(items: List<StoredEmbedding>, n: Int): List<StoredEmbedding>{
        return if(items.size > LARGE_DATASET_SIZE ) {
            reservoirSample(items, n)
        } else {
            items.shuffled().take(n)
        }
    }
    private suspend fun updateAssignments(clusterResult: ClusterResult, validIds: Set<Long> ) {
        val crossRefs = clusterResult.assignments.mapNotNull {
            if (it.key !in validIds) return@mapNotNull null

            ClusterCrossRef(
                clusterId = it.value,
                mediaId = it.key,
            )
        }
        clusterCrossRefRepository.upsertClusterCrossRefs(crossRefs)
    }
}