package com.fpf.smartscan.ui.screens.search

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImagePainter
import kotlinx.coroutines.Dispatchers
import com.fpf.smartscan.R
import com.fpf.smartscan.data.clusters.ClusterCrossRefRepository
import com.fpf.smartscan.data.clusters.ClusterMetadataRepository
import com.fpf.smartscan.data.metadata.MediaMetadataRepository
import com.fpf.smartscan.data.tags.TagCrossRefRepository
import com.fpf.smartscan.data.tags.TagRepository
import com.fpf.smartscan.data.tags.Tag
import com.fpf.smartscan.media.MediaItem
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.media.filterAccessibleMediaStoreIds
import com.fpf.smartscan.search.QueryType
import com.fpf.smartscan.utils.canOpenUri
import com.fpf.smartscan.media.onMediaLoadingError
import com.fpf.smartscan.media.openImageInGallery
import com.fpf.smartscan.media.openVideoInGallery
import com.fpf.smartscan.media.removeStaleMedia
import com.fpf.smartscan.media.toMediaItem
import com.fpf.smartscan.index.ImageIndexListener
import com.fpf.smartscan.search.SearchQuery
import com.fpf.smartscan.tag.TagManager
import com.fpf.smartscan.index.VideoIndexListener
import com.fpf.smartscan.search.dedupe
import com.fpf.smartscan.search.getPaginatedResult
import com.fpf.smartscan.search.parseQuery
import com.fpf.smartscan.services.rebuildIndex
import com.fpf.smartscan.services.refreshIndex
import com.fpf.smartscan.services.startIndexing
import com.fpf.smartscan.ui.permissions.StorageAccess
import com.fpf.smartscan.ui.permissions.getStorageAccess
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import com.fpf.smartscansdk.core.media.getBitmapFromUri
import com.fpf.smartscansdk.ml.models.ModelAssetSource
import com.fpf.smartscansdk.ml.providers.embeddings.clip.ClipImageEmbedder
import com.fpf.smartscansdk.ml.providers.embeddings.clip.ClipImageEmbedder.Companion.IMAGE_SIZE_X
import com.fpf.smartscansdk.ml.providers.embeddings.clip.ClipTextEmbedder
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicBoolean


class SearchViewModel(
    application: Application,
    private val imageStore: FileEmbeddingStore,
    private val videoStore: FileEmbeddingStore,
    private val tagRepository: TagRepository,
    private val tagCrossRefRepository: TagCrossRefRepository,
    private val clusterCrossRefRepository: ClusterCrossRefRepository,
    private val clusterMetadataRepository: ClusterMetadataRepository,
    private val mediaMetadataRepository: MediaMetadataRepository
) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "SearchViewModel"
        const val RESULTS_BATCH_SIZE = 36
        private const val MODEL_SHUTDOWN_DURATION_THRESHOLD = 60_000L
    }

    val imageIndexProgress = ImageIndexListener.progress
    val imageIndexStatus = ImageIndexListener.indexingStatus
    val videoIndexProgress = VideoIndexListener.progress
    val videoIndexStatus = VideoIndexListener.indexingStatus

    private val textEmbedder  = ClipTextEmbedder(application, ModelAssetSource.Resource(R.raw.clip_text_encoder_quant), vocabSource = ModelAssetSource.Resource(R.raw.vocab), mergesSource = ModelAssetSource.Resource(R.raw.merges))

    private val imageEmbedder = ClipImageEmbedder(application, ModelAssetSource.Resource(R.raw.clip_image_encoder_quant))

    val tagManager = TagManager(
        tagRepository=tagRepository,
        tagCrossRefRepository=tagCrossRefRepository,
        mediaMetadataRepository = mediaMetadataRepository,
        )
    val allTags: StateFlow<List<Tag>> = tagRepository.allTags.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state
    val searchFieldState: TextFieldState = TextFieldState()
    private val _hasRefreshedImageIndex = MutableStateFlow(false)
    private val _hasRefreshedVideoIndex = MutableStateFlow(false)
    private val _hasShownImageIndexAlert = MutableStateFlow(false)
    private val _hasShownVideoIndexAlert = MutableStateFlow(false)
    private val _alertTitle = MutableStateFlow<String?>(null)
    val alertTitle: StateFlow<String?> = _alertTitle
    private val _alertDescription = MutableStateFlow<String?>(null)
    val alertDescription: StateFlow<String?> = _alertDescription
    private val isLoadingMoreSearchResults = AtomicBoolean(false)

    private var hasHandledExternalSearch = false

    private var cachedIds= mutableListOf<Long>()


    init {
        loadImageIndex()
        viewModelScope.launch{
            processQuery()
        }
    }

    private fun loadImageIndex(){
        loadIndex(imageStore)
    }

    private fun loadVideoIndex(){
        loadIndex(videoStore)
    }

    private fun loadIndex(store: FileEmbeddingStore){
        viewModelScope.launch(Dispatchers.IO){
            try {
                _state.emit(_state.value.copy(error = null, loading = true))
                val embeddings = store.get()
                val hasIndexed = embeddings.isNotEmpty()
                when(_state.value.mediaType){
                    MediaType.VIDEO -> _state.emit(_state.value.copy(hasIndexedVideos = hasIndexed))
                    MediaType.IMAGE -> _state.emit(_state.value.copy(hasIndexedImages = hasIndexed))
                }
            }catch (e: Exception){
                _state.emit(_state.value.copy(error = getApplication<Application>().getString(R.string.search_error_index_loading)))
                Log.e(TAG, "Error loading index: $e")
            }finally {
                _state.emit(_state.value.copy(loading = false))
            }
        }
    }

    fun reloadIndex(mode : MediaType){
        if(mode == MediaType.IMAGE && !_hasRefreshedImageIndex.value){
            loadImageIndex()
            setIsRescanning(false)
            _hasRefreshedImageIndex.value = true
        }else if (mode == MediaType.VIDEO && !_hasRefreshedVideoIndex.value){
            loadVideoIndex()
            setIsRescanning(false)
            _hasRefreshedVideoIndex.value = true
        }
    }

    fun setIsRescanning(isRescanning: Boolean){
        _state.update { it.copy(isRescanning = isRescanning) }
    }

    fun setMediaType(type: MediaType) {
        _state.update { it.copy(mediaType = type) }
        reset()

        // saves memory by lazy loading video index
        if(type == MediaType.VIDEO && _state.value.hasIndexedVideos == null){
            viewModelScope.launch(Dispatchers.IO){loadVideoIndex()}
        }
    }

    fun reset(){
        cachedIds = mutableListOf() // clear on new search
        _state.update{ it.copy(
            totalResults = 0,
            searchResults = emptyList(),
            selectedResults = emptySet(),
            autoCompleteTagResults = emptyList(),
            error = null,
            tagFilter = null,
            tagOnlySearch = false
        ) }
    }

    fun search(threshold: Float, dedupeEnabled: Boolean, duplicateThreshold: Float = 0.95f){
        reset()
        val store = getStore()
        if(!store.exists) {
            _state.update{ currentState -> currentState.copy(error = getApplication<Application>().getString(R.string.search_error_not_indexed))}
            return
        }
        _state.update { it.copy(loading = true) }

        viewModelScope.launch(Dispatchers.Default) {
            try {
                val state = _state.value
                val queryResults = when (_state.value.queryType) {
                    QueryType.IMAGE -> {
                        val result = imageSearch(store, threshold, startDate = state.startDateFilter, endDate = state.endDateFilter)
                        _state.update{it.copy(imageEmbedderLastUsage = System.currentTimeMillis())}
                        result

                    }

                    QueryType.TEXT -> {
                        val result = textSearch(store, threshold, startDate = state.startDateFilter, endDate = state.endDateFilter)
                        _state.update{it.copy(textEmbedderLastUsage = System.currentTimeMillis())}
                        result
                    }
                }
                handleSearchResult(queryResults, store, dedupeEnabled, duplicateThreshold)
            }catch (e: Exception) {
                Log.e(TAG, "$e")
                _state.update{it.copy(error = getApplication<Application>().getString(R.string.search_error_unknown))}
            } finally {
                _state.update{it.copy(loading = false)}
            }
        }
    }

    private suspend fun textSearch(store: FileEmbeddingStore, threshold: Float, startDate: Long? = null, endDate: Long? = null): List<Long> {
        val query = searchFieldState.text.toString()
        if (query.isBlank()) {
            _state.update{currentState -> currentState.copy(error = getApplication<Application>().getString(R.string.search_error_empty_query))}
            return emptyList()
        }
        val (tag, actualQuery) = parseQuery(query)
        tag?.let{ tag ->
            _state.update { currentState -> currentState.copy(tagFilter = tag) }
            tagManager.updateLastUsage(tag)
        }
        val idsMatchingTag: List<Long> = tagManager.getMediaMatchingTag(tag, _state.value.mediaType)
        val tagOnlySearch = idsMatchingTag.isNotEmpty() && actualQuery.isBlank()

        if(tagOnlySearch){
            _state.update { currentState -> currentState.copy(tagOnlySearch = true) }
            return idsMatchingTag
        }
        if(actualQuery.isBlank()){
            return emptyList()
        }

        if(!textEmbedder.isInitialized())textEmbedder.initialize()

        val embedding = textEmbedder.embed(actualQuery)
        val filterIds = idsMatchingTag.toSet()
        val queryResults = store.query(embedding, Int.MAX_VALUE, threshold, filterIds,  startDate = startDate, endDate = endDate)
        // prevent keeping both models open
        if(shouldShutdownModel(_state.value.imageEmbedderLastUsage)) imageEmbedder.closeSession()
        return queryResults
    }

    private suspend fun imageSearch(store: FileEmbeddingStore, threshold: Float, startDate: Long? = null, endDate: Long? = null): List<Long> {
        val queryImage = _state.value.queryImage?: return  emptyList()

        if(!imageEmbedder.isInitialized()) imageEmbedder.initialize()

        val bitmap = getBitmapFromUri(getApplication(), queryImage, IMAGE_SIZE_X)
        val embedding = imageEmbedder.embed(bitmap)
        val queryResults = store.query(embedding, Int.MAX_VALUE, threshold, startDate = startDate, endDate = endDate)

        // prevent keeping both models open
        if(shouldShutdownModel(_state.value.textEmbedderLastUsage)) textEmbedder.closeSession()
        return queryResults
    }

    private suspend fun handleSearchResult(queryResults: List<Long>, store: FileEmbeddingStore, dedupeEnabled: Boolean = false, duplicateThreshold: Float = 0.95f) {
        val finalResults =  if (dedupeEnabled) dedupe(store, queryResults, duplicateThreshold) else queryResults
        cachedIds.addAll(finalResults)
        val totalCount = finalResults.size
        val initialBatch = finalResults.take(RESULTS_BATCH_SIZE) // initial results the rest loaded dynamically
        val (validIds, idsToPurge) = filterAccessibleMediaStoreIds(getApplication(), initialBatch, _state.value.mediaType)
        val filteredSearchResults = validIds.map { toMediaItem(it, _state.value.mediaType) }

        _state.emit( _state.value.copy(totalResults = totalCount - idsToPurge.size, searchResults = filteredSearchResults))

        if (filteredSearchResults.isEmpty()) {
            _state.emit(_state.value.copy(error = getApplication<Application>().getString(R.string.search_error_no_results)))
        }

        if(idsToPurge.isNotEmpty()){
            cachedIds.removeAll(idsToPurge) // PREVENTS duplicates
            purgeStaleItems(store, idsToPurge)
        }
    }

    fun externalSearch(intentSearchQuery: SearchQuery?, similarityThreshold: Float, imageSimilarityThreshold: Float, dedupeEnabled: Boolean, duplicateThreshold: Float = 0.95f){
        if(intentSearchQuery == null || hasHandledExternalSearch) return

        when(intentSearchQuery) {
            is SearchQuery.ImageQuery -> {
                setMediaType(intentSearchQuery.mediaType)
                updateSearchImageUri(intentSearchQuery.uri)
                updateQueryType(QueryType.IMAGE)
                search(imageSimilarityThreshold, dedupeEnabled, duplicateThreshold)
                hasHandledExternalSearch = true
            }

            is SearchQuery.TextQuery -> {
                setMediaType(intentSearchQuery.mediaType)
                searchFieldState.edit { replace(0, searchFieldState.text.length, intentSearchQuery.text) }
                search( similarityThreshold, dedupeEnabled, duplicateThreshold)
                hasHandledExternalSearch = true
            }
        }
    }

    fun onLoadMore() {
        if (isLoadingMoreSearchResults.getAndSet(true)) return
        val store = getStore()
        val currentItemsCount = _state.value.searchResults.size
        if (currentItemsCount >= _state.value.totalResults) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val batch = getPaginatedResult(currentItemsCount, RESULTS_BATCH_SIZE, cachedIds)
                val (filteredResults, idsToPurge) = filterAccessibleMediaStoreIds(getApplication(), batch, _state.value.mediaType)

                if (filteredResults.isNotEmpty()) {
                    val filteredSearchResults = _state.value.searchResults + filteredResults.map { toMediaItem(it, _state.value.mediaType) }
                    _state.emit(_state.value.copy(searchResults = filteredSearchResults))
                }

                if (idsToPurge.isNotEmpty()) {
                    cachedIds.removeAll(idsToPurge) // PREVENTS duplicates
                    purgeStaleItems(store, idsToPurge)
                }
            }finally {
                isLoadingMoreSearchResults.set(false)
            }
        }
    }

    private fun purgeStaleItems(store: FileEmbeddingStore, idsToPurge: List<Long>){
        viewModelScope.launch(Dispatchers.IO) {
            removeStaleMedia(idsToPurge, store, mediaMetadataRepository)
        }
    }

    fun toggleViewResult(context: Context, item: MediaItem?, autoOpenInGallery: Boolean? = null, isSelecting: Boolean = false){
        if(item != null && !canOpenUri(context, item.uri)){
            _state.update { currentState -> currentState.copy(searchResults = currentState.searchResults - item) }
            return
        }

        if(autoOpenInGallery == true && !isSelecting) {
            when(_state.value.mediaType){
                MediaType.IMAGE -> {
                    item?.let{openImageInGallery(context, it.uri)}
                }
                MediaType.VIDEO -> {
                    item?.let{openVideoInGallery(context, it.uri)}
                }
            }
        }else{
            _state.value = _state.value.copy(resultToView = item)
        }
    }


    fun refreshMediaIndex(mediaType: MediaType){
        val storageAccess = getStorageAccess(getApplication())
        if (storageAccess != StorageAccess.Denied) {
            refreshIndex(getApplication(), listOf(mediaType))
        }
    }

    fun rebuildMediaIndex(mediaType: MediaType){
        val storageAccess = getStorageAccess(getApplication())
        if (storageAccess != StorageAccess.Denied) {
            setIsRescanning(true)
            val store = getStore()
            viewModelScope.launch {
                rebuildIndex(getApplication(), listOf(mediaType to store), clusterCrossRefRepository, clusterMetadataRepository)
            }
        }
    }


    fun showIndexAlert(){
        val hasShown = if (_state.value.mediaType == MediaType.IMAGE) _hasShownImageIndexAlert else _hasShownVideoIndexAlert
        if (hasShown.value) return
        when(_state.value.mediaType){
            MediaType.IMAGE -> {
                _alertTitle.value = getApplication<Application>().getString(R.string.search_start_indexing_alert, "images")
                _alertDescription.value = getApplication<Application>().getString(R.string.first_indexing, "image")
            }
            MediaType.VIDEO -> {
                _alertTitle.value = getApplication<Application>().getString(R.string.search_start_indexing_alert, "videos")
                _alertDescription.value = getApplication<Application>().getString(R.string.first_indexing, "video")
            }
        }
        hasShown.value = true
    }

    fun clearIndexAlert(){
        _alertTitle.value = null
        _alertDescription.value = null
    }

    fun updateQueryType(type: QueryType){
        _state.value = _state.value.copy(queryType =type)
    }

    fun updateSearchImageUri(uri: Uri?){
        _state.value = _state.value.copy(queryImage =uri)
    }

    fun setStartDateFilter(date: Long?){
        _state.update {it.copy(startDateFilter = date)}

    }

    fun setEndDateFilter(date: Long?){
        _state.update {it.copy(endDateFilter = date)}

    }

    fun clearDateFilters(){
        _state.update {it.copy(endDateFilter = null, startDateFilter = null)}
    }

    private fun shouldShutdownModel(lastUsage: Long?) = lastUsage != null && System.currentTimeMillis() - lastUsage >= MODEL_SHUTDOWN_DURATION_THRESHOLD

    fun onIndex(){
        when(_state.value.mediaType){
            MediaType.IMAGE -> startIndexing(getApplication(), listOf(MediaType.IMAGE))
            MediaType.VIDEO -> startIndexing(getApplication(), listOf(MediaType.VIDEO))
        }
    }

    fun toggleSelectedResult(item: MediaItem){
        _state.update { currentState ->
            if (item in currentState.selectedResults) {
                val updatedSelectedResults = currentState.selectedResults - item
                currentState.copy(selectedResults = updatedSelectedResults)
            } else {
                val updatedSelectedResults = currentState.selectedResults + item
                currentState.copy(selectedResults = updatedSelectedResults)
            }
        }
    }

    fun clearSelectedResults(){
        _state.update{currentState -> currentState.copy(selectedResults = emptySet())}
    }

    fun tagSelectedItems(tag: String){
        viewModelScope.launch(Dispatchers.IO) {
            try {
              tagManager.tagItems(tag, _state.value.selectedResults)
            }finally {
                clearSelectedResults()
            }
        }
    }

    fun updateAutoCompleteResults(results: List<String>){
        _state.update{currentState -> currentState.copy(autoCompleteTagResults = results)}
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

    fun handleAutoCompletionCheck(query: CharSequence, substringEnd: Int, startWithHashtag: Boolean =  true){
        val results = tagManager.checkAutoCompletion(query, substringEnd, allTags.value, startWithHashtag)
        updateAutoCompleteResults(results)
    }

    @OptIn(FlowPreview::class)
    suspend fun processQuery() {
        snapshotFlow { searchFieldState.text }
            .debounce(50)
            .collectLatest { query: CharSequence ->
                val subStringEnd = searchFieldState.selection.end
                handleAutoCompletionCheck(query, subStringEnd)
            }
    }

    fun onSelectAutoCompleteResult(tag: String){
        searchFieldState.edit { replace(0, searchFieldState.text.length, "#$tag ") }
    }
    private fun getStore() = if(_state.value.mediaType == MediaType.VIDEO) videoStore else imageStore

    override fun onCleared() {
        textEmbedder.closeSession()
        imageEmbedder.closeSession()
        super.onCleared()
    }
}
