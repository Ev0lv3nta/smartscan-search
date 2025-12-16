package com.fpf.smartscan.ui.screens.search

import android.app.Application
import android.content.ContentUris
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
import com.fpf.smartscan.data.images.ImageTag
import com.fpf.smartscan.data.images.ImageTagCrossRef
import com.fpf.smartscan.data.images.ImageTagCrossRefRepository
import com.fpf.smartscan.data.images.ImageTagDatabase
import com.fpf.smartscan.data.images.ImageTagRepository
import com.fpf.smartscan.data.videos.VideoTag
import com.fpf.smartscan.data.videos.VideoTagCrossRef
import com.fpf.smartscan.data.videos.VideoTagCrossRefRepository
import com.fpf.smartscan.data.videos.VideoTagDatabase
import com.fpf.smartscan.data.videos.VideoTagRepository
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.search.QueryType
import com.fpf.smartscan.utils.canOpenUri
import com.fpf.smartscan.media.getVideoUriFromId
import com.fpf.smartscan.search.ImageIndexListener
import com.fpf.smartscan.search.VideoIndexListener
import com.fpf.smartscan.services.MediaIndexForegroundService
import com.fpf.smartscan.services.startIndexing
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import com.fpf.smartscansdk.core.indexers.ImageIndexer
import com.fpf.smartscansdk.core.indexers.VideoIndexer
import com.fpf.smartscansdk.core.media.getBitmapFromUri
import com.fpf.smartscansdk.ml.models.loaders.ResourceId
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
    val tagFilter: String? = null
)

class SearchViewModel(private val application: Application) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "SearchViewModel"
        const val RESULTS_BATCH_SIZE = 36
        private const val MODEL_SHUTDOWN_DURATION_THRESHOLD = 60_000L
    }

    val imageIndexProgress = ImageIndexListener.progress
    val imageIndexStatus = ImageIndexListener.indexingStatus
    val videoIndexProgress = VideoIndexListener.progress
    val videoIndexStatus = VideoIndexListener.indexingStatus

    private val textEmbedder = ClipTextEmbedder(application, ResourceId(R.raw.clip_text_encoder_quant))
    private val imageEmbedder = ClipImageEmbedder(application, ResourceId(R.raw.clip_image_encoder_quant))

    val imageStore = FileEmbeddingStore(File(application.filesDir, ImageIndexer.INDEX_FILENAME), imageEmbedder.embeddingDim)
    val videoStore = FileEmbeddingStore(File(application.filesDir, VideoIndexer.INDEX_FILENAME), imageEmbedder.embeddingDim )

    val imageTagsRepository = ImageTagRepository(ImageTagDatabase.getDatabase(application).tagDao())
    val videoTagsRepository = VideoTagRepository(VideoTagDatabase.getDatabase(application).tagDao())
    val imageTagsCrossRefRepository = ImageTagCrossRefRepository( ImageTagDatabase.getDatabase(application).imageTagCrossRefDao(), ImageTagDatabase.getDatabase(application).tagDao(),)
    val videoTagsCrossRefRepository = VideoTagCrossRefRepository(VideoTagDatabase.getDatabase(application).videoTagCrossRefDao(), VideoTagDatabase.getDatabase(application).tagDao(),)


    val allImageTags: StateFlow<List<ImageTag>> = imageTagsRepository.getAll().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val allVideoTags: StateFlow<List<VideoTag>> = videoTagsRepository.getAll().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

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


    init {
        loadImageIndex()
        viewModelScope.launch(){
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
        _state.value = _state.value.copy(totalResults = 0, searchResults = emptyList(), selectedResults = emptyList(), autoCompleteTagResults = emptyList(), error = null, tagFilter = null)
    }

    fun search(threshold: Float){
        when(_state.value.queryType){
            QueryType.IMAGE -> {
                imageSearch(threshold)
            }
            QueryType.TEXT -> {
                textSearch(threshold)
            }
        }
    }

    private fun textSearch(threshold: Float) {
        val query = searchFieldState.text.toString()
        if (query.isBlank()) {
            _state.update{currentState -> currentState.copy(error = application.getString(R.string.search_error_empty_query))}
            return
        }

        val store = if(_state.value.mediaType == MediaType.VIDEO) videoStore else imageStore
        if(!store.exists) {
            _state.update{ currentState -> currentState.copy(error = application.getString(R.string.search_error_not_indexed))}
            return
        }
        reset()
        _state.value = _state.value.copy(loading = true)

        viewModelScope.launch((Dispatchers.IO)) {
            try {
                val regex = Regex("""^#([a-zA-Z0-9_]+)""")
                val match = regex.find(query)
                val tag = match?.groupValues?.get(1)
                tag.let{_state.update { currentState -> currentState.copy(tagFilter = tag) }}

                val actualQueryStart = if(!tag.isNullOrBlank()) tag.length + 1 else 0
                val actualQuery = query.substring(actualQueryStart).trim()

                val idsMatchingTag: List<Long> = when{
                    _state.value.mediaType == MediaType.IMAGE && !tag.isNullOrBlank() -> {
                        imageTagsCrossRefRepository.getImageIds(tag)
                    }
                    _state.value.mediaType == MediaType.VIDEO && !tag.isNullOrBlank() -> {
                        videoTagsCrossRefRepository.getVideoIds(tag)
                    }
                    else -> { emptyList()}
                }

                // tag only search
                if(idsMatchingTag.isNotEmpty() && actualQuery.isBlank()){
                    return@launch handleQueryResults(idsMatchingTag, store)
                }else if(actualQuery.isBlank()){
                    return@launch handleQueryResults(emptyList(), store)
                }

                if(!textEmbedder.isInitialized())textEmbedder.initialize()
                if(shouldShutdownModel(_state.value.imageEmbedderLastUsage)) imageEmbedder.closeSession() // prevent keeping both models open

                val embedding = textEmbedder.embed(actualQuery)
                val queryResults = store.query(embedding, Int.MAX_VALUE, threshold).map { it.id }.filter { idsMatchingTag.isEmpty() || it in idsMatchingTag}
                handleQueryResults(queryResults, store)
            } catch (e: Exception) {
                Log.e(TAG, "$e")
                _state.emit(_state.value.copy(error = application.getString(R.string.search_error_unknown)))
            } finally {
                _state.emit(_state.value.copy(loading = false, textEmbedderLastUsage = System.currentTimeMillis()))
            }
        }
    }

    private fun imageSearch(threshold: Float) {
        val queryImage = _state.value.queryImage?: return
        val store = if(_state.value.mediaType == MediaType.VIDEO) videoStore else imageStore
        if(!store.exists) {
            _state.value = _state.value.copy(error = application.getString(R.string.search_error_not_indexed))
            return
        }
        reset()
        _state.value = _state.value.copy( loading = true)

        viewModelScope.launch((Dispatchers.IO)) {
            try {
                if(!imageEmbedder.isInitialized()) imageEmbedder.initialize()
                if(shouldShutdownModel(_state.value.textEmbedderLastUsage)) textEmbedder.closeSession() // prevent keeping both models open

                val idsMatchingTag: List<Long> = when{
                    _state.value.mediaType == MediaType.IMAGE && !_state.value.tagFilter.isNullOrBlank() -> {
                        imageTagsCrossRefRepository.getImageIds(_state.value.tagFilter!!)
                    }
                    _state.value.mediaType == MediaType.VIDEO && !_state.value.tagFilter.isNullOrBlank() -> {
                        videoTagsCrossRefRepository.getVideoIds(_state.value.tagFilter!!)
                    }
                    else -> { emptyList()}
                }
                val bitmap = getBitmapFromUri(application, queryImage, IMAGE_SIZE_X)
                val embedding = imageEmbedder.embed(bitmap)
                val resultIds = store.query(embedding, Int.MAX_VALUE, threshold).map { it.id }.filter { idsMatchingTag.isEmpty() || it in idsMatchingTag}
                handleQueryResults(resultIds, store)
            } catch (e: Exception) {
                Log.e(TAG, "$e")
                _state.emit(_state.value.copy(error = application.getString(R.string.search_error_unknown)))
            } finally {
                _state.emit(_state.value.copy(loading = false, imageEmbedderLastUsage = System.currentTimeMillis()))

            }
        }
    }

    private suspend fun handleQueryResults(queryResults: List<Long>, store: FileEmbeddingStore) {
        val initialBatch = queryResults.take(RESULTS_BATCH_SIZE) // initial results the rest loaded dynamically

        val (unprocessedFilteredResults, idsToPurge) = initialBatch.map { id ->
                val uri = if (_state.value.mediaType == MediaType.VIDEO) getVideoUriFromId(id) else getImageUriFromId(id)
                id to uri
        }.partition { (_, uri) -> canOpenUri(application, uri) }

        val filteredSearchResults = unprocessedFilteredResults.map { it.second }

        _state.emit( _state.value.copy(totalResults = queryResults.size, searchResults = filteredSearchResults))

        if (filteredSearchResults.isEmpty()) {
            _state.emit(_state.value.copy(error = application.getString(R.string.search_error_no_results)))
        }

        if(idsToPurge.isNotEmpty()){
            viewModelScope.launch(Dispatchers.IO) {
                store.remove(idsToPurge.map { it.first })
            }
        }
    }

    fun onLoadMore() {
        if (isLoadingMoreSearchResults.getAndSet(true)) return
        val store = if(_state.value.mediaType == MediaType.VIDEO) videoStore else imageStore
        val currentItemsCount = _state.value.searchResults.size
        if (currentItemsCount >= _state.value.totalResults) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val end = (currentItemsCount + RESULTS_BATCH_SIZE).coerceAtMost(_state.value.totalResults)
                val batch = store.query(currentItemsCount, end).take(RESULTS_BATCH_SIZE)

                val idsMatchingTag: List<Long> = when{
                    _state.value.mediaType == MediaType.IMAGE && !_state.value.tagFilter.isNullOrBlank() -> {
                        imageTagsCrossRefRepository.getImageIds(_state.value.tagFilter!!)
                    }
                    _state.value.mediaType == MediaType.VIDEO && !_state.value.tagFilter.isNullOrBlank() -> {
                        videoTagsCrossRefRepository.getVideoIds(_state.value.tagFilter!!)
                    }
                    else -> { emptyList()}
                }
                val (filteredResults, idsToPurge) = batch
                    .filter { idsMatchingTag.isEmpty() || it.id in idsMatchingTag}
                    .map { embed ->
                        val uri = if (_state.value.mediaType == MediaType.VIDEO) getVideoUriFromId(embed.id) else getImageUriFromId(embed.id)
                        embed.id to uri
                }.partition { (_, uri) -> canOpenUri(application, uri) }

                if (filteredResults.isNotEmpty()) {
                    val filteredSearchResults = _state.value.searchResults + filteredResults.map { it.second }
                    _state.emit(_state.value.copy(searchResults = filteredSearchResults))
                }

                if (idsToPurge.isNotEmpty()) {
                    val store = if (_state.value.mediaType == MediaType.VIDEO) videoStore else imageStore
                    viewModelScope.launch(Dispatchers.IO) {
                        store.remove(idsToPurge.map { it.first })
                    }
                }
            }finally {
                isLoadingMoreSearchResults.set(false)
            }
        }
    }

    fun toggleViewResult(uri: Uri?){
        _state.value = _state.value.copy(resultToView =uri)
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
        _state.update{currentState -> currentState.copy(selectedResults = emptyList())}
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

    @OptIn(FlowPreview::class)
    suspend fun processQuery() {
        snapshotFlow { searchFieldState.text }
            .debounce(50)
            .collectLatest { query: CharSequence ->
                val text = query.toString()
                val cursor = searchFieldState.selection.end
                val prefix = text.substring(0, cursor)

                // Regex: find #tag at the end of prefix
                val match = Regex("""^#([a-zA-Z0-9_]*)$""").find(prefix)

                if (match != null) {
                    val partialTag = match.groupValues[1]
                    // Track autocomplete only while typing the tag
                        when (_state.value.mediaType) {
                            MediaType.IMAGE -> {
                                _state.emit(_state.value.copy(autoCompleteTagResults = allImageTags.value
                                    .filter { it.name.startsWith(partialTag, ignoreCase = true) }
                                    .map{it.name}))
                            }

                            MediaType.VIDEO -> {
                                _state.emit(_state.value.copy(autoCompleteTagResults = allVideoTags.value
                                    .filter { it.name.startsWith(partialTag, ignoreCase = true) }
                                    .map { it.name }))
                            }
                        }
                    }
                else {
                    _state.emit(_state.value.copy(autoCompleteTagResults = emptyList()))
                }
            }
    }

    fun onSelectAutoCompleteResult(tag: String){
        searchFieldState.edit { replace(0, searchFieldState.text.length, "#$tag ") }
    }

    override fun onCleared() {
        textEmbedder.closeSession()
        imageEmbedder.closeSession()
        super.onCleared()
    }
}
