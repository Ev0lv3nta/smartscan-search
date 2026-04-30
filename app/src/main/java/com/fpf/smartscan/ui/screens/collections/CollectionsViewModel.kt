package com.fpf.smartscan.ui.screens.collections

import android.app.Application
import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fpf.smartscan.data.tags.TagCrossRefRepository
import com.fpf.smartscan.data.tags.TagRepository
import com.fpf.smartscan.media.MediaCollection
import com.fpf.smartscan.data.clusters.ClusterCrossRefRepository
import com.fpf.smartscan.data.clusters.ClusterMetadataRepository
import com.fpf.smartscan.data.clusters.ClusterMetadataWithCount
import com.fpf.smartscan.data.metadata.MediaMetadataRepository
import com.fpf.smartscan.data.tags.Tag
import com.fpf.smartscan.data.tags.TagCrossRef
import com.fpf.smartscan.media.mediaIdToUri
import com.fpf.smartscan.search.TagManager
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
import kotlin.collections.forEach
import kotlin.collections.plus


class CollectionsViewModel( 
    application: Application,
    private val tagRepository: TagRepository,
    private val tagCrossRefRepository: TagCrossRefRepository,
    private val clusterMetadataRepository: ClusterMetadataRepository,
    private val clusterCrossRefRepository: ClusterCrossRefRepository,
    private val mediaMetadataRepository: MediaMetadataRepository
) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "CollectionsViewModel"
        const val TOP_N = 6
    }

    val tagManager = TagManager(
        tagRepository=tagRepository,
        tagCrossRefRepository=tagCrossRefRepository,
        mediaMetadataRepository = mediaMetadataRepository,
    )

    private val _state = MutableStateFlow(CollectionsState())
    val state: StateFlow<CollectionsState> = _state

    val clusterCollections: StateFlow<List<MediaCollection>> = combine(
        clusterCrossRefRepository.getClustersWithCount(),
        _state.map {  it.showAllCollections to  it.viewAutoCollections }.distinctUntilChanged()
    ) { clusters, ( showAllCollections, viewAutoCollections) ->
        if(viewAutoCollections){
            _state.update { it.copy(totalCollections = clusters.size) }
        }
        val filteredClusters = if (showAllCollections) clusters else clusters.take(TOP_N)

        clustersToCollections(filteredClusters)
    }.flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )

    val tagCollections: StateFlow<List<MediaCollection>> = combine(
        tagCrossRefRepository.getTagsWithCounts(),
        _state.map {  it.showAllCollections to  it.viewAutoCollections }.distinctUntilChanged()
    ) { tagsWithCount, ( showAllCollections, viewAutoCollections) ->
        if(!viewAutoCollections){
            _state.update { it.copy(totalCollections = tagsWithCount.size) }
        }
        val tags = if (showAllCollections) tagsWithCount else tagsWithCount.take(TOP_N)
        tagManager.tagsToCollections(tags)
    }.flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    fun renameTagCollection(collection: MediaCollection, newName: String){
        viewModelScope.launch(Dispatchers.IO) {
            try{
                tagManager.renameTag(collection.name, newName)
                _state.update { it.copy(selectedCollections = emptySet()) }
            } catch (_: SQLiteConstraintException){
                _state.update { it.copy(error="Collection already exists") }
            }
        }
    }


    fun deleteTagCollections(collections: Set<MediaCollection>){
        viewModelScope.launch(Dispatchers.IO) {
            tagRepository.deleteTagsByName(collections.map{it.name})
            _state.update { it.copy(selectedCollections = emptySet()) }
        }
    }

    fun mergeCollections(primaryCollectionName: String, otherCollections: List<MediaCollection>){
        viewModelScope.launch (Dispatchers.IO) {
           tagManager.mergeTags(primaryCollectionName, otherCollections.map{it.name})
            _state.update { it.copy( selectedCollections = emptySet()) }
        }
    }

    fun renameClusterCollection(collection: MediaCollection, newName: String){
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cluster = clusterMetadataRepository.getMetadatas(listOf(collection.id)).firstOrNull()
                cluster?.let { clusterMetadataRepository.updateMetadatas(listOf(it.copy(label = newName))) }
                _state.update { it.copy(selectedCollections = emptySet()) }
            } catch (_: SQLiteConstraintException){
                _state.update { it.copy(error="Collection already exists") }
            }
        }
    }

    private suspend fun copyCollection(clusterId: Long, tagId: Long){
        val clusterCrossRefs = mediaMetadataRepository.getByCluster(clusterId)
        tagCrossRefRepository.upsertTagCrossRefs(clusterCrossRefs.map{ TagCrossRef(it.id, tagId)})
    }

    fun copyFromClusterToTagCollection(clusterCollections: Set<MediaCollection>, tagCollection: MediaCollection){
        viewModelScope.launch (Dispatchers.IO) {
           clusterCollections.forEach { copyCollection(it.id, tagCollection.id) }
            _state.update { it.copy( selectedCollections = emptySet()) }
        }
    }

    fun createNewCollectionAndCopy(clusterCollections: Set<MediaCollection>, newCollectionName: String){
        viewModelScope.launch (Dispatchers.IO) {
            try {
                val insertedIds = tagRepository.insertTags(listOf(Tag(name = newCollectionName)))
                val tagId = insertedIds.firstOrNull()?: return@launch
                clusterCollections.forEach { copyCollection(it.id, tagId) }
                _state.update { it.copy( selectedCollections = emptySet()) }
            }catch (_: SQLiteConstraintException){
             _state.update { it.copy(error="Collection already exists") }
            }
        }
    }

    fun resetErrorState(){
        _state.update { it.copy(error=null) }
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
        _state.update{currentState -> currentState.copy(selectedCollections = emptySet())}
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

    private suspend fun clustersToCollections(clusters: List<ClusterMetadataWithCount>): List<MediaCollection> {
        return clusters.mapNotNull {
            val id = mediaMetadataRepository.getByCluster(it.clusterId, limit = 1, offset = 0).firstOrNull()
            val uri = id?.let { id -> mediaIdToUri(id.id, it.type) }
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
}
