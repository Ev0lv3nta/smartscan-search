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
import com.fpf.smartscan.data.MediaDatabase
import com.fpf.smartscan.data.MediaPagingSource
import com.fpf.smartscan.data.tags.TagWithCount
import com.fpf.smartscan.data.tags.TagCrossRefRepository
import com.fpf.smartscan.data.tags.TagRepository
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


    private val db = MediaDatabase.getDatabase(application)

    private val tagsRepository by lazy { TagRepository(db.tagDao())}
    private val tagsCrossRefRepository by lazy { TagCrossRefRepository( db.tagCrossRefDao())}


    private val _state = MutableStateFlow(CollectionItemsState())
    val state: StateFlow<CollectionItemsState> = _state

    @OptIn(ExperimentalCoroutinesApi::class)
    val mediaItems = _state
        .map { it.mediaType to it.collectionName }
        .distinctUntilChanged()
        .flatMapLatest { (mediaType, collectionName) ->

            val tagId = collectionName?.let{tagsRepository.getTagsByName(listOf(it)).firstOrNull()?.id}

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
                            tagsCrossRefRepository = tagsCrossRefRepository,
                            mediaIdToUri = ::mediaIdToUri
                        )
                    }
                ).flow
            }
        }
        .cachedIn(viewModelScope)

    val mediaCollections: StateFlow<List<MediaCollection>> = combine(
        tagsCrossRefRepository.getTagsWithCounts(),
        _state.map { it.mediaType }
    ) { tagCounts, mediaType -> getCollections(tagCounts, mediaType)
    }.flowOn(Dispatchers.IO).stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )

    fun removeItems(mediaUris: List<Uri>){
        val mediaIds = mediaUris.map{uriToMediaId(it)}
        val collectionName = _state.value.collectionName?: return

        viewModelScope.launch (Dispatchers.IO){
            val tag = tagsRepository.getTagsByName(listOf(collectionName)).firstOrNull()?: return@launch
            tagsCrossRefRepository.deleteMediaMatchTag( mediaIds, tag.id)
            _state.update { it.copy(selectedMediaItems = emptyList()) }
        }
    }

    fun moveItems(mediaUris: List<Uri>, newCollection: MediaCollection){
        val mediaIds = mediaUris.map{uriToMediaId(it)}
        val oldCollectionName = _state.value.collectionName?: return

        viewModelScope.launch (Dispatchers.IO){
            val oldTag = tagsRepository.getTagsByName(listOf(oldCollectionName)).firstOrNull()?: return@launch
            val newTag = tagsRepository.getTagsByName(listOf(newCollection.name)).firstOrNull()?: return@launch

            tagsCrossRefRepository.upsertTagCrossRefs(newTag.id, mediaIds)
            tagsCrossRefRepository.deleteMediaMatchTag( mediaIds, oldTag.id)
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
            val id = tagsCrossRefRepository.getMediaIds(it.id, limit = 1, offset = 0).firstOrNull()
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
