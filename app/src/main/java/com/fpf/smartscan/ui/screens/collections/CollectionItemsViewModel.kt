package com.fpf.smartscan.ui.screens.collections

import android.app.Application
import android.content.ClipData
import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import androidx.compose.ui.platform.Clipboard
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import coil3.compose.AsyncImagePainter
import com.fpf.smartscan.cluster.ClusterManager
import com.fpf.smartscan.data.clusters.ClusterCrossRefRepository
import com.fpf.smartscan.data.clusters.ClusterMetadataRepository
import com.fpf.smartscan.media.MediaCollection
import com.fpf.smartscan.data.tags.TagPagingSource
import com.fpf.smartscan.data.clusters.ClusterPagingSource
import com.fpf.smartscan.data.metadata.MediaMetadataRepository
import com.fpf.smartscan.data.tags.TagCrossRefRepository
import com.fpf.smartscan.data.tags.TagRepository
import com.fpf.smartscan.events.MediaEvent
import com.fpf.smartscan.events.MediaEventType
import com.fpf.smartscan.media.MediaItem
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.media.mediaIdToUri
import com.fpf.smartscan.media.openImageInGallery
import com.fpf.smartscan.media.openVideoInGallery
import com.fpf.smartscan.media.onMediaLoadingError
import com.fpf.smartscan.media.shareMediaMulti
import com.fpf.smartscan.tag.TagManager
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.collections.map
import kotlin.collections.plus


class CollectionItemsViewModel(
    application: Application,
    private val imageStore: FileEmbeddingStore,
    private val videoStore: FileEmbeddingStore,
    private val clusterStore: FileEmbeddingStore,
    private val tagRepository: TagRepository,
    private val tagCrossRefRepository: TagCrossRefRepository,
    private val mediaMetadataRepository: MediaMetadataRepository,
    private val clusterMetadataRepository: ClusterMetadataRepository,
    private val clusterCrossRefRepository: ClusterCrossRefRepository,
) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "CollectionItemsViewModel"
        const val INVALID_CLUSTER_ID = -1L
    }

    val tagManager = TagManager(
        tagRepository=tagRepository,
        tagCrossRefRepository=tagCrossRefRepository,
        mediaMetadataRepository = mediaMetadataRepository,
    )

    val clusterManager = ClusterManager(
        clusterStore = clusterStore,
        clusterCrossRefRepository = clusterCrossRefRepository,
        clusterMetadataRepository = clusterMetadataRepository,
        mediaMetadataRepository = mediaMetadataRepository,
    )
    private val _state = MutableStateFlow(CollectionItemsState())
    val state: StateFlow<CollectionItemsState> = _state

    @OptIn(ExperimentalCoroutinesApi::class)
    val tagItems = _state
        .map { it.mediaType to it.collectionName }
        .distinctUntilChanged()
        .flatMapLatest { (mediaType, collectionName) ->

            val tagId = collectionName?.let{tagRepository.getTagsByName(listOf(it)).firstOrNull()?.id}

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
            if (clusterId == INVALID_CLUSTER_ID) {
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

    val tagCollections: StateFlow<List<MediaCollection>> = combine(
        tagCrossRefRepository.getTagsWithCounts(),
        _state.map { it.mediaType }
    ) { tagCounts, mediaType -> tagManager.tagsToCollections(tagCounts)
    }.flowOn(Dispatchers.IO).stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )

    val clusterCollections: StateFlow<List<MediaCollection>> = combine(
        clusterCrossRefRepository.getClustersWithCount(),
        _state.map { it.mediaType }
    ) { clusters, mediaType -> clusterManager.toCollections(clusters)
    }.flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )

    private val _event = MutableSharedFlow<MediaEvent>()
    val event = _event.asSharedFlow()

    fun onAction(action: MediaItemAction){
        when(action){
            is MediaItemAction.CopyMedia -> onCopyItem(action.clipboard, action.context)
            is MediaItemAction.CreateNewTagCollectionAndMove -> createNewCollectionAndMove(action.newName)
            MediaItemAction.RemoveMedia -> removeItems()
            is MediaItemAction.SetMediaToView -> setMediaToView(action.context, action.item, autoOpenInGallery = action.autoOpenInGallery, isSelecting = action.isSelecting)
            is MediaItemAction.ShareMedia -> onShareItems(action.context)
            is MediaItemAction.ToggleSelectedMedia -> toggleSelectedItem(action.item)
            is MediaItemAction.SetCollectionToView -> setCollection(action.name, action.clusterId)
            is MediaItemAction.MoveMedia -> moveItems(action.destinationCollection, action.clusterId)
        }
    }

    fun clearSelectedItems() = _state.update{it.copy(selectedMediaItems = emptySet())}

    private fun removeItems(){
        val state = _state.value
        val mediaIds = state.selectedMediaItems.map{it.id}
        val collectionName = state.collectionName?: return

        viewModelScope.launch (Dispatchers.IO){
            try {
                tagManager.removeItems(collectionName, mediaIds)
                clearSelectedItems()
                _event.emit(MediaEvent(MediaEventType.REMOVE, success = true, message = "Removed ${mediaIds.size} item(s)"))
            }catch (e: Exception){
                Log.e(TAG, "Error removing items: ${e.message}")
                _event.emit(MediaEvent(MediaEventType.REMOVE, success = false, message = "Error removing items"))
            }
        }
    }

    private fun moveItems(newCollection: MediaCollection, oldClusterId: Long? = null){
        val state = _state.value
        val oldCollectionName = state.collectionName?: return
        val items = state.selectedMediaItems
        if (items.isEmpty()) return
        _state.update { it.copy(loading = true) }

        viewModelScope.launch (Dispatchers.IO){
            try {
                if(newCollection.isAutoCollection){
                    val clusterId = oldClusterId?: error("Invalid Cluster ID")
                    clusterManager.moveItems(items.map{it.id}, newCollection.id, clusterId, imageEmbedStore = imageStore, videoEmbedStore = videoStore)
                }else{
                    tagManager.moveItems(items, oldCollectionName, newCollection.name)
                }
                clearSelectedItems()
                _event.emit(MediaEvent(MediaEventType.MOVE, success = true, message = "Moved ${items.size} item(s)"))
            }catch (e: Exception){
                Log.e(TAG, "Error moving items: ${e.message}")
                _event.emit(MediaEvent(MediaEventType.MOVE, success = false, message = "Error moving items"))
            }finally {
                _state.update { it.copy(loading = false) }
            }
        }
    }

    private fun onCopyItem(clipboard: Clipboard, context: Context){
        clipboard.nativeClipboard.setPrimaryClip(ClipData.newUri(context.contentResolver, "smartscan_media", _state.value.selectedMediaItems.first().uri))
        viewModelScope.launch {
            _event.emit(MediaEvent(MediaEventType.COPY, success = true))
            clearSelectedItems()
        }
    }

    private fun onShareItems(context: Context){
        shareMediaMulti(context, _state.value.selectedMediaItems.map{it.uri})
        viewModelScope.launch {
            _event.emit(MediaEvent(MediaEventType.SHARE, success = true))
            clearSelectedItems()
        }
    }

    private fun createNewCollectionAndMove( newCollectionName: String){
        val state = _state.value
        val oldCollectionName = state.collectionName?: return
        if(oldCollectionName == newCollectionName) return

        val items = state.selectedMediaItems
        if (items.isEmpty()) return

        viewModelScope.launch (Dispatchers.IO) {
            try {
                tagManager.createNewTagAndMoveItems(items, oldCollectionName, newCollectionName)
                clearSelectedItems()
                _event.emit(MediaEvent(MediaEventType.MOVE, success = true, message = "Moved ${items.size} item(s)"))
            }catch (_: SQLiteConstraintException){
                _event.emit(MediaEvent(MediaEventType.MOVE, success = false, message = "Collection already exists"))
            }catch (e: Exception){
                Log.e(TAG, "Error moving items: ${e.message}")
                _event.emit(MediaEvent(MediaEventType.MOVE, success = false, message = "Error moving items"))            }
        }
    }


   private fun toggleSelectedItem(item: MediaItem){
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

    private fun setCollection(name: String?, clusterId: Long) = _state.update { it.copy(collectionName=name, clusterId = clusterId) }

    private fun setMediaToView(context: Context, item: MediaItem?, autoOpenInGallery: Boolean? = null, isSelecting: Boolean = false){
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
                mediaMetadataRepository =mediaMetadataRepository
                )
        }
    }
}
