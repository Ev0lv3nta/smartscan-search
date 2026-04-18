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
import coil3.compose.AsyncImagePainter
import com.fpf.smartscan.media.getImageUriFromId
import kotlinx.coroutines.Dispatchers
import com.fpf.smartscan.R
import com.fpf.smartscan.constants.EmbeddingStoresFiles
import com.fpf.smartscan.data.MediaDatabase
import com.fpf.smartscan.data.clusters.ClusterCrossRefRepository
import com.fpf.smartscan.data.clusters.ClusterMetadataRepository
import com.fpf.smartscan.data.tags.TagCrossRef
import com.fpf.smartscan.data.tags.TagCrossRefRepository
import com.fpf.smartscan.data.tags.TagRepository
import com.fpf.smartscan.data.tags.Tag
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
import com.fpf.smartscan.search.AutoTagger
import com.fpf.smartscan.search.SearchQuery
import com.fpf.smartscan.search.TagSuggestionsResult
import com.fpf.smartscan.search.VideoIndexListener
import com.fpf.smartscan.services.MediaIndexForegroundService
import com.fpf.smartscan.services.startIndexing
import com.fpf.smartscan.utils.isWorkScheduled
import com.fpf.smartscan.workers.IndexWorker
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
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class SearchViewModel( application: Application) : AndroidViewModel(application) {
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


    private val db = MediaDatabase.getDatabase(application)

    private val tagsRepository by lazy { TagRepository(db.tagDao())}
    private val tagsCrossRefRepository by lazy { TagCrossRefRepository( db.tagCrossRefDao())}
    private val clusterCrossRefRepository by lazy { ClusterCrossRefRepository(db.clusterCrossRefDao()) }

    val allTags: StateFlow<List<Tag>> = tagsRepository.allTags.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

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

    private var cachedIds= mutableListOf<Long>()


    init {
        loadImageIndex()
        viewModelScope.launch{
            processQuery()
        }
        viewModelScope.launch(Dispatchers.IO){
            if(!isWorkScheduled(context = application, workName = IndexWorker.TAG)) scheduleIndexWorker()
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
        _state.value = _state.value.copy(totalResults = 0, searchResults = emptyList(), selectedResults = emptySet(), autoCompleteTagResults = emptyList(), error = null, tagFilter = null, suggestedTags = TagSuggestionsResult(), tagOnlySearch = false)
    }

    fun search(threshold: Float, useClusterSearch: Boolean){
        val store = getStore()
        if(!store.exists) {
            _state.update{ currentState -> currentState.copy(error = getApplication<Application>().getString(R.string.search_error_not_indexed))}
            return
        }
        when(_state.value.queryType){
            QueryType.IMAGE -> {
                imageSearch(store, threshold, useClusterSearch)
            }
            QueryType.TEXT -> {
                textSearch(store, threshold, useClusterSearch)
            }
        }
    }

    private fun textSearch(store: FileEmbeddingStore, threshold: Float, useClusterSearch: Boolean) {
        cachedIds = mutableListOf() // clear on new search

        val query = searchFieldState.text.toString()
        if (query.isBlank()) {
            _state.update{currentState -> currentState.copy(error = getApplication<Application>().getString(R.string.search_error_empty_query))}
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
                val idsMatchingTag: List<Long> = getMediaMatchingTag(tag, _state.value.mediaType,RESULTS_BATCH_SIZE, 0) // load initial
                val totalResults = countMediaMatchingTag(tag, _state.value.mediaType)

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
                val targetClusters = if (useClusterSearch) getTargetClusters(embedding, threshold, 3) else emptyList()
                val filterIds: Set<Long> = buildSet {
                    for (clusterId in targetClusters) {
                        val ids = clusterCrossRefRepository.getClusterToMediaIdsMap()[clusterId] ?: continue
                        addAll(ids)
                    }
                    addAll(idsMatchingTag)
                }
                val queryResults = store.query(embedding, Int.MAX_VALUE, threshold, filterIds)
                cachedIds.addAll(queryResults)
                handleQueryResults(queryResults, store)
            } catch (e: Exception) {
                Log.e(TAG, "$e")
                _state.emit(_state.value.copy(error = getApplication<Application>().getString(R.string.search_error_unknown)))
            } finally {
                _state.emit(_state.value.copy(loading = false, textEmbedderLastUsage = System.currentTimeMillis()))
            }
        }
    }

    private fun imageSearch(store: FileEmbeddingStore, threshold: Float, useClusterSearch: Boolean) {
        cachedIds = mutableListOf() // clear on new search

        val queryImage = _state.value.queryImage?: return

        reset()
        _state.value = _state.value.copy( loading = true)

        viewModelScope.launch((Dispatchers.IO)) {
            try {
                if(!imageEmbedder.isInitialized()) imageEmbedder.initialize()
                if(shouldShutdownModel(_state.value.textEmbedderLastUsage)) textEmbedder.closeSession() // prevent keeping both models open

                val bitmap = getBitmapFromUri(getApplication<Application>(), queryImage, IMAGE_SIZE_X)
                val embedding = imageEmbedder.embed(bitmap)
                val mediaIdsInCluster: Set<Long> = if(useClusterSearch) buildSet {
                    for (clusterId in getTargetClusters(embedding, threshold, 3)) {
                        val ids = clusterCrossRefRepository.getClusterToMediaIdsMap()[clusterId] ?: continue
                        addAll(ids)
                    }
                } else emptySet()
                val resultIds = store.query(embedding, Int.MAX_VALUE, threshold, mediaIdsInCluster)
                cachedIds.addAll(resultIds)
                handleQueryResults(resultIds, store)
            } catch (e: Exception) {
                Log.e(TAG, "$e")
                _state.emit(_state.value.copy(error = getApplication<Application>().getString(R.string.search_error_unknown)))
            } finally {
                _state.emit(_state.value.copy(loading = false, imageEmbedderLastUsage = System.currentTimeMillis()))
            }
        }
    }

    private suspend fun getTargetClusters(queryEmbedding: FloatArray, threshold: Float, topK: Int = 1): List<Long>{
        val store = getClusterStore()
        if(!store.exists) return emptyList()
        val resultIds = store.query(queryEmbedding, topK, threshold)
        return resultIds
    }

    private suspend fun handleQueryResults(queryResults: List<Long>, store: FileEmbeddingStore, totalResults: Int? = null) {
        val totalCount = totalResults?: queryResults.size
        val initialBatch = queryResults.take(RESULTS_BATCH_SIZE) // initial results the rest loaded dynamically
        val (validIds, idsToPurge) = filterAccessibleMediaStoreIds(getApplication(), initialBatch, _state.value.mediaType)
        val filteredSearchResults = validIds.map { mediaIdToUri(it, _state.value.mediaType) }


        _state.emit( _state.value.copy(totalResults = totalCount - idsToPurge.size, searchResults = filteredSearchResults))

        if (filteredSearchResults.isEmpty()) {
            _state.emit(_state.value.copy(error = getApplication<Application>().getString(R.string.search_error_no_results)))
        }

        if(idsToPurge.isNotEmpty()){
            cachedIds.removeAll(idsToPurge) // PREVENTS duplicates
            purgeStaleItems(store, idsToPurge)
        }
    }

    fun externalSearch(intentSearchQuery: SearchQuery?, similarityThreshold: Float, useClusterSearch: Boolean){
        if(intentSearchQuery == null || hasHandledExternalSearch) return

        when(intentSearchQuery) {
            is SearchQuery.ImageQuery -> {
                setMediaType(intentSearchQuery.mediaType)
                updateSearchImageUri(intentSearchQuery.uri)
                updateQueryType(QueryType.IMAGE)
                search(similarityThreshold, useClusterSearch)
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
                    val filteredSearchResults = _state.value.searchResults + filteredResults.map { mediaIdToUri(it, _state.value.mediaType) }
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
            removeStaleMedia(idsToPurge, store, tagsCrossRefRepository, clusterCrossRefRepository)
        }
    }

    private suspend fun getPaginatedResult(currentItemsCount: Int): List<Long>{
        return if(_state.value.tagOnlySearch && _state.value.tagFilter != null){
            val offset = (currentItemsCount).coerceAtMost(_state.value.totalResults)
            getMediaMatchingTag(_state.value.tagFilter, _state.value.mediaType, RESULTS_BATCH_SIZE, offset = offset)
        }else{
            val start = currentItemsCount.coerceAtLeast(0)
            val end = (start + RESULTS_BATCH_SIZE).coerceAtMost(cachedIds.size)
            if (start >= end) return emptyList()
            cachedIds.subList(start, end)
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

    private fun shouldShutdownModel(lastUsage: Long?) = lastUsage != null && System.currentTimeMillis() - lastUsage >= MODEL_SHUTDOWN_DURATION_THRESHOLD

    fun onIndex(){
        when(_state.value.mediaType){
            MediaType.IMAGE -> startIndexing(getApplication(), MediaIndexForegroundService.TYPE_IMAGE)
            MediaType.VIDEO -> startIndexing(getApplication(), MediaIndexForegroundService.TYPE_VIDEO)
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
        _state.update{currentState -> currentState.copy(selectedResults = emptySet(), suggestedTags = TagSuggestionsResult())}
    }

    fun tagSelectedItems(tag: String){
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val selectedMediaIds = _state.value.selectedResults.map { ContentUris.parseId(it) }

                when (val mediaType = _state.value.mediaType) {
                    MediaType.IMAGE -> {
                        val existing = tagsRepository.getTagsByName(listOf(tag)).firstOrNull()
                        var id = existing?.id
                        if(id == null){
                            id = tagsRepository.insertTags(listOf(Tag(name=tag.trim()))).first()
                        }
                        val tagEntries = selectedMediaIds.map { TagCrossRef(mediaId = it, tagId = id, type = mediaType) }
                        tagsCrossRefRepository.upsertTagCrossRefs(tagEntries)
                    }
                    MediaType.VIDEO -> {
                        val existing = tagsRepository.getTagsByName(listOf(tag)).firstOrNull()
                        var id = existing?.id
                        if(id == null){
                            id = tagsRepository.insertTags(listOf(Tag(name=tag.trim()))).first()
                        }
                        val tagEntries = selectedMediaIds.map { TagCrossRef(mediaId = it, tagId = id, type = mediaType) }
                        tagsCrossRefRepository.upsertTagCrossRefs(tagEntries)
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

    private fun scheduleIndexWorker(){
        if (!imageStore.exists && !videoStore.exists) return
        // Delay is required to prevent race condition issues on first index
        IndexWorker.scheduleWorker(getApplication(), Pair(1L, TimeUnit.DAYS), Pair(1L, TimeUnit.DAYS))
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
                        val suggestedTags = autoTagger.getSuggestedTags(allTags.value, prototype)
                        _state.update{currentState -> currentState.copy(suggestedTags = suggestedTags)}

                    }
                    MediaType.VIDEO -> {
                        val videoEmbeddings = videoStore.get(ids)
                        val prototype = generatePrototypeEmbedding(videoEmbeddings.map{it.embedding})
                        val suggestedTags = autoTagger.getSuggestedTags(allTags.value, prototype)
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

    private suspend fun getMediaMatchingTag(tagName: String?, mediaType: MediaType, limit: Int, offset: Int): List<Long>{
        tagName?: return emptyList()
        return when (mediaType){
            MediaType.IMAGE -> {
                val tag = tagsRepository.getTagsByName(listOf(tagName)).firstOrNull()
                tag?.let { tagsCrossRefRepository.getByTag(it.id, limit, offset).map{it.mediaId}  }?: emptyList()
            }
            MediaType.VIDEO -> {
                val tag = tagsRepository.getTagsByName(listOf(tagName)).firstOrNull()
                tag?.let { tagsCrossRefRepository.getByTag(it.id, limit, offset).map{it.mediaId} } ?: emptyList()
            }
        }
    }


    private suspend fun countMediaMatchingTag(tagName: String?, mediaType: MediaType): Int{
        tagName?: return 0
        return when (mediaType) {
            MediaType.IMAGE -> {
                val tag = tagsRepository.getTagsByName(listOf(tagName)).firstOrNull()
                tag?.let{tagsCrossRefRepository.count(it.id)}?: 0
            }
            MediaType.VIDEO -> {
                val tag = tagsRepository.getTagsByName(listOf(tagName)).firstOrNull()
                tag?.let{tagsCrossRefRepository.count(it.id)}?: 0
            }
        }
    }
    private suspend fun updateTagLastUsage(tag: String){
        val tag = allTags.value.find { it.name == tag }?: return
        tagsRepository.updateTags(listOf(Tag(tag.id, tag.name, System.currentTimeMillis())))
    }

    private fun mediaIdToUri(id: Long, mediaType: MediaType): Uri {
        return when (mediaType) {
            MediaType.IMAGE -> getImageUriFromId(id)
            MediaType.VIDEO -> getVideoUriFromId(id)
        }
    }

    override fun onCleared() {
        textEmbedder.closeSession()
        imageEmbedder.closeSession()
        // required now that remove() method only removes from in-memory cache not disk
        runBlocking {
            imageStore.save()
            videoStore.save()
        }
        super.onCleared()
    }
}
