package com.fpf.smartscan.ui.screens.collections

import android.app.Application
import android.content.ContentUris
import android.content.Context
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
import com.fpf.smartscan.media.openImageInGallery
import com.fpf.smartscan.media.openVideoInGallery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
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


    // batched incremental streaming to reduce time-to-first-render.
    @OptIn(ExperimentalCoroutinesApi::class)
    val mediaItems: Flow<List<Uri>> = _state.flatMapLatest { state ->
            val tagId = getTag(state.mediaType, state.collectionName)?.id

            if(tagId == null){
                flowOf(emptyList())
            }else {
                val idsFlow = when (state.mediaType) {
                    MediaType.IMAGE -> imageTagsCrossRefRepository.getMediaIdsFlow(tagId)
                    MediaType.VIDEO -> videoTagsCrossRefRepository.getMediaIdsFlow(tagId)
                }

                idsFlow.flatMapLatest { ids ->
                    uriBatchFlow(ids, state.mediaType, batchSize = 100)
                }
            }
        }


    private fun uriBatchFlow(ids: List<Long>, mediaType: MediaType, batchSize: Int = 100) = flow {
        ids.chunked(batchSize).forEach { chunk ->
            emit(chunk.map { id ->
                mediaIdToUri(id, mediaType)
            })
        }
    }.flowOn(Dispatchers.IO)

    fun removeItems(mediaType: MediaType, mediaUris: List<Uri>){
        val mediaIds = mediaUris.map{uriToMediaId(it)}
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
