package com.fpf.smartscan.ui.screens.collections

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fpf.smartscan.data.tags.TagCrossRefRepository
import com.fpf.smartscan.data.tags.TagRepository
import com.fpf.smartscan.collections.MediaCollection
import com.fpf.smartscan.data.MediaDatabase
import com.fpf.smartscan.data.tags.TagWithCount
import com.fpf.smartscan.data.clusters.ClusterCrossRefRepository
import com.fpf.smartscan.data.clusters.MediaClusterMetadata
import com.fpf.smartscan.data.clusters.ClusterMetadataRepository
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.media.getImageUriFromId
import com.fpf.smartscan.media.getVideoUriFromId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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

    private val _state = MutableStateFlow(CollectionsState())
    val state: StateFlow<CollectionsState> = _state

    val mediaClusters: StateFlow<List<MediaClusterMetadata>> = combine(
        clusterMetadataRepository.getMetadataByTypeFlow(MediaType.IMAGE),
        clusterMetadataRepository.getMetadataByTypeFlow(MediaType.VIDEO),
            _state
        ) { imageClusters, videoClusters, state ->
            when (state.mediaType) {
                MediaType.IMAGE -> imageClusters
                MediaType.VIDEO -> videoClusters
            }
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Lazily,
                initialValue = emptyList()
            )


    val mediaCollections: StateFlow<List<MediaCollection>> = combine(
        tagsCrossRefRepository.getTagsWithCounts(),
        _state.map{it.mediaType to it.showAllCollections}
    ) { tagCounts, (mediaType, showAllCollections) ->
            if(showAllCollections) {
                getCollections(tagCounts, mediaType)
            } else{
                getCollections(tagCounts.take(6), mediaType)
            }
    }.flowOn(Dispatchers.IO).stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    fun renameCollection(collection: MediaCollection, newName: String){
        viewModelScope.launch (Dispatchers.IO){
            val tag = tagsRepository.getTagsByName(listOf(collection.name)).firstOrNull()
            tag?.let{tagsRepository.updateTags(listOf((it).copy(name = newName)))}
            _state.update { it.copy(selectedCollections = emptyList()) }
        }
    }


    fun deleteCollection(collection: MediaCollection){
        viewModelScope.launch (Dispatchers.IO){
            tagsRepository.deleteTagsByName(listOf(collection.name))
            _state.update { it.copy(selectedCollections = emptyList()) }
        }
    }

    fun mergeCollections(mediaType: MediaType, primaryCollectionName: String, otherCollections: List<MediaCollection>){
        viewModelScope.launch (Dispatchers.IO) {
            val primaryTag = tagsRepository.getTagsByName(listOf(primaryCollectionName)).firstOrNull()
            val tagsToMerge = tagsRepository.getTagsByName(otherCollections.map{it.name})
            val mediaToUpdate = tagsToMerge.map{tagsCrossRefRepository.getMediaIds(it.id)}.flatten()
            if(primaryTag != null && mediaToUpdate.isNotEmpty()){
                tagsCrossRefRepository.upsertTagCrossRefs(primaryTag.id, mediaToUpdate)
                tagsRepository.deleteTags(tagsToMerge)
            }
            _state.update { it.copy( selectedCollections = emptyList()) }
        }
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

    private suspend fun getCollections(tags: List<TagWithCount>, mediaType: MediaType): List<MediaCollection> {
        return tags.mapNotNull {
            val id = tagsCrossRefRepository.getMediaIds(it.id, limit = 1, offset = 0).firstOrNull()
            val uri = id?.let { id -> getUriFromMediaId(id, mediaType) }
            uri?.let { uri ->
                MediaCollection(
                    name = it.name,
                    thumbNail = uri,
                    size = it.count
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
