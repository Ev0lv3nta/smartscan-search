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
import com.fpf.smartscan.media.getImageUriFromId
import kotlinx.coroutines.Dispatchers
import com.fpf.smartscan.R
import com.fpf.smartscan.data.clusters.ClusterCrossRefRepository
import com.fpf.smartscan.data.clusters.ClusterMetadataRepository
import com.fpf.smartscan.data.metadata.MediaMetadataRepository
import com.fpf.smartscan.data.tags.TagCrossRef
import com.fpf.smartscan.data.tags.TagCrossRefRepository
import com.fpf.smartscan.data.tags.TagRepository
import com.fpf.smartscan.data.tags.Tag
import com.fpf.smartscan.media.MediaItem
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.media.filterAccessibleMediaStoreIds
import com.fpf.smartscan.search.QueryType
import com.fpf.smartscan.utils.canOpenUri
import com.fpf.smartscan.media.getVideoUriFromId
import com.fpf.smartscan.media.onMediaLoadingError
import com.fpf.smartscan.media.openImageInGallery
import com.fpf.smartscan.media.openVideoInGallery
import com.fpf.smartscan.media.removeStaleMedia
import com.fpf.smartscan.search.ImageIndexListener
import com.fpf.smartscan.search.SearchQuery
import com.fpf.smartscan.search.VideoIndexListener
import com.fpf.smartscan.services.startIndexing
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
import kotlin.math.log2
import kotlin.math.pow

class SearchViewModel(
    application: Application,
    private val imageStore: FileEmbeddingStore,
    private val videoStore: FileEmbeddingStore,
    private val imageClusterStore: FileEmbeddingStore,
    private val videoClusterStore: FileEmbeddingStore,
    private val tagRepository: TagRepository,
    private val tagCrossRefRepository: TagCrossRefRepository,
    private val clusterCrossRefRepository: ClusterCrossRefRepository,
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

                val embeddings = if(store.exists) store.get() else emptyList()
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

    fun refreshIndex(mode : MediaType){
        if(mode == MediaType.IMAGE && !_hasRefreshedImageIndex.value){
            loadImageIndex()
            _hasRefreshedImageIndex.value = true
        }else if (mode == MediaType.VIDEO && !_hasRefreshedVideoIndex.value){
            loadVideoIndex()
            _hasRefreshedVideoIndex.value = true
        }
    }

    fun setMediaType(type: MediaType) {
        _state.value = _state.value.copy(mediaType = type)
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

    fun search(threshold: Float, useClusterSearch: Boolean){
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
                        val result = imageSearch(store, threshold, useClusterSearch, startDate = state.startDateFilter, endDate = state.endDateFilter)
                        _state.update{it.copy(imageEmbedderLastUsage = System.currentTimeMillis())}
                        result

                    }

                    QueryType.TEXT -> {
                        val result = textSearch(store, threshold, useClusterSearch, startDate = state.startDateFilter, endDate = state.endDateFilter)
                        _state.update{it.copy(textEmbedderLastUsage = System.currentTimeMillis())}
                        result
                    }
                }
                handleSearchResult(queryResults, store)
            }catch (e: Exception) {
                Log.e(TAG, "$e")
                _state.update{it.copy(error = getApplication<Application>().getString(R.string.search_error_unknown))}
            } finally {
                _state.update{it.copy(loading = false)}
            }
        }
    }

    private suspend fun textSearch(store: FileEmbeddingStore, threshold: Float, useClusterSearch: Boolean, startDate: Long? = null, endDate: Long? = null): List<Long> {
        val query = searchFieldState.text.toString()
        if (query.isBlank()) {
            _state.update{currentState -> currentState.copy(error = getApplication<Application>().getString(R.string.search_error_empty_query))}
            return emptyList()
        }
        val (tag, actualQuery) = parseQuery(query)
        tag?.let{ tag ->
            _state.update { currentState -> currentState.copy(tagFilter = tag) }
            updateTagLastUsage(tag)
        }
        val idsMatchingTag: List<Long> = getMediaMatchingTag(tag, _state.value.mediaType)
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
        val targetClusters = if (useClusterSearch) getTargetClusters(embedding, threshold) else emptyList()
        val idsMatchingTargetClusters = getIdsInTargetClusters(targetClusters)
        val filterIds = if(tag != null) idsMatchingTag.toSet() else idsMatchingTargetClusters
        val queryResults = store.query(embedding, Int.MAX_VALUE, threshold, filterIds,  startDate = startDate, endDate = endDate)

        // prevent keeping both models open
        if(shouldShutdownModel(_state.value.imageEmbedderLastUsage)) imageEmbedder.closeSession()
        return queryResults
    }

    private fun parseQuery(query: String): Pair<String?, String>{
        val regex = Regex("""^#([a-zA-Z0-9_]+)""")
        val match = regex.find(query)
        val tag = match?.groupValues?.get(1)
        val actualQueryStart = if(!tag.isNullOrBlank()) tag.length + 1 else 0
        val actualQuery = query.substring(actualQueryStart).trim()
        return Pair(tag, actualQuery)
    }

    // increases search breadth when cluster fragmentation is high
    private fun computeDynamicTopK(totalItems: Int, singletonCount: Int, min: Int = 3): Int {
        val base = log2(totalItems.toDouble())
        val singletonRatio = singletonCount.toDouble() / totalItems
        return (base * (1.0 + singletonRatio)).toInt().coerceAtLeast(min)
    }

    private fun computeSingletonTopK(baseTopK: Int, singletonCount: Int, totalClusters: Int, sharpness: Double = 3.0): Int {
        if (totalClusters == 0) return baseTopK
        val t = (singletonCount.toDouble() / totalClusters).coerceIn(0.0, 1.0)
        val expansion = 1.0 - (1.0 - t).pow(sharpness)
        val computedTopK = baseTopK + (singletonCount - baseTopK).coerceAtLeast(0) * expansion
        return computedTopK.toInt().coerceAtLeast(baseTopK)
    }
    private suspend fun imageSearch(store: FileEmbeddingStore, threshold: Float, useClusterSearch: Boolean, startDate: Long? = null, endDate: Long? = null): List<Long> {
        val queryImage = _state.value.queryImage?: return  emptyList()

        if(!imageEmbedder.isInitialized()) imageEmbedder.initialize()

        val bitmap = getBitmapFromUri(getApplication(), queryImage, IMAGE_SIZE_X)
        val embedding = imageEmbedder.embed(bitmap)
        val targetClusters = if (useClusterSearch) getTargetClusters(embedding, threshold) else emptyList()
        val idsMatchingTargetClusters = getIdsInTargetClusters(targetClusters)
        val queryResults = store.query(embedding, Int.MAX_VALUE, threshold, idsMatchingTargetClusters, startDate = startDate, endDate = endDate)

        // prevent keeping both models open
        if(shouldShutdownModel(_state.value.textEmbedderLastUsage)) textEmbedder.closeSession()
        return queryResults
    }


    private suspend fun getIdsInTargetClusters(targetClusters: List<Long>): Set<Long>{
        val idsMatchingCluster: Set<Long> = buildSet {
            for (clusterId in targetClusters) {
                val ids = clusterCrossRefRepository.getClusterToMediaIdsMap()[clusterId] ?: continue
                addAll(ids)
            }
        }
        return idsMatchingCluster
    }


    // Singletons are handled separately from the main clusters to prevent singletons dominating topK
    private suspend fun getTargetClusters(queryEmbedding: FloatArray, threshold: Float): List<Long>{
        val (singletonClusters, mainClusters) =  clusterCrossRefRepository.getClusterCounts().entries.partition { it.value == 1 }
        val singletonCount = singletonClusters.size
        val totalClusters =  mainClusters.size + singletonCount

        val baseTopK = computeDynamicTopK(totalClusters, singletonCount)
        val singletonTopK = computeSingletonTopK(baseTopK,totalClusters, singletonCount)

        val store = getClusterStore()
        if(!store.exists) return emptyList()

        val mainResultIds = store.query(queryEmbedding, baseTopK, threshold, ids = mainClusters.map{it.key}.toSet())
        val singletonResultIds = store.query(queryEmbedding, singletonTopK, threshold, ids = singletonClusters.map{it.key}.toSet())
        return mainResultIds + singletonResultIds
    }


    private suspend fun handleSearchResult(queryResults: List<Long>, store: FileEmbeddingStore) {
        cachedIds.addAll(queryResults)
        val totalCount = queryResults.size
        val initialBatch = queryResults.take(RESULTS_BATCH_SIZE) // initial results the rest loaded dynamically
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

    fun externalSearch(intentSearchQuery: SearchQuery?, similarityThreshold: Float, imageSimilarityThreshold: Float, useClusterSearch: Boolean){
        if(intentSearchQuery == null || hasHandledExternalSearch) return

        when(intentSearchQuery) {
            is SearchQuery.ImageQuery -> {
                setMediaType(intentSearchQuery.mediaType)
                updateSearchImageUri(intentSearchQuery.uri)
                updateQueryType(QueryType.IMAGE)
                search(imageSimilarityThreshold, useClusterSearch)
                hasHandledExternalSearch = true
            }

            is SearchQuery.TextQuery -> {
                setMediaType(intentSearchQuery.mediaType)
                searchFieldState.edit { replace(0, searchFieldState.text.length, intentSearchQuery.text) }
                search( similarityThreshold, useClusterSearch)
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
                val batch = getPaginatedResult(currentItemsCount)
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

    private fun getPaginatedResult(currentItemsCount: Int): List<Long>{
        val end = (currentItemsCount + RESULTS_BATCH_SIZE).coerceAtMost(cachedIds.size)
        if (currentItemsCount >= end) return emptyList()
        return cachedIds.subList(currentItemsCount, end)
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
                val selectedMediaIds = _state.value.selectedResults.map { it.id }
                val existing = tagRepository.getTagsByName(listOf(tag)).firstOrNull()
                var id = existing?.id
                if(id == null){
                    id = tagRepository.insertTags(listOf(Tag(name=tag.trim()))).first()
                }
                val tagEntries = selectedMediaIds.map { TagCrossRef(mediaId = it, tagId = id) }
                tagCrossRefRepository.upsertTagCrossRefs(tagEntries)
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
        val text = query.toString()
        val prefix = text.substring(0, substringEnd)

        // Regex: find #tag at the end of prefix
        var pattern =  """^#([a-zA-Z0-9_]*)$"""
        pattern = if(!startWithHashtag )  pattern.replace("#", "") else pattern
        val match = Regex(pattern).find(prefix)

        if (match != null) {
            val partialTag = match.groupValues[1]
            // Track autocomplete only while typing the tag
            updateAutoCompleteResults(allTags.value
                .filter { it.name.startsWith(partialTag, ignoreCase = true) }
                .map { it.name }
            )
        }
        else {
            updateAutoCompleteResults(emptyList())
        }
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
    private fun getClusterStore() = if(_state.value.mediaType == MediaType.VIDEO) videoClusterStore else imageClusterStore


    private suspend fun getMediaMatchingTag(tagName: String?, mediaType: MediaType): List<Long>{
        tagName?: return emptyList()
        val state = _state.value
        val tag = tagRepository.getTagsByName(listOf(tagName)).firstOrNull()
        return if(state.endDateFilter != null || state.startDateFilter != null){
            tag?.let { tag-> mediaMetadataRepository.getByTagTypeAndDateRange(tag.id, mediaType,state.startDateFilter, state.endDateFilter).map{it.id}  }?: emptyList()
        }else{
            tag?.let { tag-> mediaMetadataRepository.getByTagAndType(tag.id, mediaType).map{it.id}  }?: emptyList()
        }
    }
    private suspend fun updateTagLastUsage(tag: String){
        val tag = allTags.value.find { it.name == tag }?: return
        tagRepository.updateTags(listOf(Tag(tag.id, tag.name, System.currentTimeMillis())))
    }

    private fun mediaIdToUri(id: Long, mediaType: MediaType): Uri {
        return when (mediaType) {
            MediaType.IMAGE -> getImageUriFromId(id)
            MediaType.VIDEO -> getVideoUriFromId(id)
        }
    }

    private fun toMediaItem(id: Long, type: MediaType): MediaItem{
        return MediaItem(
            id = id,
            uri = mediaIdToUri(id, type),
            type = type
        )
    }

    override fun onCleared() {
        textEmbedder.closeSession()
        imageEmbedder.closeSession()
        super.onCleared()
    }
}
