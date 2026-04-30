package com.fpf.smartscan.search

import com.fpf.smartscan.data.clusters.ClusterCrossRef
import com.fpf.smartscan.data.clusters.ClusterCrossRefRepository
import com.fpf.smartscan.data.clusters.ClusterMetadataRepository
import com.fpf.smartscan.data.clusters.MediaClusterMetadata
import com.fpf.smartscan.data.metadata.MediaMetadataRepository
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscansdk.core.cluster.Cluster
import com.fpf.smartscansdk.core.cluster.ClusterResult
import com.fpf.smartscansdk.core.cluster.IncrementalClusterer
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import com.fpf.smartscansdk.core.embeddings.StoredEmbedding
import kotlin.collections.filterNot
import kotlin.collections.mapNotNull
import kotlin.math.log2
import kotlin.math.pow

class ClusterManager(
    private val clusterCrossRefRepository: ClusterCrossRefRepository,
    private val clusterStore: FileEmbeddingStore,
    private val clusterMetadataRepository: ClusterMetadataRepository,
    private val mediaMetadataRepository: MediaMetadataRepository,
    private val mediaType: MediaType
) {
    private var clusterToMediaIdsMap: MutableMap<Long, MutableSet<Long>> = mutableMapOf()
    private var clusterCounts: MutableMap<Long, Int> = mutableMapOf()
    private var assignments: MutableMap<Long, Long> = mutableMapOf()

    suspend fun clusterMedia(itemEmbeds: List<StoredEmbedding>) {
        val existingAssignments = getAssignments()
        val validIds = mediaMetadataRepository.getByType(mediaType).map { it.id }.toSet()
        val existingClusters: Map<Long, Cluster> = getExistingClusters()

        val filteredItems = itemEmbeds
            .filterNot { it.id in existingAssignments }
            .filter { it.id in validIds }

        val clusterer = IncrementalClusterer(
            existingClusters = existingClusters,
            defaultThreshold = 0.5f
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

    // Singletons are handled separately from the main clusters to prevent singletons dominating topK
    suspend fun getTargetClusters(queryEmbedding: FloatArray, threshold: Float): List<Long>{
        val (singletonClusters, mainClusters) =  getClusterCounts().entries.partition { it.value == 1 }
        val singletonCount = singletonClusters.size
        val totalClusters =  mainClusters.size + singletonCount

        val baseTopK = computeDynamicTopK(totalClusters, singletonCount)
        val singletonTopK = computeSingletonTopK(baseTopK,totalClusters, singletonCount)

        if(!clusterStore.exists) return emptyList()

        val mainResultIds = clusterStore.query(queryEmbedding, baseTopK, threshold, ids = mainClusters.map{it.key}.toSet())
        val singletonResultIds = clusterStore.query(queryEmbedding, singletonTopK, threshold, ids = singletonClusters.map{it.key}.toSet())
        return mainResultIds + singletonResultIds
    }

    suspend fun getIdsInTargetClusters(targetClusters: List<Long>): Set<Long>{
        val idsMatchingCluster: Set<Long> = buildSet {
            for (clusterId in targetClusters) {
                val ids = getClusterToMediaIdsMap()[clusterId] ?: continue
                addAll(ids)
            }
        }
        return idsMatchingCluster
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

    suspend fun getClusterCounts(): Map<Long, Int> {
        if (clusterCounts.isNotEmpty()) return clusterCounts

        val map = getClusterToMediaIdsMap()

        for ((clusterId, mediaIds) in map) {
            clusterCounts[clusterId] = mediaIds.size
        }
        return clusterCounts
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

    // increases search breadth when cluster fragmentation is high
    private fun computeDynamicTopK(totalItems: Int, singletonCount: Int, min: Int = 3): Int {
        val base = log2(totalItems.toDouble())
        val singletonRatio = singletonCount.toDouble() / totalItems
        return (base * (1.0 + singletonRatio)).toInt().coerceAtLeast(min)
    }

    private fun computeSingletonTopK(baseTopK: Int, singletonCount: Int, totalClusters: Int, sharpness: Double = 3.0): Int {
        if (totalClusters == 0) return baseTopK
        val t = (singletonCount.toDouble() / totalClusters).coerceIn(0.0, 1.0)
        val expansion = 1.0 - (1.0 - t).pow(sharpness)
        val computedTopK = baseTopK + (singletonCount - baseTopK).coerceAtLeast(0) * expansion
        return computedTopK.toInt().coerceAtLeast(baseTopK)
    }

    fun clear() {
        clusterToMediaIdsMap.clear()
        clusterCounts.clear()
        assignments.clear()
    }
}