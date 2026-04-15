package com.fpf.smartscan.ui.screens.collections

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fpf.smartscan.data.tags.TagCrossRefRepository
import com.fpf.smartscan.data.tags.TagRepository
import com.fpf.smartscan.collections.MediaCollection
import com.fpf.smartscan.constants.EmbeddingStoresFiles
import com.fpf.smartscan.data.MediaDatabase
import com.fpf.smartscan.data.tags.TagWithCount
import com.fpf.smartscan.data.clusters.ClusterCrossRefRepository
import com.fpf.smartscan.data.clusters.MediaClusterMetadata
import com.fpf.smartscan.data.clusters.ClusterMetadataRepository
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.media.getImageUriFromId
import com.fpf.smartscan.media.getVideoUriFromId
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import kotlin.collections.plus


class CollectionsViewModel( application: Application) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "CollectionsViewModel"
    }

    private val db by lazy {  MediaDatabase.getDatabase(application)}

    private val tagsRepository by lazy { TagRepository(db.tagDao())}
    private val tagsCrossRefRepository by lazy { TagCrossRefRepository( db.tagCrossRefDao())}
    private val clusterCrossRefRepository by lazy { ClusterCrossRefRepository(db.clusterCrossRefDao()) }
    private val clusterMetadataRepository by lazy { ClusterMetadataRepository(db.clusterMetadataDao()) }

    private val imageClusterStore by lazy { FileEmbeddingStore(File(application.filesDir, EmbeddingStoresFiles.IMAGE_CLUSTER), 512)}

    private val videoClusterStore  by lazy { FileEmbeddingStore(File(application.filesDir, EmbeddingStoresFiles.VIDEO_CLUSTER), 512 )}


    private val _state = MutableStateFlow(CollectionsState())
    val state: StateFlow<CollectionsState> = _state

    val clusterCollections: StateFlow<List<MediaCollection>> = combine(
        clusterMetadataRepository.getMetadataByTypeFlow(MediaType.IMAGE),
        clusterMetadataRepository.getMetadataByTypeFlow(MediaType.VIDEO),
        _state.map { it.mediaType to it.showAllCollections }.distinctUntilChanged()
    ) { imageClusters, videoClusters, (mediaType, showAllCollections) ->

        val clusters = when (mediaType) {
            MediaType.IMAGE -> imageClusters
            MediaType.VIDEO -> videoClusters
        }
        val filteredClusters = if (showAllCollections) clusters else clusters.take(6)

        clustersToCollections(filteredClusters, mediaType)
    }.flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )

    val tagCollections: StateFlow<List<MediaCollection>> = combine(
        tagsCrossRefRepository.getTagsWithCounts(),
        _state.map { it.mediaType to it.showAllCollections }.distinctUntilChanged()
    ) { tagsWithCount, (mediaType, showAllCollections) ->
        val tags = if (showAllCollections) tagsWithCount else tagsWithCount.take(6)
        tagsToCollections(tags, mediaType)
    }.flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    fun renameTagCollection(collection: MediaCollection, newName: String){
        viewModelScope.launch(Dispatchers.IO) {
            val tag = tagsRepository.getTagsByName(listOf(collection.name)).firstOrNull()
            tag?.let { tagsRepository.updateTags(listOf((it).copy(name = newName))) }
            _state.update { it.copy(selectedCollections = emptyList()) }
        }
    }


    fun deleteTagCollection(collection: MediaCollection){
        viewModelScope.launch(Dispatchers.IO) {
            tagsRepository.deleteTagsByName(listOf(collection.name))
            _state.update { it.copy(selectedCollections = emptyList()) }
        }
    }

    fun mergeTagCollections(primaryCollectionName: String, otherCollections: List<MediaCollection>){
        viewModelScope.launch(Dispatchers.IO) {
            val primaryTag =
                tagsRepository.getTagsByName(listOf(primaryCollectionName)).firstOrNull()
            val tagsToMerge = tagsRepository.getTagsByName(otherCollections.map { it.name })
            val mediaToUpdate = tagsToMerge.flatMap { tagsCrossRefRepository.getMediaIds(it.id) }
            if (primaryTag != null && mediaToUpdate.isNotEmpty()) {
                tagsCrossRefRepository.upsertTagCrossRefs(primaryTag.id, mediaToUpdate)
                tagsRepository.deleteTags(tagsToMerge)
            }
            _state.update { it.copy(selectedCollections = emptyList()) }
        }
    }

    fun renameClusterCollection(collection: MediaCollection, newName: String){
        viewModelScope.launch(Dispatchers.IO) {
            val cluster =
                clusterMetadataRepository.getMetadatas(listOf(collection.id)).firstOrNull()
            cluster?.let { clusterMetadataRepository.updateMetadatas(listOf(it.copy(label = newName))) }
            _state.update { it.copy(selectedCollections = emptyList()) }
        }
    }

  fun mergeClusterCollections(primaryCollectionName: String, otherCollections: List<MediaCollection>){
        val primaryCollection = _state.value.selectedCollections.find { it.name == primaryCollectionName }?: return
        viewModelScope.launch(Dispatchers.IO) {
            val primaryCluster = clusterMetadataRepository.getMetadatas(listOf(primaryCollection.id)).firstOrNull()
            val idsToMerge = otherCollections.map { it.id }
            val clustersToMerge = clusterMetadataRepository.getMetadatas(idsToMerge)
            val mediaToUpdate =
                clustersToMerge.flatMap { clusterCrossRefRepository.getMediaIds(it.clusterId) }
            if (primaryCluster != null && mediaToUpdate.isNotEmpty()) {
                val store =
                    if (primaryCluster.type == MediaType.IMAGE) imageClusterStore else videoClusterStore
                val storedEmbeds = store.get(listOf(primaryCollection.id) + idsToMerge)
                if (storedEmbeds.size != otherCollections.size + 1) error("Missing cluster in embed store")
                val embeddings = mutableListOf<FloatArray>()
                val counts = mutableListOf<Int>()
                val idToClusterMetadata = clustersToMerge.associateBy { it.clusterId }
                for (emb in storedEmbeds) {
                    val meta = idToClusterMetadata[emb.id] ?: continue
                    counts.add(meta.prototypeSize)
                    embeddings.add(emb.embedding)
                }


                val newPrototype = mergePrototypes(embeddings, counts)
                val newSize = counts.sum()
                val updatedMeta = primaryCluster.copy(prototypeSize = newSize)

                // TODO: need to add update method to embedding store
                // TODO: need to recompute std and mean sim
//            clusterMetadataRepository.updateMetadatas(listOf(updatedMeta))
//            clusterCrossRefRepository.upsertClusterCrossRefs(primaryCluster.clusterId, mediaToUpdate)
//            clusterMetadataRepository.deleteMetadatas(idsToMerge)
            }
            _state.update { it.copy(selectedCollections = emptyList()) }
        }
    }

    private fun mergePrototypes(prototypes: List<FloatArray>, counts: List<Int>): FloatArray {
        require(prototypes.isNotEmpty()) { "No prototypes provided" }
        require(prototypes.size == counts.size) { "Mismatched inputs" }

        val dim = prototypes[0].size
        for (p in prototypes) {
            require(p.size == dim) { "Embedding dimensions must match" }
        }

        val total = counts.sum()
        require(total > 0) { "Total count must be > 0" }

        val result = FloatArray(dim)

        for (i in prototypes.indices) {
            val p = prototypes[i]
            val w = counts[i]
            for (d in 0 until dim) {
                result[d] += p[d] * w
            }
        }

        for (d in 0 until dim) {
            result[d] /= total
        }

        return result
    }

    fun toggleSelectedCollection(collection: MediaCollection){
        _state.update { currentState ->
            if (collection in currentState.selectedCollections) {
                val updatedSelectedResults = currentState.selectedCollections - collection
                currentState.copy(selectedCollections = updatedSelectedResults)
            } else {
                val updatedSelectedResults = currentState.selectedCollections + collection
                currentState.copy(selectedCollections = updatedSelectedResults)
            }
        }
    }

    fun clearSelectedCollections(){
        _state.update{currentState -> currentState.copy(selectedCollections = emptyList())}
    }

    fun toggleViewAllCollections(){
        _state.update{ it.copy(showAllCollections = !it.showAllCollections)}
    }

    fun setCollectionToView(collection: MediaCollection?){
        _state.update { it.copy(collectToView = collection) }
    }

    fun toggleViewAutoCollections(){
        _state.update { it.copy(viewAutoCollections = !it.viewAutoCollections) }
    }

    private suspend fun tagsToCollections(tags: List<TagWithCount>, mediaType: MediaType): List<MediaCollection> {
        return tags.mapNotNull {
            val id = tagsCrossRefRepository.getMediaIds(it.id, limit = 1, offset = 0).firstOrNull()
            val uri = id?.let { id -> getUriFromMediaId(id, mediaType) }
            uri?.let { uri ->
                MediaCollection(
                    id = it.id,
                    name = it.name,
                    thumbNail = uri,
                    size = it.count
                )
            }
        }
    }

    private suspend fun clustersToCollections(clusters: List<MediaClusterMetadata>, mediaType: MediaType): List<MediaCollection> {
        return clusters.mapNotNull {
            val id = clusterCrossRefRepository.getMediaIds(it.clusterId, limit = 1, offset = 0).firstOrNull()
            val uri = id?.let { id -> getUriFromMediaId(id, mediaType) }
            uri?.let { uri ->
                MediaCollection(
                    id = it.clusterId,
                    name = it.label?: "?",
                    thumbNail = uri,
                    size = it.prototypeSize,
                    isAutoCollection = true
                )
            }
        }
    }

    private fun getUriFromMediaId(id: Long, mediaType: MediaType): Uri {
        return when (mediaType) {
            MediaType.IMAGE -> getImageUriFromId(id)
            MediaType.VIDEO -> getVideoUriFromId(id)
        }
    }
}
