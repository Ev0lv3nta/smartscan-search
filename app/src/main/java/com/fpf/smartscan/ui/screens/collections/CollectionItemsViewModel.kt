package com.fpf.smartscan.ui.screens.collections

import android.app.Application
import android.content.ContentUris
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fpf.smartscan.data.MediaTag
import com.fpf.smartscan.data.images.ImageDatabase
import com.fpf.smartscan.data.images.tags.ImageTagCrossRefRepository
import com.fpf.smartscan.data.images.tags.ImageTagRepository
import com.fpf.smartscan.data.videos.VideoDatabase
import com.fpf.smartscan.data.videos.tags.VideoTagCrossRefRepository
import com.fpf.smartscan.data.videos.tags.VideoTagRepository
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.media.getImageUriFromId
import com.fpf.smartscan.media.getVideoUriFromId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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
    val mediaItems: StateFlow<List<Uri>> = _state.flatMapLatest { state ->
                val id = getTag(state.mediaType, state.collectionName)?.id

                if (id == null) {
                    flowOf(emptyList())
                } else {
                    combine(
                        imageTagsCrossRefRepository.getMediaIdsFlow(id),
                        videoTagsCrossRefRepository.getMediaIdsFlow(id),
                        flowOf(state.mediaType)
                    ) { imageIds, videoIds, mediaType ->
                        when (mediaType) {
                            MediaType.IMAGE -> imageIds.map { mediaIdToUri(it, mediaType) }
                            MediaType.VIDEO -> videoIds.map { mediaIdToUri(it, mediaType) }
                        }
                    }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = emptyList()
            )


    fun deleteItems(mediaType: MediaType, mediaIds: List<Long>){
        viewModelScope.launch (Dispatchers.IO){
            when (mediaType) {
                MediaType.IMAGE -> imageTagsCrossRefRepository.deleteByMediaIds(mediaIds)
                MediaType.VIDEO -> videoTagsCrossRefRepository.deleteByMediaIds(mediaIds)
            }
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

    private suspend fun getTag(mediaType: MediaType, collectionName: String?): MediaTag?{
        collectionName?: return null
        return when (mediaType) {
            MediaType.IMAGE -> imageTagsRepository.getTagsByName(listOf(collectionName)).firstOrNull()
            MediaType.VIDEO -> videoTagsRepository.getTagsByName(listOf(collectionName)).firstOrNull()
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
