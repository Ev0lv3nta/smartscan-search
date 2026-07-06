package com.fpf.smartscan.cluster

import com.fpf.smartscan.data.clusters.ClusterCrossRef
import com.fpf.smartscan.data.clusters.ClusterCrossRefRepository
import com.fpf.smartscan.data.clusters.ClusterMetadataRepository
import com.fpf.smartscan.data.clusters.ClusterMetadataWithCount
import com.fpf.smartscan.data.clusters.MediaClusterMetadata
import com.fpf.smartscan.data.metadata.MediaMetadataRepository
import com.fpf.smartscan.media.CollectionType
import com.fpf.smartscan.media.MediaCollection
import com.fpf.smartscan.media.MediaCollection.Companion.UNLABELLED_COLLECTION
import com.fpf.smartscan.media.MediaItem
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.media.mediaIdToUri
import com.fpf.smartscan.utils.reservoirSample
import com.fpf.smartscansdk.core.cluster.Assignments
import com.fpf.smartscansdk.core.cluster.Cluster
import com.fpf.smartscansdk.core.cluster.ClusterResult
import com.fpf.smartscansdk.core.cluster.IncrementalClusterer
import com.fpf.smartscansdk.core.embeddings.Embedding
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import com.fpf.smartscansdk.core.embeddings.StoredEmbedding
import com.fpf.smartscansdk.core.embeddings.generatePrototypeEmbedding
import com.fpf.smartscansdk.core.embeddings.getSimilarities
import com.fpf.smartscansdk.core.embeddings.toQInt8Embed
import kotlin.math.sqrt

class ClusterManager(
    private val clusterEmbedStore: FileEmbeddingStore,
    private val imageEmbedStore: FileEmbeddingStore,
    private val videoEmbedStore: FileEmbeddingStore,
    private val clusterCrossRefRepository: ClusterCrossRefRepository,
    private val clusterMetadataRepository: ClusterMetadataRepository,
    private val mediaMetadataRepository: MediaMetadataRepository,
) {
    companion object {
        private const val LARGE_DATASET_SIZE: Int = 10000
        private const val MIN_SAMPLE_SIZE: Int = 500
        private const val MAX_SAMPLE_SIZE: Int = 5000

        const val TAG = "ClusterManager"
    }

    suspend fun cluster() {
        val unclusteredItemIds = mediaMetadataRepository.getUnclusteredItemIds()
        val unclusterItemEmbeds = mutableListOf<StoredEmbedding>()
        // unclusteredItemIds includes video and image ids, each store will only return the ones that exist
        // this is simpler than making 2 separate queries that filter by media type
        unclusterItemEmbeds.addAll(imageEmbedStore.get(unclusteredItemIds))
        unclusterItemEmbeds.addAll(videoEmbedStore.get(unclusteredItemIds))
        if(unclusterItemEmbeds.isEmpty()) return

        val existingClusters: Map<Long, Cluster> = getExistingClusters()
        val defaultThreshold = if(existingClusters.isEmpty()) {
            val sampleSize = (unclusterItemEmbeds.size * 0.01).toInt().coerceIn(MIN_SAMPLE_SIZE, MAX_SAMPLE_SIZE)
            getDefaultThresholdFromSample(unclusterItemEmbeds, sampleSize)
        } else {
            getDefaultThreshold(existingClusters)
        }
        val clusterer = IncrementalClusterer(existingClusters = existingClusters, defaultThreshold = defaultThreshold)
        val result = clusterer.cluster(unclusterItemEmbeds.associate { it.id to it.embedding})
        updateClustersAndAssign(result, existingClusters.keys)
    }

    suspend fun getExistingClusters(): Map<Long, Cluster> {
        return if (clusterEmbedStore.exists) {
            val metadataMap = clusterMetadataRepository.getAllMetadataAsMap()

            clusterEmbedStore.get().mapNotNull { cluster ->
                metadataMap[cluster.id]?.let { meta ->
                    cluster.id to Cluster(cluster.id, cluster.embedding, meta)
                }
            }.toMap()
        } else emptyMap()
    }

    suspend fun mergeClusters(primaryClusterId: Long, otherClusters: List<Long>){
        val otherClustersCrossRefs = clusterCrossRefRepository.getByClusterIds(otherClusters)
        val updatedClusterCrossRefs = otherClustersCrossRefs.map { it.copy(clusterId = primaryClusterId) }
        clusterCrossRefRepository.insertClusterCrossRefs(updatedClusterCrossRefs)

        // Delete clusters which are being merged (cascades all related crossrefs)
        clusterMetadataRepository.deleteMetadata(otherClusters)
        clusterEmbedStore.remove(otherClusters)
        sync(primaryClusterId)
    }

    suspend fun moveItems(itemIds: List<Long>,newClusterId: Long, oldClusterId: Long){
        clusterCrossRefRepository.updateAssignments(itemIds, newClusterId)
        listOf(oldClusterId, newClusterId).forEach { sync(it) }
    }

    suspend fun createNewClusterAndMoveItems(items: Set<MediaItem>, newClusterLabel: String, oldClusterId: Long){
        val (imageItems, videoItems) = items.partition { it.type == MediaType.IMAGE }
        val itemEmbeds = mutableListOf<StoredEmbedding>()
        itemEmbeds.addAll(imageEmbedStore.get(imageItems.map{it.id}))
        itemEmbeds.addAll(videoEmbedStore.get(videoItems.map{it.id}))
        createNewCluster(itemEmbeds, newClusterLabel)
        sync(oldClusterId)
    }

    suspend fun updateLabel(clusterId: Long, newLabel: String){
        val cluster = clusterMetadataRepository.getMetadata(clusterId)
        cluster?.let { clusterMetadataRepository.updateMetadata(it.copy(label = newLabel)) }
    }

    suspend fun toCollections(clusters: List<ClusterMetadataWithCount>): List<MediaCollection> {
        return clusters.mapNotNull {
            val meta = mediaMetadataRepository.getByCluster(it.clusterId, limit = 1, offset = 0).firstOrNull()
            val uri = meta?.let { meta -> mediaIdToUri(meta.id, meta.type) }
            uri?.let { uri ->
                MediaCollection(
                    id = it.clusterId,
                    name = it.label?: UNLABELLED_COLLECTION,
                    thumbNail = uri,
                    size = it.count,
                    type = CollectionType.CLUSTER
                )
            }
        }
    }

    private suspend fun updateClustersAndAssign(clusterResult: ClusterResult, existingClusterIds: Set<Long>) {
        val (existingClusters, newClusters) = clusterResult.clusters.values.partition { it.clusterId in existingClusterIds }

        val existingMetadata = existingClusters.map {
            MediaClusterMetadata(
                clusterId = it.clusterId,
                prototypeSize = it.metadata.prototypeSize,
                meanSimilarity = it.metadata.meanSimilarity,
                stdSimilarity = it.metadata.stdSimilarity,
                label = it.metadata.label,
            )
        }

        val newMetadata = newClusters.map {
            MediaClusterMetadata(
                clusterId = it.clusterId,
                prototypeSize = it.metadata.prototypeSize,
                meanSimilarity = it.metadata.meanSimilarity,
                stdSimilarity = it.metadata.stdSimilarity,
                label = it.metadata.label,
            )
        }

        clusterMetadataRepository.updateMetadata(existingMetadata)
        clusterMetadataRepository.insertMetadata(newMetadata)

        val existingEmbeds = existingClusters.map {
            StoredEmbedding(
                id = it.clusterId,
                embedding = it.embedding.toQInt8Embed(),
                date = System.currentTimeMillis()
            )
        }

        val newEmbeds = newClusters.map {
            StoredEmbedding(
                id = it.clusterId,
                embedding = it.embedding.toQInt8Embed(),
                date = System.currentTimeMillis()
            )
        }

        clusterEmbedStore.add(newEmbeds)
        clusterEmbedStore.update(existingEmbeds)
        assign(clusterResult.assignments)
    }

    private suspend fun createNewCluster(itemEmbeds: List<StoredEmbedding>, clusterLabel: String): Long{
        val (metadata, prototype ) = if(itemEmbeds.size == 1) {
            val defaultThreshold = getDefaultThreshold(getExistingClusters())
            val meta = MediaClusterMetadata(
                clusterId = System.currentTimeMillis(),
                prototypeSize = itemEmbeds.size,
                meanSimilarity = defaultThreshold,
                stdSimilarity = 0f,
                label = clusterLabel,
            )
            val prototypeEmbedding = itemEmbeds.first().embedding
            Pair(meta, prototypeEmbedding)
        }else{
            val (prototypeEmbedding, meanSim, stdSim) = computeClusterMetrics(itemEmbeds.map { it.embedding })
            val meta = MediaClusterMetadata(
                clusterId = System.currentTimeMillis(),
                prototypeSize = itemEmbeds.size,
                meanSimilarity = meanSim,
                stdSimilarity = stdSim,
                label = clusterLabel,
            )
            Pair(meta, prototypeEmbedding)
        }

        val clusterEmbed =  StoredEmbedding(
            id = metadata.clusterId,
            embedding = prototype.toQInt8Embed(),
            date = System.currentTimeMillis()
        )
        clusterEmbedStore.add(listOf(clusterEmbed))
        clusterMetadataRepository.insertMetadata(metadata)
        clusterCrossRefRepository.updateAssignments(itemEmbeds.map { it.id }, clusterEmbed.id)
        return clusterEmbed.id
    }
    private fun getDefaultThresholdFromSample(items: List<StoredEmbedding>, n: Int): Float{
        val sample = getSample(items, n)
        val clusterer = IncrementalClusterer(defaultThreshold = 0.6f)
        val result = clusterer.cluster(sample.associate { it.id to it.embedding})
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
    private suspend fun assign(assignments: Assignments ) {
        val crossRefs = assignments.map {
            ClusterCrossRef(clusterId = it.value, mediaId = it.key)
        }
        clusterCrossRefRepository.insertClusterCrossRefs(crossRefs)
    }

    private fun computeClusterMetrics(embeddings: List<Embedding> ): Triple<Embedding, Float, Float>{
        val prototypeEmbedding = generatePrototypeEmbedding(embeddings)
        val sims = getSimilarities(prototypeEmbedding, embeddings)
        val meanSim = sims.average().toFloat()
        val stdSim = sqrt(sims.map { (it - meanSim) * (it - meanSim) }.average()).toFloat()
        return Triple(prototypeEmbedding, meanSim, stdSim)
    }

    private suspend fun sync(clusterId: Long){
        val clusterCrossRefs = clusterCrossRefRepository.getByClusterIds(listOf(clusterId))

        // Delete cluster from embed store if there is no items in cluster
        if(clusterCrossRefs.isEmpty()) {
            val clusterEmbed = clusterEmbedStore.get(listOf(clusterId)).firstOrNull()
            clusterEmbed?.let{ clusterEmbedStore.remove(listOf(it.id)) }
            return
        }

        val mediaIds = clusterCrossRefs.map{it.mediaId}
        val embeddings = mutableListOf<Embedding>()

        // Note: mediaIds may contain both image and video ids so get calls are required to both stores
        // In the event that it only contains 1 media type, then an empty list will be returned if that media type doesnt match the embed store
        // This is quicker than checking type via db
        embeddings.addAll(imageEmbedStore.get(mediaIds).map{it.embedding})
        embeddings.addAll(videoEmbedStore.get(mediaIds).map{it.embedding})

        val (prototypeEmbedding, meanSim, stdSim) = computeClusterMetrics(embeddings)
        val oldStoredEmbed = clusterEmbedStore.get(listOf(clusterId)).firstOrNull()?: error("Cluster embedding not found")
        val updatedStoredEmbed = oldStoredEmbed.copy(embedding = prototypeEmbedding)
        val clusterMetadata = clusterMetadataRepository.getMetadata(clusterId)?: return
        val updatedMetadata = clusterMetadata.copy(meanSimilarity = meanSim, stdSimilarity = stdSim, prototypeSize = mediaIds.size)
        clusterEmbedStore.update(listOf(updatedStoredEmbed))
        clusterMetadataRepository.updateMetadata(updatedMetadata)
    }

}