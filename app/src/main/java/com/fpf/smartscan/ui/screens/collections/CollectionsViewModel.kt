package com.fpf.smartscan.ui.screens.collections

import android.app.Application
import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fpf.smartscan.cluster.ClusterManager
import com.fpf.smartscan.data.tags.TagCrossRefRepository
import com.fpf.smartscan.data.tags.TagRepository
import com.fpf.smartscan.media.MediaCollection
import com.fpf.smartscan.data.clusters.ClusterCrossRefRepository
import com.fpf.smartscan.data.clusters.ClusterMetadataRepository
import com.fpf.smartscan.data.metadata.MediaMetadataRepository
import com.fpf.smartscan.data.tags.Tag
import com.fpf.smartscan.data.tags.TagCrossRef
import com.fpf.smartscan.events.CollectionEvent
import com.fpf.smartscan.events.CollectionEventType
import com.fpf.smartscan.media.MediaItem
import com.fpf.smartscan.media.mediaIdToUri
import com.fpf.smartscan.tag.TagManager
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.collections.forEach
import kotlin.collections.plus


class CollectionsViewModel( 
    application: Application,
    private val tagRepository: TagRepository,
    private val tagCrossRefRepository: TagCrossRefRepository,
    private val clusterMetadataRepository: ClusterMetadataRepository,
    private val clusterCrossRefRepository: ClusterCrossRefRepository,
    private val mediaMetadataRepository: MediaMetadataRepository,
    private val imageStore: FileEmbeddingStore,
    private val videoStore: FileEmbeddingStore,
    clusterStore: FileEmbeddingStore,
    ) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "CollectionsViewModel"
        const val TOP_N = 6
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

    private val _state = MutableStateFlow(CollectionsState())
    val state: StateFlow<CollectionsState> = _state

    val clusterCollections: StateFlow<List<MediaCollection>> = combine(
        clusterCrossRefRepository.getClustersWithCount(),
        _state.map {  it.showAllCollections to  it.groupBySimilarity }.distinctUntilChanged()
    ) { clusters, ( showAllCollections, viewAutoCollections) ->
        if(viewAutoCollections){
            _state.update { it.copy(totalCollections = clusters.size) }
        }
        val filteredClusters = if (showAllCollections) clusters else clusters.take(TOP_N)

        clusterManager.toCollections(filteredClusters)
    }.flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )

    val tagCollections: StateFlow<List<MediaCollection>> = combine(
        tagCrossRefRepository.getTagsWithCounts(),
        _state.map {  it.showAllCollections to  it.groupBySimilarity }.distinctUntilChanged()
    ) { tagsWithCount, ( showAllCollections, viewAutoCollections) ->
        if(!viewAutoCollections){
            _state.update { it.copy(totalCollections = tagsWithCount.size) }
        }
        val tags = if (showAllCollections) tagsWithCount else tagsWithCount.take(TOP_N)
        tagManager.tagsToCollections(tags)
    }.flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    private val _event = MutableSharedFlow<CollectionEvent>()
    val event = _event.asSharedFlow()

    fun onAction(action: CollectionAction){
        when(action){
            is CollectionAction.MergeCollections -> mergeCollections(action.primaryCollectionName, action.isNewMergedLabel)
            is CollectionAction.RenameCollection -> renameCollection(action.newName)
            is CollectionAction.CreateNewTagAndTagClusters -> createNewTagAndTagClusters(action.newName)
            is CollectionAction.TagClusters -> tagClusterCollections(action.tagId)
            is CollectionAction.ToggleSelectedCollection -> toggleSelectedCollection(action.collection)
            is CollectionAction.SetCollectionToView -> setCollectionToView(action.collection)
            is CollectionAction.SetGroupBySimilarity -> setGroupingMode(action.groupBySimilarity)
            is CollectionAction.DeleteCollections -> deleteCollections()
            is CollectionAction.ToggleViewAllCollections -> toggleViewAllCollections()
            is CollectionAction.SetSelectAll -> setSelectAll(action.selectAll)
        }
    }

    fun clearSelectedCollections() = _state.update{ it.copy(selectedCollections = emptySet(), excludedCollections = emptySet(), selectAll = false, selectedCount = 0)}

    private fun renameCollection(newName: String){
        val collection = _state.value.selectedCollections.first()
        viewModelScope.launch(Dispatchers.IO) {
            try{
                if(_state.value.groupBySimilarity){
                    clusterManager.updateLabel(collection.id, newName)
                }else{
                    tagManager.renameTag(collection.name, newName)
                }
                clearSelectedCollections()
                _event.emit(CollectionEvent(CollectionEventType.RENAME, success = true))
            } catch (_: SQLiteConstraintException){
                _event.emit(CollectionEvent(CollectionEventType.RENAME, success = false, message = "Collection already exists"))
            }
            catch (e: Exception){
                Log.e(TAG, "Error renaming collection: ${e.message}")
                _event.emit(CollectionEvent(CollectionEventType.RENAME, success = false, message = "Error renaming collection"))

            }
        }
    }

    private fun deleteCollections(){
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val selectedCollections = getSelectedCollections()
                tagRepository.deleteTagsByName(selectedCollections.map{it.name})
                clearSelectedCollections()
                _event.emit(CollectionEvent(CollectionEventType.DELETE, success = true))
            }catch (e: Exception){
                Log.e(TAG, "Error deleting collections: ${e.message}")
                _event.emit(CollectionEvent(CollectionEventType.DELETE, success = false, message = "Error deleting collections"))
            }
        }
    }

    private fun mergeCollections(primaryCollectionName: String, isNewMergedLabel: Boolean){
        _state.update { it.copy(loading = true) }

        viewModelScope.launch (Dispatchers.IO) {
            try {
                val selectedCollections = getSelectedCollections()
                var primaryCollection = selectedCollections.firstOrNull{it.name == primaryCollectionName}

                if(isNewMergedLabel) {
                    primaryCollection = selectedCollections.firstOrNull()
                    primaryCollection?.let { collection ->
                        if (collection.isAutoCollection) {
                            clusterManager.updateLabel(collection.id, primaryCollectionName)
                        } else {
                            tagManager.renameTag(collection.name, primaryCollectionName)
                        }
                    }
                }

                primaryCollection?.let { collection ->
                    val otherCollections = selectedCollections.filter { selectedCollection -> selectedCollection.id != collection.id }
                    if (collection.isAutoCollection) {
                        clusterManager.mergeClusters(collection.id, otherCollections.map { it.id }, imageStore, videoStore)
                    } else {
                        tagManager.mergeTags(primaryCollectionName, otherCollections.map { it.name })
                    }
                }
                clearSelectedCollections()
                _event.emit(CollectionEvent(CollectionEventType.MERGE, success = true))
            }
            catch (_: SQLiteConstraintException){
                _event.emit(CollectionEvent(CollectionEventType.MERGE, success = false, message = "Collection already exists"))
            }
            catch (e: Exception){
                Log.e(TAG, "Error merging collections: ${e.message}")
                _event.emit(CollectionEvent(CollectionEventType.MERGE, success = false, message = "Error merging collections"))
            }finally {
                _state.update { it.copy(loading = false) }
            }
        }
    }

    private suspend fun tagCluster(clusterId: Long, tagId: Long){
        val clusterCrossRefs = mediaMetadataRepository.getByCluster(clusterId)
        tagCrossRefRepository.insertTagCrossRefs(clusterCrossRefs.map{ TagCrossRef(it.id, tagId)})
    }

    private fun tagClusterCollections(tagId: Long){
        _state.update { it.copy(loading = true) }
        viewModelScope.launch (Dispatchers.IO) {
            try {
                val selectedCollections = getSelectedCollections()
                selectedCollections.forEach { tagCluster(it.id, tagId) }
                clearSelectedCollections()
                _event.emit(CollectionEvent(CollectionEventType.COPY, success = true))
            }catch (e: Exception){
                Log.e(TAG, "Error tagging collection(s): ${e.message}")
                _event.emit(CollectionEvent(CollectionEventType.COPY, success = false, message = "Error tagging collection(s)"))
            }finally {
                _state.update { it.copy(loading = false) }
            }
        }
    }

    private fun createNewTagAndTagClusters(newTag: String){
        _state.update { it.copy(loading = true) }

        viewModelScope.launch (Dispatchers.IO) {
            try {
                val selectedCollections = getSelectedCollections()
                val tagId = tagRepository.insertTags(listOf(Tag(name = newTag))).firstOrNull()?: return@launch
                selectedCollections.forEach { tagCluster(it.id, tagId) }
                clearSelectedCollections()
                _event.emit(CollectionEvent(CollectionEventType.COPY, success = true))
            }catch (_: SQLiteConstraintException){
                _event.emit(CollectionEvent(CollectionEventType.COPY, success = false, message = "Collection already exists"))
            }catch (e: Exception){
                Log.e(TAG, "Error copying collections: ${e.message}")
                _event.emit(CollectionEvent(CollectionEventType.COPY, success = false, message = "Error copying collection(s)"))
            }
            finally {
                _state.update { it.copy(loading = false) }
            }
        }
    }

    private fun setGroupingMode(groupBySimilarity: Boolean) {
        Log.d(TAG, "groupBySimilarity: $groupBySimilarity")
        _state.update { it.copy(groupBySimilarity = groupBySimilarity) }
    }
    

    private fun toggleViewAllCollections(){
        _state.update{ it.copy(showAllCollections = !it.showAllCollections)}
    }

    private fun setCollectionToView(collection: MediaCollection?){
        _state.update { it.copy(collectToView = collection) }
    }


 
    private fun toggleSelectedCollection(item: MediaCollection){
        _state.update {
            if(it.selectAll){
                if (item in it.excludedCollections) {
                    val updatedExcludedResults = it.excludedCollections - item
                    val safeCount = ( it.selectedCount + 1).coerceAtLeast(0 )
                    it.copy(excludedCollections = updatedExcludedResults, selectedCount =safeCount)
                } else {
                    val safeCount = ( it.selectedCount - 1).coerceAtMost(it.totalCollections )
                    val updatedExcludedResults = it.excludedCollections + item
                    it.copy(excludedCollections = updatedExcludedResults, selectedCount = safeCount)
                }
            }
            else{
                if (item in it.selectedCollections) {
                    val safeCount = ( it.selectedCount - 1).coerceAtLeast(0 )
                    val updatedSelectedResults = it.selectedCollections - item
                    it.copy(selectedCollections = updatedSelectedResults, selectedCount = safeCount)
                } else {
                    val safeCount = ( it.selectedCount + 1).coerceAtMost(it.totalCollections )
                    val updatedSelectedResults = it.selectedCollections + item
                    it.copy(selectedCollections = updatedSelectedResults, selectedCount = safeCount)
                }
            }
        }
    }

    private fun setSelectAll(selectAll: Boolean) {
        val currentState = _state.value
        if(currentState.selectAll && currentState.excludedCollections.isNotEmpty()){
            _state.update { it.copy(selectAll = true, selectedCollections = emptySet(), excludedCollections = emptySet())}
        }else{
            _state.update { it.copy(selectAll = selectAll, selectedCollections = emptySet(), excludedCollections = emptySet())}
        }

        _state.update { it.copy(selectedCount=getSelectedCount()) }
    }

    private suspend fun getSelectedCollections(): Set<MediaCollection>{
        val currentState = state.value
        return if(currentState.selectAll){
            val items = if (currentState.groupBySimilarity ){
               if(currentState.showAllCollections) {
                   clusterCollections.value
               } else {
                   clusterManager.toCollections(clusterCrossRefRepository.getClustersWithCount().first() )
               }
            }else{
                if(currentState.showAllCollections) {
                    tagCollections.value
                } else {
                    tagManager.tagsToCollections(tagCrossRefRepository.getTagsWithCounts().first())
                }
            }.toMutableSet()
            items.removeAll(currentState.excludedCollections)
            items
        }else{
            currentState.selectedCollections
        }
    }

    private fun getSelectedCount(): Int{
        val currentState = _state.value
        return if(currentState.selectAll){
            if(currentState.excludedCollections.isEmpty()) currentState.totalCollections else currentState.totalCollections - currentState.excludedCollections.size
        }else{
            currentState.selectedCollections.size
        }
    }
}
