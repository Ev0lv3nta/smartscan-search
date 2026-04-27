package com.fpf.smartscan.ui.screens.collections

import android.app.Application
import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import coil3.compose.AsyncImagePainter
import com.fpf.smartscan.constants.EmbeddingStoresFiles
import com.fpf.smartscan.media.MediaCollection
import com.fpf.smartscan.data.MediaDatabase
import com.fpf.smartscan.data.tags.TagPagingSource
import com.fpf.smartscan.data.clusters.ClusterCrossRefRepository
import com.fpf.smartscan.data.clusters.ClusterMetadataRepository
import com.fpf.smartscan.data.clusters.ClusterPagingSource
import com.fpf.smartscan.data.metadata.MediaMetadataRepository
import com.fpf.smartscan.data.tags.Tag
import com.fpf.smartscan.data.tags.TagCrossRef
import com.fpf.smartscan.data.tags.TagWithCount
import com.fpf.smartscan.data.tags.TagCrossRefRepository
import com.fpf.smartscan.data.tags.TagRepository
import com.fpf.smartscan.media.MediaItem
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.media.getImageUriFromId
import com.fpf.smartscan.media.getVideoUriFromId
import com.fpf.smartscan.media.openImageInGallery
import com.fpf.smartscan.media.openVideoInGallery
import com.fpf.smartscan.media.onMediaLoadingError
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
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
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.collections.plus


class CollectionItemsViewModel(
    application: Application,
    private val imageStore: FileEmbeddingStore,
    private val videoStore: FileEmbeddingStore,
) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "CollectionItemsViewModel"
    }


    private val db = MediaDatabase.getDatabase(application)

    private val tagsRepository by lazy { TagRepository(db.tagDao())}
    private val tagsCrossRefRepository by lazy { TagCrossRefRepository( db.tagCrossRefDao())}
    private val clusterCrossRefRepository by lazy { ClusterCrossRefRepository(db.clusterCrossRefDao()) }

    private val mediaMetadataRepository by lazy { MediaMetadataRepository( db.metadataDao())}


    private val _state = MutableStateFlow(CollectionItemsState())
    val state: StateFlow<CollectionItemsState> = _state

    @OptIn(ExperimentalCoroutinesApi::class)
    val tagItems = _state
        .map { it.mediaType to it.collectionName }
        .distinctUntilChanged()
        .flatMapLatest { (mediaType, collectionName) ->

            val tagId = collectionName?.let{tagsRepository.getTagsByName(listOf(it)).firstOrNull()?.id}

            if (tagId == null) {
                flowOf(PagingData.empty())
            } else {
                Pager(
                    config = PagingConfig(
                        pageSize = 50,
                        initialLoadSize = 50,
                        prefetchDistance = 25,
                        enablePlaceholders = false
                    ),
                    pagingSourceFactory = {
                        TagPagingSource(
                            mediaType = mediaType,
                            tagId = tagId,
                            mediaMetadataRepository = mediaMetadataRepository,
                            mediaIdToUri = ::mediaIdToUri
                        )
                    }
                ).flow
            }
        }
        .cachedIn(viewModelScope)

    @OptIn(ExperimentalCoroutinesApi::class)
    val clusterItems = _state
        .map { Triple(it.mediaType, it.collectionName, it.clusterId) }
        .distinctUntilChanged()
        .flatMapLatest { (mediaType, collectionName, clusterId) ->
            if (clusterId == -1L) {
                flowOf(PagingData.empty())
            } else {
                Pager(
                    config = PagingConfig(
                        pageSize = 50,
                        initialLoadSize = 50,
                        prefetchDistance = 25,
                        enablePlaceholders = false
                    ),
                    pagingSourceFactory = {
                        ClusterPagingSource(
                            mediaType = mediaType,
                            clusterId = clusterId,
                            mediaMetadataRepository = mediaMetadataRepository,
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
    ) { tagCounts, mediaType -> getCollections(tagCounts)
    }.flowOn(Dispatchers.IO).stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )

    fun removeItems(items: List<MediaItem>, onComplete: () -> Unit){
        val mediaIds = items.map{it.id}
        val collectionName = _state.value.collectionName?: return

        viewModelScope.launch (Dispatchers.IO){
            val tag = tagsRepository.getTagsByName(listOf(collectionName)).firstOrNull()?: return@launch
            tagsCrossRefRepository.deleteMediaMatchTag( mediaIds, tag.id)
            onComplete()
            _state.update { it.copy(selectedMediaItems = emptySet()) }
        }
    }

    fun moveItems(items: Set<MediaItem>, newCollection: MediaCollection, onComplete: () -> Unit){
        val oldCollectionName = _state.value.collectionName?: return

        viewModelScope.launch (Dispatchers.IO){
            val newTag = tagsRepository.getTagsByName(listOf(newCollection.name)).firstOrNull()?: return@launch
            val updatedCrossRef = items.map{ TagCrossRef(mediaId = it.id, tagId=newTag.id, type=it.type)}
            tagsCrossRefRepository.upsertTagCrossRefs(updatedCrossRef)

            val oldTag = tagsRepository.getTagsByName(listOf(oldCollectionName)).firstOrNull()?: return@launch
            tagsCrossRefRepository.deleteMediaMatchTag(  items.map{it.id}, oldTag.id)
            onComplete()
            _state.update { it.copy(selectedMediaItems = emptySet()) }
        }
    }

    fun createNewCollectionAndMove(items: Set<MediaItem>, newCollectionName: String, onComplete: () -> Unit){
        val oldCollectionName = _state.value.collectionName?: return
        if(oldCollectionName == newCollectionName) return

        viewModelScope.launch (Dispatchers.IO) {
            try {
                val newTagId = tagsRepository.insertTags(listOf(Tag(name = newCollectionName))).firstOrNull()?: return@launch
                val updatedCrossRef = items.map{ TagCrossRef(mediaId = it.id, tagId=newTagId, type=it.type)}
                tagsCrossRefRepository.upsertTagCrossRefs(updatedCrossRef)

                val oldTag = tagsRepository.getTagsByName(listOf(oldCollectionName)).firstOrNull()?: return@launch
                tagsCrossRefRepository.deleteMediaMatchTag(  items.map{it.id}, oldTag.id)
                onComplete()
                _state.update { it.copy(selectedMediaItems = emptySet()) }
            }catch (_: SQLiteConstraintException){
                _state.update { it.copy(error="Collection already exists", selectedMediaItems = emptySet()) }
            }
        }
    }


    fun toggleSelectedItem(item: MediaItem){
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

    fun resetErrorState(){
        _state.update { it.copy(error=null) }
    }

    fun clearSelectedItems(){
        _state.update{currentState -> currentState.copy(selectedMediaItems = emptySet())}
    }

    fun setCollection(name: String?, clusterId: Long){
        _state.update { it.copy(collectionName=name, clusterId = clusterId) }
    }

    fun setMediaToView(context: Context, item: MediaItem?, autoOpenInGallery: Boolean? = null, isSelecting: Boolean = false){
        if(autoOpenInGallery == true && !isSelecting) {
            when(item?.type){
                MediaType.IMAGE -> openImageInGallery(context, item.uri)
                MediaType.VIDEO -> openVideoInGallery(context, item.uri)
                else -> {}
            }
        }else{
            _state.update { it.copy(mediaToView =item) }
        }
    }

    fun onErrorAsyncImage(error: AsyncImagePainter.State.Error){
        viewModelScope.launch (Dispatchers.IO){
            onMediaLoadingError(error,
                imageEmbedStore = imageStore,
                videoEmbedStore = videoStore,
                tagsCrossRefRepository = tagsCrossRefRepository,
                clusterCrossRefRepository=clusterCrossRefRepository
                )
        }
    }

    private suspend fun getCollections(tags: List<TagWithCount>): List<MediaCollection> {
        return tags.mapNotNull {
            val crossRef = mediaMetadataRepository.getByTag(it.id, limit = 1, offset = 0).firstOrNull()
            val uri = crossRef?.let { crossRef -> mediaIdToUri(crossRef.id, crossRef.type) }
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

    private fun mediaIdToUri(id: Long, mediaType: MediaType): Uri {
        return when (mediaType) {
            MediaType.IMAGE -> getImageUriFromId(id)
            MediaType.VIDEO -> getVideoUriFromId(id)
        }
    }

    override fun onCleared() {
        // required now that remove() method only removes from in-memory cache not disk
        runBlocking {
            imageStore.save()
            videoStore.save()
        }
        super.onCleared()
    }
}
