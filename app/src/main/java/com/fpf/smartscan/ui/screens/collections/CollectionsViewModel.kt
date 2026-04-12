package com.fpf.smartscan.ui.screens.collections

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fpf.smartscan.data.images.ImageDatabase
import com.fpf.smartscan.data.images.clusters.ImageClusterCrossRefRepository
import com.fpf.smartscan.data.images.clusters.ImageClusterMetadataRepository
import com.fpf.smartscan.data.images.tags.ImageTagCrossRefRepository
import com.fpf.smartscan.data.images.tags.ImageTagRepository
import com.fpf.smartscan.data.videos.VideoDatabase
import com.fpf.smartscan.data.videos.clusters.VideoClusterCrossRefRepository
import com.fpf.smartscan.data.videos.clusters.VideoClusterMetadataRepository
import com.fpf.smartscan.data.videos.tags.VideoTagCrossRefRepository
import com.fpf.smartscan.data.videos.tags.VideoTagRepository
import com.fpf.smartscan.collections.MediaCollection
import com.fpf.smartscan.data.MediaClusterMetadata
import com.fpf.smartscan.data.MediaTag
import com.fpf.smartscan.data.MediaTagCrossRef
import com.fpf.smartscan.data.MediaTagCrossRefRepository
import com.fpf.smartscan.data.MediaTagRepository
import com.fpf.smartscan.data.TagWithCount
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.media.getImageUriFromId
import com.fpf.smartscan.media.getVideoUriFromId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.collections.plus


class CollectionsViewModel( application: Application) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "CollectionsViewModel"
    }

    private val imageDB by lazy { ImageDatabase.getDatabase(application)}
    private val videoDB by lazy { VideoDatabase.getDatabase(application)}
    private val imageTagsRepository by lazy { ImageTagRepository(imageDB.tagDao())}
    private val videoTagsRepository by lazy { VideoTagRepository(videoDB.tagDao())}
    private val imageTagsCrossRefRepository by lazy { ImageTagCrossRefRepository( imageDB.imageTagCrossRefDao())}
    private val videoTagsCrossRefRepository by lazy {  VideoTagCrossRefRepository(videoDB.videoTagCrossRefDao())}

    private val imageClusterCrossRefRepository by lazy {  ImageClusterCrossRefRepository(imageDB.imageClusterCrossRefDao())}
    private val videoClusterCrossRefRepository by lazy {  VideoClusterCrossRefRepository(videoDB.videoClusterCrossRefDao())}

    private val imageClusterMetadataRepository by lazy {  ImageClusterMetadataRepository(imageDB.imageClusterMetadataDao())}
    private val videoClusterMetadataRepository by lazy {  VideoClusterMetadataRepository(videoDB.videoClusterMetadataDao())}

    private val _state = MutableStateFlow(CollectionsState())
    val state: StateFlow<CollectionsState> = _state

    val mediaClusters: StateFlow<List<MediaClusterMetadata>> = combine(
            imageClusterMetadataRepository.allMetadata,
            videoClusterMetadataRepository.allMetadata,
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
        imageTagsCrossRefRepository.getTagsWithCounts(),
        videoTagsCrossRefRepository.getTagsWithCounts(),
        _state
    ) { imageTagCounts, videoTagCounts, state ->
        when (state.mediaType) {
            MediaType.IMAGE -> {
                val collections = getCollections(imageTagCounts, state.mediaType)
                if(state.showAllCollections) collections else collections.take(6)
            }
            MediaType.VIDEO -> {
                val collections = getCollections(videoTagCounts, state.mediaType)
                if(state.showAllCollections) collections else collections.take(6)
            }
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    fun renameCollection(mediaType: MediaType, collection: MediaCollection, newName: String){
        viewModelScope.launch (Dispatchers.IO){
            when (mediaType) {
                MediaType.IMAGE -> {
                    val tag = imageTagsRepository.getTagsByName(listOf(collection.name)).firstOrNull()
                    tag?.let{imageTagsRepository.updateTags(listOf((it).copy(name = newName)))}
                }
                MediaType.VIDEO -> {
                    val tag = videoTagsRepository.getTagsByName(listOf(collection.name)).firstOrNull()
                    tag?.let{videoTagsRepository.updateTags(listOf(it.copy(name = newName)))}
                }
            }
            // TODO: update name in collection. However mapping is not efficient using StateFlow for list may be better suited
            _state.update { it.copy(selectedCollections = emptyList()) }
        }
    }


    fun deleteCollection(mediaType: MediaType, collection: MediaCollection){
        viewModelScope.launch (Dispatchers.IO){
            when (mediaType) {
                MediaType.IMAGE -> imageTagsRepository.deleteTagsByName(listOf(collection.name))
                MediaType.VIDEO -> videoTagsRepository.deleteTagsByName(listOf(collection.name))
            }
            _state.update { it.copy(selectedCollections = emptyList()) }
        }
    }

    fun mergeCollections(mediaType: MediaType, primaryCollectionName: String, otherCollections: List<MediaCollection>){
        viewModelScope.launch (Dispatchers.IO) {
            when (mediaType) {
                MediaType.IMAGE -> mergeTags(primaryCollectionName, otherCollections.map{it.name}, imageTagsRepository, imageTagsCrossRefRepository)
                MediaType.VIDEO -> mergeTags(primaryCollectionName, otherCollections.map{it.name}, videoTagsRepository, videoTagsCrossRefRepository)
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

    private suspend fun <T: MediaTag, K: MediaTagCrossRef>mergeTags(primaryTagName: String, namesOfTagsToMerge: List<String>, mediaTagRepository: MediaTagRepository<T>, mediaTagCrossRefRepository: MediaTagCrossRefRepository<K>){
            val primaryTag = mediaTagRepository.getTagsByName(listOf(primaryTagName)).firstOrNull()
            val tagsToMerge = mediaTagRepository.getTagsByName(namesOfTagsToMerge)
            val mediaToUpdate = tagsToMerge.map{mediaTagCrossRefRepository.getMediaIds(it.id)}.flatten()
            if(primaryTag != null && mediaToUpdate.isNotEmpty()){
                mediaTagCrossRefRepository.upsertTagCrossRefs(primaryTag.id, mediaToUpdate)
                mediaTagRepository.deleteTags(tagsToMerge)
        }
    }

    private suspend fun getCollections(tags: List<TagWithCount>, mediaType: MediaType): List<MediaCollection> {
        return tags.mapNotNull {
            val id = getMediaMatchingTag(it.id, mediaType, limit = 1).firstOrNull()
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

    private suspend fun getMediaMatchingTag(tagId: Long, mediaType: MediaType, limit: Int, offset: Int = 0): List<Long> {
        return when (mediaType) {
            MediaType.IMAGE -> imageTagsCrossRefRepository.getMediaIds(tagId, limit, offset)
            MediaType.VIDEO -> videoTagsCrossRefRepository.getMediaIds(tagId, limit, offset)
        }
    }

    private fun getUriFromMediaId(id: Long, mediaType: MediaType): Uri {
        return when (mediaType) {
            MediaType.IMAGE -> getImageUriFromId(id)
            MediaType.VIDEO -> getVideoUriFromId(id)
        }
    }
}
