package com.fpf.smartscan.ui.screens.collections

import android.app.Application
import android.database.sqlite.SQLiteConstraintException
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fpf.smartscan.data.tags.TagCrossRefRepository
import com.fpf.smartscan.data.tags.TagRepository
import com.fpf.smartscan.media.MediaCollection
import com.fpf.smartscan.data.MediaDatabase
import com.fpf.smartscan.data.tags.TagWithCount
import com.fpf.smartscan.data.clusters.ClusterCrossRefRepository
import com.fpf.smartscan.data.clusters.ClusterMetadataRepository
import com.fpf.smartscan.data.clusters.ClusterMetadataWithCount
import com.fpf.smartscan.data.metadata.MediaMetadataRepository
import com.fpf.smartscan.data.tags.Tag
import com.fpf.smartscan.data.tags.TagCrossRef
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.media.getImageUriFromId
import com.fpf.smartscan.media.getVideoUriFromId
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
import kotlin.collections.plus


class CollectionsViewModel( application: Application) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "CollectionsViewModel"
        const val TOP_N = 6
    }

    private val db by lazy {  MediaDatabase.getDatabase(application)}

    private val tagsRepository by lazy { TagRepository(db.tagDao())}
    private val tagsCrossRefRepository by lazy { TagCrossRefRepository( db.tagCrossRefDao())}
    private val clusterCrossRefRepository by lazy { ClusterCrossRefRepository(db.clusterCrossRefDao()) }
    private val clusterMetadataRepository by lazy { ClusterMetadataRepository(db.clusterMetadataDao()) }
    private val mediaMetadataRepository by lazy { MediaMetadataRepository( db.metadataDao())}


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
        tagsCrossRefRepository.getTagsWithCounts(),
        _state.map {  it.showAllCollections to  it.viewAutoCollections }.distinctUntilChanged()
    ) { tagsWithCount, ( showAllCollections, viewAutoCollections) ->
        if(!viewAutoCollections){
            _state.update { it.copy(totalCollections = tagsWithCount.size) }
        }
        val tags = if (showAllCollections) tagsWithCount else tagsWithCount.take(TOP_N)
        tagsToCollections(tags)
    }.flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    fun renameTagCollection(collection: MediaCollection, newName: String){
        viewModelScope.launch(Dispatchers.IO) {
            try{
                val tag = tagsRepository.getTagsByName(listOf(collection.name)).firstOrNull()
                tag?.let { tagsRepository.updateTags(listOf((it).copy(name = newName))) }
                _state.update { it.copy(selectedCollections = emptySet()) }
            } catch (_: SQLiteConstraintException){
                _state.update { it.copy(error="Collection already exists") }
            }
        }
    }


    fun deleteTagCollections(collections: Set<MediaCollection>){
        viewModelScope.launch(Dispatchers.IO) {
            tagsRepository.deleteTagsByName(collections.map{it.name})
            _state.update { it.copy(selectedCollections = emptySet()) }
        }
    }

    fun mergeCollections(primaryCollectionName: String, otherCollections: List<MediaCollection>){
        viewModelScope.launch (Dispatchers.IO) {
            val primaryTag = tagsRepository.getTagsByName(listOf(primaryCollectionName)).firstOrNull()
            val tagsToMerge = tagsRepository.getTagsByName(otherCollections.map{it.name})
            val mediaToUpdate = tagsToMerge.flatMap { mediaMetadataRepository.getByTag(it.id) }
            if(primaryTag != null && mediaToUpdate.isNotEmpty()){
                val updated = mediaToUpdate.map{ TagCrossRef(mediaId = it.id, type = it.type, tagId = primaryTag.id)}
                tagsCrossRefRepository.upsertTagCrossRefs(updated)
                tagsRepository.deleteTags(tagsToMerge)
            }
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

    fun copyFromClusterToTagCollection(clusterCollection: MediaCollection, tagCollection: MediaCollection){
        viewModelScope.launch (Dispatchers.IO) {
            val clusterCrossRefs = mediaMetadataRepository.getByCluster(clusterCollection.id)
            tagsCrossRefRepository.upsertTagCrossRefs(clusterCrossRefs.map{ TagCrossRef(it.id, tagCollection.id, it.type)})
            _state.update { it.copy( selectedCollections = emptySet()) }
        }
    }

    fun createNewCollectionAndCopy(clusterCollection: MediaCollection, newCollectionName: String){
        viewModelScope.launch (Dispatchers.IO) {
            try {
                val insertedIds = tagsRepository.insertTags(listOf(Tag(name = newCollectionName)))
                if(insertedIds.isEmpty()) return@launch
                val clusterCrossRefs = mediaMetadataRepository.getByCluster(clusterCollection.id)
                tagsCrossRefRepository.upsertTagCrossRefs(clusterCrossRefs.map{ TagCrossRef(it.id, insertedIds.first(), it.type)})
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

    private suspend fun tagsToCollections(tags: List<TagWithCount>): List<MediaCollection> {
        return tags.mapNotNull {
            val mediaMeta = mediaMetadataRepository.getByTag(it.id, limit = 1, offset = 0).firstOrNull()
            val uri = mediaMeta?.let { mediaMeta -> getUriFromMediaId(mediaMeta.id, mediaMeta.type) }
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

    private suspend fun clustersToCollections(clusters: List<ClusterMetadataWithCount>): List<MediaCollection> {
        return clusters.mapNotNull {
            val id = mediaMetadataRepository.getByCluster(it.clusterId, limit = 1, offset = 0).firstOrNull()
            val uri = id?.let { id -> getUriFromMediaId(id.id, it.type) }
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

    private fun getUriFromMediaId(id: Long, mediaType: MediaType): Uri {
        return when (mediaType) {
            MediaType.IMAGE -> getImageUriFromId(id)
            MediaType.VIDEO -> getVideoUriFromId(id)
        }
    }
}
