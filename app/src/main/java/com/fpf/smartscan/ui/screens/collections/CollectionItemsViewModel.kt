package com.fpf.smartscan.ui.screens.collections

import android.app.Application
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.fpf.smartscan.collections.MediaCollection
import com.fpf.smartscan.data.MediaPagingSource
import com.fpf.smartscan.data.MediaTag
import com.fpf.smartscan.data.TagWithCount
import com.fpf.smartscan.data.images.ImageDatabase
import com.fpf.smartscan.data.images.tags.ImageTagCrossRefRepository
import com.fpf.smartscan.data.images.tags.ImageTagRepository
import com.fpf.smartscan.data.videos.VideoDatabase
import com.fpf.smartscan.data.videos.tags.VideoTagCrossRefRepository
import com.fpf.smartscan.data.videos.tags.VideoTagRepository
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.media.getImageUriFromId
import com.fpf.smartscan.media.getVideoUriFromId
import com.fpf.smartscan.media.openImageInGallery
import com.fpf.smartscan.media.openVideoInGallery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.collections.plus


class CollectionItemsViewModel( application: Application) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "CollectionItemsViewModel"
    }

    private val imageDB by lazy { ImageDatabase.getDatabase(application)}
    private val videoDB by lazy { VideoDatabase.getDatabase(application)}
    private val imageTagsCrossRefRepository by lazy { ImageTagCrossRefRepository( imageDB.imageTagCrossRefDao())}
    private val videoTagsCrossRefRepository by lazy {  VideoTagCrossRefRepository(videoDB.videoTagCrossRefDao())}
    private val imageTagsRepository by lazy { ImageTagRepository(imageDB.tagDao())}
    private val videoTagsRepository by lazy { VideoTagRepository(videoDB.tagDao())}

    private val _state = MutableStateFlow(CollectionItemsState())
    val state: StateFlow<CollectionItemsState> = _state

    @OptIn(ExperimentalCoroutinesApi::class)
    val mediaItems = _state
        .map { it.mediaType to it.collectionName }
        .distinctUntilChanged()
        .flatMapLatest { (mediaType, collectionName) ->

            val tagId = getTag(mediaType, collectionName)?.id

            if (tagId == null) {
                flowOf(PagingData.empty())
            } else {
                Pager(
                    config = PagingConfig(
                        pageSize = 100,
                        initialLoadSize = 100,
                        enablePlaceholders = false
                    ),
                    pagingSourceFactory = {
                        MediaPagingSource(
                            mediaType = mediaType,
                            tagId = tagId,
                            imageRepo = imageTagsCrossRefRepository,
                            videoRepo = videoTagsCrossRefRepository,
                            mediaIdToUri = ::mediaIdToUri
                        )
                    }
                ).flow
            }
        }
        .cachedIn(viewModelScope)

    val mediaCollections: StateFlow<List<MediaCollection>> = combine(
            imageTagsCrossRefRepository.getTagsWithCounts(),
            videoTagsCrossRefRepository.getTagsWithCounts(),
            _state.map { it.mediaType }
        ) { imageTagCounts, videoTagCounts, mediaType ->

            when (mediaType) {
                MediaType.IMAGE -> getCollections(imageTagCounts, mediaType)
                MediaType.VIDEO -> getCollections(videoTagCounts, mediaType)
            }
        }
            .flowOn(Dispatchers.IO)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Lazily,
                initialValue = emptyList()
            )

    fun removeItems(mediaType: MediaType, mediaUris: List<Uri>){
        val mediaIds = mediaUris.map{uriToMediaId(it)}
        val collectionName = _state.value.collectionName?: return

        viewModelScope.launch (Dispatchers.IO){
            val tag = getTag(mediaType, collectionName)?: return@launch
            deleteMediaMatchingTag(mediaType, mediaIds, tag.id)
            _state.update { it.copy(selectedMediaItems = emptyList()) }
        }
    }

    fun moveItems(mediaType: MediaType, mediaUris: List<Uri>, newCollection: MediaCollection){
        val mediaIds = mediaUris.map{uriToMediaId(it)}
        val oldCollectionName = _state.value.collectionName?: return

        viewModelScope.launch (Dispatchers.IO){
            val oldTag = getTag(mediaType, oldCollectionName)?: return@launch
            val newTag = getTag(mediaType, newCollection.name)?: return@launch

            when (mediaType) {
                MediaType.IMAGE -> imageTagsCrossRefRepository.upsertTagCrossRefs(newTag.id, mediaIds)
                MediaType.VIDEO -> videoTagsCrossRefRepository.upsertTagCrossRefs(newTag.id, mediaIds)
            }
            deleteMediaMatchingTag(mediaType, mediaIds, oldTag.id)
            _state.update { it.copy(selectedMediaItems = emptyList()) }
        }
    }


    fun toggleSelectedItem(item: Uri){
        _state.update { currentState ->
            if (item in currentState.selectedMediaItems) {
                val updatedSelectedResults = currentState.selectedMediaItems - item
                currentState.copy(selectedMediaItems = updatedSelectedResults)
            } else {
                val updatedSelectedResults = currentState.selectedMediaItems + item
                currentState.copy(selectedMediaItems = updatedSelectedResults)
            }
        }
    }

    fun clearSelectedItems(){
        _state.update{currentState -> currentState.copy(selectedMediaItems = emptyList())}
    }

    fun setCollection(name: String?){
        _state.update { it.copy(collectionName=name) }
    }

    fun setMediaToView(context: Context, uri: Uri?, autoOpenInGallery: Boolean? = null, isSelecting: Boolean = false){
        if(autoOpenInGallery == true && !isSelecting) {
            when(_state.value.mediaType){
                MediaType.IMAGE -> {
                    uri?.let{openImageInGallery(context, it)}
                }
                MediaType.VIDEO -> {
                    uri?.let{openVideoInGallery(context, it)}
                }
            }
        }else{
            _state.update { it.copy(mediaToView =uri) }
        }
    }

    private suspend fun getCollections(tags: List<TagWithCount>, mediaType: MediaType): List<MediaCollection> {
        return tags.mapNotNull {
            val id = getMediaMatchingTag(it.id, mediaType, limit = 1).firstOrNull()
            val uri = id?.let { id -> mediaIdToUri(id, mediaType) }
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
    private suspend fun getTag(mediaType: MediaType, collectionName: String?): MediaTag?{
        collectionName?: return null
        return when (mediaType) {
            MediaType.IMAGE -> imageTagsRepository.getTagsByName(listOf(collectionName)).firstOrNull()
            MediaType.VIDEO -> videoTagsRepository.getTagsByName(listOf(collectionName)).firstOrNull()
        }
    }

    private suspend fun deleteMediaMatchingTag(mediaType: MediaType, mediaIds: List<Long>, tagId: Long){
        when (mediaType) {
            MediaType.IMAGE -> imageTagsCrossRefRepository.deleteMediaMatchTag(mediaIds, tagId)
            MediaType.VIDEO -> videoTagsCrossRefRepository.deleteMediaMatchTag(mediaIds, tagId)
        }
    }
    private fun mediaIdToUri(id: Long, mediaType: MediaType): Uri {
        return when (mediaType) {
            MediaType.IMAGE -> getImageUriFromId(id)
            MediaType.VIDEO -> getVideoUriFromId(id)
        }
    }

    private fun uriToMediaId(uri: Uri): Long {
        return ContentUris.parseId(uri)
    }
}
