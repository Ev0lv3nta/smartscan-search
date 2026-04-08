package com.fpf.smartscan.ui.screens.search

import android.app.Application
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fpf.smartscan.media.getImageUriFromId
import kotlinx.coroutines.Dispatchers
import com.fpf.smartscan.R
import com.fpf.smartscan.constants.EmbeddingStoresFiles
import com.fpf.smartscan.data.images.ImageTag
import com.fpf.smartscan.data.images.ImageTagCrossRef
import com.fpf.smartscan.data.images.ImageTagCrossRefRepository
import com.fpf.smartscan.data.images.ImageTagDatabase
import com.fpf.smartscan.data.images.ImageTagRepository
import com.fpf.smartscan.data.images.clusters.ImageClusterCrossRefRepository
import com.fpf.smartscan.data.images.clusters.ImageClusterDatabase
import com.fpf.smartscan.data.videos.VideoTag
import com.fpf.smartscan.data.videos.VideoTagCrossRef
import com.fpf.smartscan.data.videos.VideoTagCrossRefRepository
import com.fpf.smartscan.data.videos.VideoTagDatabase
import com.fpf.smartscan.data.videos.VideoTagRepository
import com.fpf.smartscan.data.videos.clusters.VideoClusterCrossRefRepository
import com.fpf.smartscan.data.videos.clusters.VideoClusterDatabase
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.search.QueryType
import com.fpf.smartscan.utils.canOpenUri
import com.fpf.smartscan.media.getVideoUriFromId
import com.fpf.smartscan.media.openImageInGallery
import com.fpf.smartscan.media.openVideoInGallery
import com.fpf.smartscan.search.ImageIndexListener
import com.fpf.smartscan.search.AutoTagger
import com.fpf.smartscan.search.SearchQuery
import com.fpf.smartscan.search.TagSuggestionsResult
import com.fpf.smartscan.search.VideoIndexListener
import com.fpf.smartscan.services.MediaIndexForegroundService
import com.fpf.smartscan.services.startIndexing
import com.fpf.smartscan.utils.isWorkScheduled
import com.fpf.smartscan.workers.AutoTagWorker
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import com.fpf.smartscansdk.core.embeddings.generatePrototypeEmbedding
import com.fpf.smartscansdk.core.media.getBitmapFromUri
import com.fpf.smartscansdk.core.models.ModelAssetSource
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
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

data class SearchState(
    val searchResults: List<Uri> = emptyList(),
    val totalResults: Int = 0,
    val mediaType: MediaType = MediaType.IMAGE,
    val queryType: QueryType = QueryType.TEXT,
    val queryImage: Uri? = null,
    val hasIndexedImages: Boolean? = null,
    val hasIndexedVideos: Boolean? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val resultToView: Uri? = null,
    val selectedResults: List<Uri> = emptyList(),
    val imageEmbedderLastUsage: Long? = null,
    val textEmbedderLastUsage: Long? = null,
    val autoCompleteTagResults: List<String> = emptyList(),
    val tagFilter: String? = null,
    val suggestedTags: TagSuggestionsResult = TagSuggestionsResult(),
    val tagOnlySearch: Boolean = false,
    )

class SearchViewModel(private val application: Application) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "SearchViewModel"
        const val RESULTS_BATCH_SIZE = 36
        private const val MODEL_SHUTDOWN_DURATION_THRESHOLD = 60_000L
        private const val MAX_N_PROTOTYPE = 10
    }

    val imageIndexProgress = ImageIndexListener.progress
    val imageIndexStatus = ImageIndexListener.indexingStatus
    val videoIndexProgress = VideoIndexListener.progress
    val videoIndexStatus = VideoIndexListener.indexingStatus

    private val textEmbedder  = ClipTextEmbedder(application, ModelAssetSource.Resource(R.raw.clip_text_encoder_quant), vocabSource = ModelAssetSource.Resource(R.raw.vocab), mergesSource = ModelAssetSource.Resource(R.raw.merges))

    private val imageEmbedder = ClipImageEmbedder(application, ModelAssetSource.Resource(R.raw.clip_image_encoder_quant))


    val imageStore = FileEmbeddingStore(File(application.filesDir, EmbeddingStoresFiles.IMAGE), imageEmbedder.embeddingDim)
    val videoStore = FileEmbeddingStore(File(application.filesDir, EmbeddingStoresFiles.VIDEO), imageEmbedder.embeddingDim )

    val imageClusterStore = FileEmbeddingStore(File(application.filesDir, EmbeddingStoresFiles.IMAGE_CLUSTER), imageEmbedder.embeddingDim)

    val videoClusterStore = FileEmbeddingStore(File(application.filesDir, EmbeddingStoresFiles.VIDEO_CLUSTER), imageEmbedder.embeddingDim )


    val tagStore = FileEmbeddingStore(File(application.filesDir, EmbeddingStoresFiles.TAGS), imageEmbedder.embeddingDim)

    private val imageTagsRepository = ImageTagRepository(ImageTagDatabase.getDatabase(application).tagDao())
    private val videoTagsRepository = VideoTagRepository(VideoTagDatabase.getDatabase(application).tagDao())
    private val imageTagsCrossRefRepository = ImageTagCrossRefRepository( ImageTagDatabase.getDatabase(application).imageTagCrossRefDao(), ImageTagDatabase.getDatabase(application).tagDao())
    private val videoTagsCrossRefRepository = VideoTagCrossRefRepository(VideoTagDatabase.getDatabase(application).videoTagCrossRefDao(), VideoTagDatabase.getDatabase(application).tagDao())
    private val imageClusterCrossRefRepository = ImageClusterCrossRefRepository(ImageClusterDatabase.getDatabase(application).imageClusterCrossRefDao())
    private val videoClusterCrossRefRepository = VideoClusterCrossRefRepository(VideoClusterDatabase.getDatabase(application).videoClusterCrossRefDao())

    val allImageTags: StateFlow<List<ImageTag>> = imageTagsRepository.allTags.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val allVideoTags: StateFlow<List<VideoTag>> = videoTagsRepository.allTags.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val autoTagger = AutoTagger(tagStore)

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


    init {
        loadImageIndex()
        viewModelScope.launch{
            processQuery()
        }
        viewModelScope.launch(Dispatchers.IO){
            if(!isWorkScheduled(context = application, workName = AutoTagWorker.TAG)) scheduleAutoTagging()
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
                _state.emit(_state.value.copy(error = application.getString(R.string.search_error_index_loading)))
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
        _state.value = _state.value.copy(totalResults = 0, searchResults = emptyList(), selectedResults = emptyList(), autoCompleteTagResults = emptyList(), error = null, tagFilter = null, suggestedTags = TagSuggestionsResult(), tagOnlySearch = false)
    }

    fun search(threshold: Float){
        val store = getStore()
        if(!store.exists) {
            _state.update{ currentState -> currentState.copy(error = application.getString(R.string.search_error_not_indexed))}
            return
        }
        when(_state.value.queryType){
            QueryType.IMAGE -> {
                imageSearch(store, threshold)
            }
            QueryType.TEXT -> {
                textSearch(store, threshold)
            }
        }
    }

    private fun textSearch(store: FileEmbeddingStore, threshold: Float) {
        val query = searchFieldState.text.toString()
        if (query.isBlank()) {
            _state.update{currentState -> currentState.copy(error = application.getString(R.string.search_error_empty_query))}
            return
        }
        reset()
        _state.value = _state.value.copy(loading = true)

        viewModelScope.launch((Dispatchers.IO)) {
            try {
                val regex = Regex("""^#([a-zA-Z0-9_]+)""")
                val match = regex.find(query)
                val tag = match?.groupValues?.get(1)
                tag?.let{ tag ->
                    _state.update { currentState -> currentState.copy(tagFilter = tag) }
                    updateTagLastUsage(tag)
                }

                val actualQueryStart = if(!tag.isNullOrBlank()) tag.length + 1 else 0
                val actualQuery = query.substring(actualQueryStart).trim()
                val idsMatchingTag: List<Long> = getMediaIds(tag, RESULTS_BATCH_SIZE, 0) // load initial
                val totalResults = countMediaIds(tag)

                val tagOnlySearch = idsMatchingTag.isNotEmpty() && actualQuery.isBlank()
                if(tagOnlySearch){
                    _state.update { currentState -> currentState.copy(tagOnlySearch = true) }
                    return@launch handleQueryResults(idsMatchingTag, store, totalResults)
                }else if(actualQuery.isBlank()){
                    return@launch handleQueryResults(emptyList(), store)
                }

                if(!textEmbedder.isInitialized())textEmbedder.initialize()
                if(shouldShutdownModel(_state.value.imageEmbedderLastUsage)) imageEmbedder.closeSession() // prevent keeping both models open

                val embedding = textEmbedder.embed(actualQuery)
//                val start = System.currentTimeMillis()
                val targetClusters = getTargetClusters(embedding, threshold, 3)
                val filterIds: Set<Long> = buildSet {
                    for (clusterId in targetClusters) {
                        val ids = getClusterToMediaIdsMap()[clusterId] ?: continue
                        addAll(ids)
                    }
                    addAll(idsMatchingTag)
                }
                val queryResults = store.query(embedding, Int.MAX_VALUE, threshold, filterIds)
//                val timeElapsed = System.currentTimeMillis() - start
//                Log.d(TAG, "time elapsed: $timeElapsed | results: ${queryResults.size}")
                handleQueryResults(queryResults, store)
            } catch (e: Exception) {
                Log.e(TAG, "$e")
                _state.emit(_state.value.copy(error = application.getString(R.string.search_error_unknown)))
            } finally {
                _state.emit(_state.value.copy(loading = false, textEmbedderLastUsage = System.currentTimeMillis()))
            }
        }
    }

    private fun imageSearch(store: FileEmbeddingStore, threshold: Float) {
        val queryImage = _state.value.queryImage?: return

        reset()
        _state.value = _state.value.copy( loading = true)

        viewModelScope.launch((Dispatchers.IO)) {
            try {
                if(!imageEmbedder.isInitialized()) imageEmbedder.initialize()
                if(shouldShutdownModel(_state.value.textEmbedderLastUsage)) textEmbedder.closeSession() // prevent keeping both models open

                val bitmap = getBitmapFromUri(application, queryImage, IMAGE_SIZE_X)
                val embedding = imageEmbedder.embed(bitmap)
                val targetClusters = getTargetClusters(embedding, threshold, 3)
                val mediaIdsInCluster: Set<Long> = buildSet {
                    for (clusterId in targetClusters) {
                        val ids = getClusterToMediaIdsMap()[clusterId] ?: continue
                        addAll(ids)
                    }
                }
                val resultIds = store.query(embedding, Int.MAX_VALUE, threshold, mediaIdsInCluster)
                handleQueryResults(resultIds, store)
            } catch (e: Exception) {
                Log.e(TAG, "$e")
                _state.emit(_state.value.copy(error = application.getString(R.string.search_error_unknown)))
            } finally {
                _state.emit(_state.value.copy(loading = false, imageEmbedderLastUsage = System.currentTimeMillis()))
            }
        }
    }

    private suspend fun getTargetClusters(queryEmbedding: FloatArray, threshold: Float, topK: Int = 1): List<Long>{
        val store = getClusterStore()
        val resultIds = store.query(queryEmbedding, topK, threshold)
        return resultIds
    }

    private suspend fun handleQueryResults(queryResults: List<Long>, store: FileEmbeddingStore, totalResults: Int? = null) {
        val totalCount = totalResults?: queryResults.size
        val initialBatch = queryResults.take(RESULTS_BATCH_SIZE) // initial results the rest loaded dynamically

        val (unprocessedFilteredResults, idsToPurge) = initialBatch.map { id ->
                val uri = if (_state.value.mediaType == MediaType.VIDEO) getVideoUriFromId(id) else getImageUriFromId(id)
                id to uri
        }.partition { (_, uri) -> canOpenUri(application, uri) }

        val filteredSearchResults = unprocessedFilteredResults.map { it.second }

        _state.emit( _state.value.copy(totalResults = totalCount, searchResults = filteredSearchResults))

        if (filteredSearchResults.isEmpty()) {
            _state.emit(_state.value.copy(error = application.getString(R.string.search_error_no_results)))
        }

        if(idsToPurge.isNotEmpty()){
            viewModelScope.launch(Dispatchers.IO) {
                store.remove(idsToPurge.map { it.first })
            }
        }
    }

    fun externalSearch(intentSearchQuery: SearchQuery?, similarityThreshold: Float){
        if(intentSearchQuery == null || hasHandledExternalSearch) return

        when(intentSearchQuery) {
            is SearchQuery.ImageQuery -> {
                setMediaType(intentSearchQuery.mediaType)
                updateSearchImageUri(intentSearchQuery.uri)
                updateQueryType(QueryType.IMAGE)
                search(similarityThreshold)
                hasHandledExternalSearch = true
            }

            is SearchQuery.TextQuery -> {
               setMediaType(intentSearchQuery.mediaType)
                searchFieldState.edit { replace(0, searchFieldState.text.length, intentSearchQuery.text) }
                search( similarityThreshold)
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
                val batch = if(_state.value.tagOnlySearch && _state.value.tagFilter != null){
                    val offset = (currentItemsCount).coerceAtMost(_state.value.totalResults)
                    getMediaIds(_state.value.tagFilter, RESULTS_BATCH_SIZE, offset = offset)
                }else{
                    val end = (currentItemsCount + RESULTS_BATCH_SIZE).coerceAtMost(_state.value.totalResults)
                    store.query(currentItemsCount, end).take(RESULTS_BATCH_SIZE)
                }
                val (filteredResults, idsToPurge) = batch.map { id ->
                        val uri = if (_state.value.mediaType == MediaType.VIDEO) getVideoUriFromId(id) else getImageUriFromId(id)
                        id to uri
                }.partition { (_, uri) -> canOpenUri(application, uri) }

                if (filteredResults.isNotEmpty()) {
                    val filteredSearchResults = _state.value.searchResults + filteredResults.map { it.second }
                    _state.emit(_state.value.copy(searchResults = filteredSearchResults))
                }

                if (idsToPurge.isNotEmpty()) {
                    viewModelScope.launch(Dispatchers.IO) {
                        store.remove(idsToPurge.map { it.first })
                    }
                }
            }finally {
                isLoadingMoreSearchResults.set(false)
            }
        }
    }

    fun toggleViewResult(context: Context, uri: Uri?, autoOpenInGallery: Boolean? = null, isSelecting: Boolean = false){
        if(uri != null && !canOpenUri(context, uri)){
            _state.update { currentState -> currentState.copy(searchResults = currentState.searchResults - uri) }
            return
        }

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
            _state.value = _state.value.copy(resultToView = uri)
        }
    }

    fun showIndexAlert(){
        val hasShown = if (_state.value.mediaType == MediaType.IMAGE) _hasShownImageIndexAlert else _hasShownVideoIndexAlert
        if (hasShown.value) return
        when(_state.value.mediaType){
            MediaType.IMAGE -> {
                _alertTitle.value = application.getString(R.string.search_start_indexing_alert, "images")
                _alertDescription.value = application.getString(R.string.first_indexing, "image")
            }
            MediaType.VIDEO -> {
                _alertTitle.value = application.getString(R.string.search_start_indexing_alert, "videos")
                _alertDescription.value = application.getString(R.string.first_indexing, "video")
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

    private fun shouldShutdownModel(lastUsage: Long?) = lastUsage != null && System.currentTimeMillis() - lastUsage >= MODEL_SHUTDOWN_DURATION_THRESHOLD

    fun onIndex(){
        when(_state.value.mediaType){
            MediaType.IMAGE -> startIndexing(application, MediaIndexForegroundService.TYPE_IMAGE)
            MediaType.VIDEO -> startIndexing(application, MediaIndexForegroundService.TYPE_VIDEO)
        }
    }

    fun toggleSelectedResult(item: Uri){
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
        _state.update{currentState -> currentState.copy(selectedResults = emptyList(), suggestedTags = TagSuggestionsResult())}
    }

    fun addTag(tag: String){
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ids = _state.value.selectedResults.map { ContentUris.parseId(it) }

                when (_state.value.mediaType) {
                    MediaType.IMAGE -> {
                        val tagEntries = ids.map { ImageTagCrossRef(imageId = it, tag = tag.trim()) }
                        imageTagsCrossRefRepository.addTags(tagEntries)
                    }
                    MediaType.VIDEO -> {
                        val tagEntries = ids.map { VideoTagCrossRef(videoId = it, tag = tag.trim()) }
                        videoTagsCrossRefRepository.addTags(tagEntries)
                    }
                }
            }finally {
                clearSelectedResults()
            }
        }
    }

    fun updateAutoCompleteResults(results: List<String>){
        _state.update{currentState -> currentState.copy(autoCompleteTagResults = results)}
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
            when(_state.value.mediaType){
                MediaType.IMAGE -> {
                    updateAutoCompleteResults(allImageTags.value
                        .filter { it.name.startsWith(partialTag, ignoreCase = true) }
                        .map { it.name }
                    )
                }
                MediaType.VIDEO -> {
                    updateAutoCompleteResults(allVideoTags.value
                        .filter { it.name.startsWith(partialTag, ignoreCase = true) }
                        .map { it.name }
                    )
                }
            }
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

    fun scheduleAutoTagging(){
        if (!imageStore.exists && !videoStore.exists) return
        AutoTagWorker.scheduleWorker(application, Pair(1L, TimeUnit.DAYS))
    }

    fun updateSuggestedTags(){
        if(!tagStore.exists) return
        viewModelScope.launch(Dispatchers.IO){
            val ids = _state.value.selectedResults.take(MAX_N_PROTOTYPE).map{ContentUris.parseId(it)}
            _state.update{currentState -> currentState.copy(suggestedTags = TagSuggestionsResult())} // reset

            try {
                when(_state.value.mediaType){
                    MediaType.IMAGE -> {
                        val imageEmbeddings = imageStore.get(ids)
                        val prototype = generatePrototypeEmbedding(imageEmbeddings.map{it.embedding})
                        val suggestedTags = autoTagger.getSuggestedTags(allImageTags.value, prototype)
                        _state.update{currentState -> currentState.copy(suggestedTags = suggestedTags)}

                    }
                    MediaType.VIDEO -> {
                        val videoEmbeddings = videoStore.get(ids)
                        val prototype = generatePrototypeEmbedding(videoEmbeddings.map{it.embedding})
                        val suggestedTags = autoTagger.getSuggestedTags(allVideoTags.value, prototype)
                        _state.update{currentState -> currentState.copy(suggestedTags = suggestedTags)}
                    }
                }
            }catch (e: Exception){
                Log.e(TAG, "$e")
            }
        }
    }

    private fun getStore() = if(_state.value.mediaType == MediaType.VIDEO) videoStore else imageStore
    private fun getClusterStore() = if(_state.value.mediaType == MediaType.VIDEO) videoClusterStore else imageClusterStore

    private suspend fun getClusterToMediaIdsMap() = if(_state.value.mediaType == MediaType.VIDEO) videoClusterCrossRefRepository.getClusterToVideoIdsMap() else imageClusterCrossRefRepository.getClusterToImageIdsMap()

    private suspend fun getMediaIds(tag: String?, limit: Int, offset: Int): List<Long>{
        return when {
            _state.value.mediaType == MediaType.IMAGE && !tag.isNullOrBlank() -> {
                imageTagsCrossRefRepository.getImageIds(tag, limit, offset)
            }
            _state.value.mediaType == MediaType.VIDEO && !tag.isNullOrBlank() -> {
                videoTagsCrossRefRepository.getVideoIds(tag, limit, offset)
            }
            else -> { emptyList() }
        }
    }

    private suspend fun countMediaIds(tag: String?): Int{
        return when {
            _state.value.mediaType == MediaType.IMAGE && !tag.isNullOrBlank() -> {
                imageTagsCrossRefRepository.count(tag)
            }
            _state.value.mediaType == MediaType.VIDEO && !tag.isNullOrBlank() -> {
                videoTagsCrossRefRepository.count(tag)
            }
            else -> 0
        }
    }

    private suspend fun updateTagLastUsage(tag: String){
        when(_state.value.mediaType){
            MediaType.VIDEO -> {
                val videoTag = allVideoTags.value.find { it.name == tag }
                videoTag?.let{videoTagsRepository.upsert(it.copy(lastUsedAt = System.currentTimeMillis()))}
            }
            MediaType.IMAGE -> {
                val imageTag = allImageTags.value.find { it.name == tag }
                imageTag?.let{imageTagsRepository.upsert(it.copy(lastUsedAt = System.currentTimeMillis()))}
            }
        }
    }

    override fun onCleared() {
        textEmbedder.closeSession()
        imageEmbedder.closeSession()
        super.onCleared()
    }
}
