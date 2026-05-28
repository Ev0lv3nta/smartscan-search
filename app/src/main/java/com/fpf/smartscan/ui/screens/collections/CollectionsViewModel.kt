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
        }
    }

    fun clearSelectedCollections() = _state.update{ it.copy(selectedCollections = emptySet())}

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
                tagRepository.deleteTagsByName(_state.value.selectedCollections.map{it.name})
                clearSelectedCollections()
                _event.emit(CollectionEvent(CollectionEventType.DELETE, success = true))
            }catch (e: Exception){
                Log.e(TAG, "Error deleting collections: ${e.message}")
                _event.emit(CollectionEvent(CollectionEventType.DELETE, success = false, message = "Error deleting collections"))
            }
        }
    }

    private fun mergeCollections(primaryCollectionName: String, isNewMergedLabel: Boolean){
        val currentState = _state.value
        val selectedCollections = currentState.selectedCollections
        var primaryCollection = selectedCollections.firstOrNull{it.name == primaryCollectionName}
        _state.update { it.copy(loading = true) }

        viewModelScope.launch (Dispatchers.IO) {
            try {
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
        tagCrossRefRepository.upsertTagCrossRefs(clusterCrossRefs.map{ TagCrossRef(it.id, tagId)})
    }

    private fun tagClusterCollections(tagId: Long){
        val selectedCollections = _state.value.selectedCollections
        _state.update { it.copy(loading = true) }

        viewModelScope.launch (Dispatchers.IO) {
            try {
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
        val selectedCollections = _state.value.selectedCollections
        _state.update { it.copy(loading = true) }

        viewModelScope.launch (Dispatchers.IO) {
            try {
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


    private fun toggleSelectedCollection(collection: MediaCollection){
        _state.update { currentState ->
            if (collection in currentState.selectedCollections) {
                val updatedSelectedResults = currentState.selectedCollections - collection
                currentState.copy(selectedCollections = updatedSelectedResults)
            } else {
                val updatedSelectedResults = currentState.selectedCollections + collection
                currentState.copy(selectedCollections = updatedSelectedResults)
            }
        }
    }

    private fun toggleViewAllCollections(){
        _state.update{ it.copy(showAllCollections = !it.showAllCollections)}
    }

    private fun setCollectionToView(collection: MediaCollection?){
        _state.update { it.copy(collectToView = collection) }
    }
}
