package com.fpf.smartscan.ui.screens.search

import android.app.Application
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.launch
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fpf.smartscan.data.images.ImageEmbeddingDatabase
import com.fpf.smartscan.data.images.ImageEmbeddingRepository
import com.fpf.smartscan.media.getImageUriFromId
import kotlinx.coroutines.Dispatchers
import com.fpf.smartscan.R
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.search.QueryType
import com.fpf.smartscan.data.videos.VideoEmbeddingDatabase
import com.fpf.smartscan.data.videos.VideoEmbeddingRepository
import com.fpf.smartscan.utils.canOpenUri
import com.fpf.smartscan.media.getVideoUriFromId
import com.fpf.smartscan.search.ImageIndexListener
import com.fpf.smartscan.search.VideoIndexListener
import com.fpf.smartscan.services.MediaIndexForegroundService
import com.fpf.smartscan.services.startIndexing
import com.fpf.smartscansdk.core.data.Embedding
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import com.fpf.smartscansdk.core.indexers.ImageIndexer
import com.fpf.smartscansdk.core.indexers.VideoIndexer
import com.fpf.smartscansdk.core.media.getBitmapFromUri
import com.fpf.smartscansdk.ml.data.ResourceId
import com.fpf.smartscansdk.ml.models.providers.embeddings.clip.ClipConfig
import com.fpf.smartscansdk.ml.models.providers.embeddings.clip.ClipImageEmbedder
import com.fpf.smartscansdk.ml.models.providers.embeddings.clip.ClipTextEmbedder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

data class SearchState(
    val searchResults: List<Uri> = emptyList(),
    val totalResults: Int = 0,
    val mediaType: MediaType = MediaType.IMAGE,
    val queryType: QueryType = QueryType.TEXT,
    val queryImage: Uri? = null,
    val query: String = "",
    val hasIndexedImages: Boolean? = false,
    val hasIndexedVideos: Boolean? = false,
    val loading: Boolean = false,
    val error: String? = null,
    val resultToView: Uri? = null
)

class SearchViewModel(private val application: Application) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "SearchViewModel"
        const val RESULTS_BATCH_SIZE = 30
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

    private val repository: ImageEmbeddingRepository = ImageEmbeddingRepository(
        ImageEmbeddingDatabase.getDatabase(application).imageEmbeddingDao()
    )
    private val videoRepository: VideoEmbeddingRepository = VideoEmbeddingRepository(
        VideoEmbeddingDatabase.getDatabase(application).videoEmbeddingDao()
    )
    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state

    private val _hasRefreshedImageIndex = MutableStateFlow(false)
    private val _hasRefreshedVideoIndex = MutableStateFlow(false)
    private val _hasShownImageIndexAlert = MutableStateFlow(false)
    private val _hasShownVideoIndexAlert = MutableStateFlow(false)
    private val _alertTitle = MutableStateFlow<String?>(null)
    val alertTitle: StateFlow<String?> = _alertTitle
    private val _alertDescription = MutableStateFlow<String?>(null)
    val alertDescription: StateFlow<String?> = _alertDescription
    private val isLoadingMoreSearchResults = AtomicBoolean(false)

    var imageEmbedderLastUsage: Long? = null
    var textEmbedderLastUsage: Long? = null
    var hasLoadVideoIndex: Boolean = false

    init {
        loadImageIndex()
    }

    private fun loadImageIndex(){
        loadIndex(imageStore, { repository.getAllEmbeddingsWithFileSync() })
    }

    private fun loadVideoIndex(){
        loadIndex(videoStore, { videoRepository.getAllEmbeddingsWithFileSync() })
    }

    private fun loadIndex(store: FileEmbeddingStore, fetchFromRoom: suspend () -> List<Embedding>){
        viewModelScope.launch(Dispatchers.IO){
            try {
                _state.emit(_state.value.copy(error = null, loading = true))

                val embeddings = if(store.exists) {
                    store.get()
                } else  {
                    // For backwards compatibility with old Room storage
                    val embs = fetchFromRoom()
                    store.add(embs)
                    embs
                }
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

    fun clearResults(){
        _state.value = _state.value.copy(searchResults = emptyList())
    }

    fun setMediaType(type: MediaType) {
        _state.value = _state.value.copy(mediaType = type)
        reset()

        // saves memory by lazy loading video index
        if(type == MediaType.VIDEO && !hasLoadVideoIndex){
            viewModelScope.launch(Dispatchers.IO){loadVideoIndex()}
        }
    }

    private fun reset(){
        _state.value = _state.value.copy(error = null)
        clearResults()
    }

    fun textSearch(query: String, threshold: Float = 0.2f) {
        if (query.isBlank()) {
            _state.value = _state.value.copy(error = application.getString(R.string.search_error_empty_query))
            return
        }

        val store = if(_state.value.mediaType == MediaType.VIDEO) videoStore else imageStore
        if(!store.exists) {
            _state.value = _state.value.copy(error = application.getString(R.string.search_error_not_indexed))
            return
        }
        _state.value = _state.value.copy(error = null , loading = true)

        viewModelScope.launch((Dispatchers.IO)) {
            try {
                if(!textEmbedder.isInitialized())textEmbedder.initialize()
                if(shouldShutdownModel(imageEmbedderLastUsage)) imageEmbedder.closeSession() // prevent keeping both models open
                val embedding = textEmbedder.embed(query)
                search(store, embedding, threshold)
            } catch (e: Exception) {
                Log.e(TAG, "$e")
                _state.emit(_state.value.copy(error = application.getString(R.string.search_error_unknown)))
            } finally {
                _state.emit(_state.value.copy(loading = false))
                textEmbedderLastUsage = System.currentTimeMillis()
            }
        }
    }

    fun imageSearch(threshold: Float = 0.2f) {
        val queryImage = _state.value.queryImage?: return
        val store = if(_state.value.mediaType == MediaType.VIDEO) videoStore else imageStore
        if(!store.exists) {
            _state.value = _state.value.copy(error = application.getString(R.string.search_error_not_indexed))
            return
        }
        _state.value = _state.value.copy(error = null , loading = true)

        viewModelScope.launch((Dispatchers.IO)) {
            try {
                if(!imageEmbedder.isInitialized()) imageEmbedder.initialize()
                if(shouldShutdownModel(textEmbedderLastUsage)) textEmbedder.closeSession() // prevent keeping both models open
                val bitmap = getBitmapFromUri(application, queryImage, ClipConfig.IMAGE_SIZE_X)
                val embedding = imageEmbedder.embed(bitmap)
                search(store, embedding, threshold)
            } catch (e: Exception) {
                Log.e(TAG, "$e")
                _state.emit(_state.value.copy(error = application.getString(R.string.search_error_unknown)))
            } finally {
                _state.emit(_state.value.copy(loading = false))
                imageEmbedderLastUsage = System.currentTimeMillis()
            }
        }
    }

    private suspend fun search(store: FileEmbeddingStore, embedding: FloatArray, threshold: Float = 0.2f) {
        var results = store.query(embedding, Int.MAX_VALUE, threshold)
        _state.emit( _state.value.copy(totalResults = results.size))
        results = results.take(RESULTS_BATCH_SIZE) // initial results the result loaded dynamically

        if (results.isEmpty()) {
            _state.emit(_state.value.copy(searchResults = emptyList(), error = application.getString(R.string.search_error_no_results)))
            return
        }

        val (filteredResults, idsToPurge) = results.map { embed ->
            val uri = if (_state.value.mediaType == MediaType.VIDEO) getVideoUriFromId(embed.id) else getImageUriFromId(embed.id)
            embed.id to uri
        }.partition { (_, uri) -> canOpenUri(application, uri) }

        if (filteredResults.isEmpty()) {
            _state.emit(_state.value.copy(searchResults = emptyList(), error = application.getString(R.string.search_error_no_results)))
        }

        val filteredSearchResults = filteredResults.map { it.second }
        _state.emit(_state.value.copy(searchResults = filteredSearchResults))

        if(idsToPurge.isNotEmpty()){
            viewModelScope.launch(Dispatchers.IO) {
                store.remove(idsToPurge.map { it.first })
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

    fun onLoadMore() {
        if (isLoadingMoreSearchResults.getAndSet(true)) return
        val store = if(_state.value.mediaType == MediaType.VIDEO) videoStore else imageStore
        val currentItemsCount = _state.value.searchResults.size
        if (currentItemsCount >= _state.value.totalResults) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val end = (currentItemsCount + RESULTS_BATCH_SIZE).coerceAtMost(_state.value.totalResults)
                val batch = store.query(currentItemsCount, end).take(RESULTS_BATCH_SIZE)

                val (filteredResults, idsToPurge) = batch.map { embed ->
                    embed.id to if (_state.value.mediaType == MediaType.VIDEO) getVideoUriFromId(embed.id) else getImageUriFromId(
                        embed.id
                    )
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

    fun onIndex(){
        when(_state.value.mediaType){
            MediaType.IMAGE -> startIndexing(application, MediaIndexForegroundService.TYPE_IMAGE)
            MediaType.VIDEO -> startIndexing(application, MediaIndexForegroundService.TYPE_VIDEO)
        }
    }
    

    override fun onCleared() {
        textEmbedder.closeSession()
        imageEmbedder.closeSession()
        super.onCleared()
    }
}
