package com.fpf.smartscan.cluster

import com.fpf.smartscan.data.clusters.ClusterCrossRef
import com.fpf.smartscan.data.clusters.ClusterCrossRefRepository
import com.fpf.smartscan.data.clusters.ClusterMetadataRepository
import com.fpf.smartscan.data.clusters.ClusterMetadataWithCount
import com.fpf.smartscan.data.clusters.MediaClusterMetadata
import com.fpf.smartscan.data.metadata.MediaMetadataRepository
import com.fpf.smartscan.media.MediaCollection
import com.fpf.smartscan.media.mediaIdToUri
import com.fpf.smartscan.utils.reservoirSample
import com.fpf.smartscansdk.core.cluster.Cluster
import com.fpf.smartscansdk.core.cluster.ClusterResult
import com.fpf.smartscansdk.core.cluster.IncrementalClusterer
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import com.fpf.smartscansdk.core.embeddings.StoredEmbedding
import com.fpf.smartscansdk.core.embeddings.generatePrototypeEmbedding
import com.fpf.smartscansdk.core.embeddings.getSimilarities
import kotlin.collections.iterator
import kotlin.math.sqrt

class ClusterManager(
    private val clusterCrossRefRepository: ClusterCrossRefRepository,
    private val clusterStore: FileEmbeddingStore,
    private val clusterMetadataRepository: ClusterMetadataRepository,
    private val mediaMetadataRepository: MediaMetadataRepository,
) {
    companion object {
        private const val LARGE_DATASET_SIZE: Int = 10000
        private const val MIN_SAMPLE_SIZE: Int = 500
        private const val MAX_SAMPLE_SIZE: Int = 5000
    }
    private var clusterToMediaIdsMap: MutableMap<Long, MutableSet<Long>> = mutableMapOf()
    private var assignments: MutableMap<Long, Long> = mutableMapOf()

    suspend fun cluster(itemEmbeds: List<StoredEmbedding>) {
        val existingAssignments = getAssignments()
        val validIds = mediaMetadataRepository.getAllIds().toSet()
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
        updateClustersFromResult(result, existingClusters)
        updateAssignmentsFromResult(result, validIds)
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

    suspend fun mergeClusters(primaryClusterId: Long, otherClusters: List<Long>, imageEmbedStore: FileEmbeddingStore, videoEmbedStore: FileEmbeddingStore){
        val primaryClusterMetadata = clusterMetadataRepository.getMetadatas(listOf(primaryClusterId)).firstOrNull()?: return
        val mediaIdsInPrimaryCluster = mediaMetadataRepository.getByCluster(primaryClusterId).map{it.id}
        val otherClustersCrossRefs = clusterCrossRefRepository.getByClusterIds(otherClusters)
        val updatedClusterCrossRefs = otherClustersCrossRefs.map { it.copy(clusterId = primaryClusterId) }
        clusterCrossRefRepository.upsertClusterCrossRefs(updatedClusterCrossRefs)
        clusterMetadataRepository.deleteMetadatas(otherClusters)

        val mediaIdsInCluster = mediaIdsInPrimaryCluster + otherClustersCrossRefs.map { it.mediaId }.toSet()
        val embeddings = mutableListOf<FloatArray>()

        // Note: mediaIds may contain both image and video ids so get calls are required to both stores
        // In the event that it only contains 1 media type, then an empty list will be returned if that media type doesnt match the embed store
        // This is quicker than checking type via db
        embeddings.addAll(imageEmbedStore.get(mediaIdsInCluster).map{it.embedding})
        embeddings.addAll(videoEmbedStore.get(mediaIdsInCluster).map{it.embedding})
        val (prototypeEmbedding, meanSim, stdSim) = computeClusterMetrics(embeddings)

        // Update primary cluster
        val oldStoredEmbed = clusterStore.get(listOf(primaryClusterId)).firstOrNull()?: error("Cluster embedding not found")
        val updatedStoredEmbed = oldStoredEmbed.copy(embedding = prototypeEmbedding)
        clusterStore.update(listOf(updatedStoredEmbed))

        val updatedMetadata = primaryClusterMetadata.copy(meanSimilarity = meanSim, stdSimilarity = stdSim, prototypeSize = mediaIdsInCluster.size)
        clusterMetadataRepository.updateMetadatas(listOf(updatedMetadata))
    }

    suspend fun getClusterToMediaIdsMap(): Map<Long, MutableSet<Long>> {
        if (clusterToMediaIdsMap.isNotEmpty()) return clusterToMediaIdsMap

        for(ref in clusterCrossRefRepository.getAllCrossRefs()){
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

    suspend fun updateLabel(clusterId: Long, newLabel: String){
        val cluster = clusterMetadataRepository.getMetadatas(listOf(clusterId)).firstOrNull()
        cluster?.let { clusterMetadataRepository.updateMetadatas(listOf(it.copy(label = newLabel))) }
    }

    suspend fun toCollections(clusters: List<ClusterMetadataWithCount>): List<MediaCollection> {
        return clusters.mapNotNull {
            val meta = mediaMetadataRepository.getByCluster(it.clusterId, limit = 1, offset = 0).firstOrNull()
            val uri = meta?.let { meta -> mediaIdToUri(meta.id, meta.type) }
            uri?.let { uri ->
                MediaCollection(
                    id = it.clusterId,
                    name = it.label?: "?",
                    thumbNail = uri,
                    size = it.count,
                    isAutoCollection = true
                )
            }
        }
    }

    private suspend fun updateClustersFromResult(clusterResult: ClusterResult, existingClustersMap: Map<Long, Cluster>) {
        val (existingClusters, newClusters) = clusterResult.clusters.values.partition { it.prototypeId in existingClustersMap }

        val existingMetadata = existingClusters.map {
            MediaClusterMetadata(
                clusterId = it.prototypeId,
                prototypeSize = it.metadata.prototypeSize,
                meanSimilarity = it.metadata.meanSimilarity,
                stdSimilarity = it.metadata.stdSimilarity,
                label = it.metadata.label,
            )
        }

        val newMetadata = newClusters.map {
            MediaClusterMetadata(
                clusterId = it.prototypeId,
                prototypeSize = it.metadata.prototypeSize,
                meanSimilarity = it.metadata.meanSimilarity,
                stdSimilarity = it.metadata.stdSimilarity,
                label = it.metadata.label,
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
    private suspend fun updateAssignmentsFromResult(clusterResult: ClusterResult, validIds: Set<Long> ) {
        val crossRefs = clusterResult.assignments.mapNotNull {
            if (it.key !in validIds) return@mapNotNull null

            ClusterCrossRef(
                clusterId = it.value,
                mediaId = it.key,
            )
        }
        clusterCrossRefRepository.upsertClusterCrossRefs(crossRefs)
    }

    private fun computeClusterMetrics(embeddings: List<FloatArray> ): Triple<FloatArray, Float, Float>{
        val prototypeEmbedding = generatePrototypeEmbedding(embeddings)
        val sims = getSimilarities(prototypeEmbedding, embeddings)
        val meanSim = sims.average().toFloat()
        val stdSim = sqrt(sims.map { (it - meanSim) * (it - meanSim) }.average()).toFloat()
        return Triple(prototypeEmbedding, meanSim, stdSim)
    }
}