package com.fpf.smartscan.ui.screens.collections

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fpf.smartscan.model.MediaTag
import com.fpf.smartscan.data.images.ImageDatabase
import com.fpf.smartscan.data.images.clusters.ImageClusterCrossRefRepository
import com.fpf.smartscan.data.images.clusters.ImageClusterMetadataRepository
import com.fpf.smartscan.data.images.tags.ImageTagCrossRefRepository
import com.fpf.smartscan.data.images.tags.ImageTagRepository
import com.fpf.smartscan.data.videos.VideoDatabase
import com.fpf.smartscan.data.videos.clusters.VideoClusterCrossRefRepository
import com.fpf.smartscan.data.videos.clusters.VideoClusterMetadataRepository
import com.fpf.smartscan.data.videos.tags.VideoTagCrossRefRepository
import com.fpf.smartscan.data.videos.tags.VideoTagRepository
import com.fpf.smartscan.collections.MediaCollection
import com.fpf.smartscan.data.MediaClusterMetadata
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.media.getImageUriFromId
import com.fpf.smartscan.media.getVideoUriFromId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class CollectionsViewModel( application: Application) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "CollectionsViewModel"
    }

    private val imageDB by lazy { ImageDatabase.getDatabase(application)}
    private val videoDB by lazy { VideoDatabase.getDatabase(application)}
    private val imageTagsRepository by lazy { ImageTagRepository(imageDB.tagDao())}
    private val videoTagsRepository by lazy { VideoTagRepository(videoDB.tagDao())}
    private val imageTagsCrossRefRepository by lazy { ImageTagCrossRefRepository( imageDB.imageTagCrossRefDao())}
    private val videoTagsCrossRefRepository by lazy {  VideoTagCrossRefRepository(videoDB.videoTagCrossRefDao())}

    private val imageClusterCrossRefRepository by lazy {  ImageClusterCrossRefRepository(imageDB.imageClusterCrossRefDao())}
    private val videoClusterCrossRefRepository by lazy {  VideoClusterCrossRefRepository(videoDB.videoClusterCrossRefDao())}

    private val imageClusterMetadataRepository by lazy {  ImageClusterMetadataRepository(imageDB.imageClusterMetadataDao())}
    private val videoClusterMetadataRepository by lazy {  VideoClusterMetadataRepository(videoDB.videoClusterMetadataDao())}

    private val _state = MutableStateFlow(CollectionsState())
    val state: StateFlow<CollectionsState> = _state

    val mediaClusters: StateFlow<List<MediaClusterMetadata>> = combine(
            imageClusterMetadataRepository.allMetadata,
            videoClusterMetadataRepository.allMetadata,
            _state
        ) { imageClusters, videoClusters, state ->
            when (state.mediaType) {
                MediaType.IMAGE -> imageClusters
                MediaType.VIDEO -> videoClusters
            }
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Lazily,
                initialValue = emptyList()
            )


    val mediaTagCounts: StateFlow<Map<String, Int>> = combine(
        imageTagsCrossRefRepository.getTagCounts(),
        videoTagsCrossRefRepository.getTagCounts(),
        _state
    ) { imageTagCounts, videoTagCounts, state ->
        when (state.mediaType) {
            MediaType.IMAGE -> imageTagCounts
            MediaType.VIDEO -> videoTagCounts
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyMap()
        )

    init {
        viewModelScope.launch {
            mediaTagCounts.first { it.isNotEmpty() }
            loadInitialCollections(topK = 6)
        }
    }

    private suspend fun loadInitialCollections(topK: Int){
        val topTags = mediaTagCounts.value.entries.sortedByDescending { it.value }.take(topK).associate { it.key to it.value }
        _state.update { it.copy(totalCollections = mediaTagCounts.value.size, collections =  getCollections(topTags.keys.toList(), it.mediaType))}
    }

    private suspend fun getCollections(tags: List<String>, mediaType: MediaType): List<MediaCollection> {
        return tags.mapNotNull {
            val id = getMediaMatchingTag(it, mediaType, limit = 1).firstOrNull()
            val uri = id?.let { id -> getUriFromMediaId(id, mediaType) }
            uri?.let { uri ->
                MediaCollection(
                    name = it,
                    thumbNail = uri,
                    size = mediaTagCounts.value[it] ?:0
                )
            }
        }
    }

    private suspend fun getMediaMatchingTag(tag: String, mediaType: MediaType, limit: Int, offset: Int = 0): List<Long> {
        return when (mediaType) {
            MediaType.IMAGE -> imageTagsCrossRefRepository.getMediaIds(tag, limit, offset)
            MediaType.VIDEO -> videoTagsCrossRefRepository.getMediaIds(tag, limit, offset)
        }
    }

    private fun getUriFromMediaId(id: Long, mediaType: MediaType): Uri {
        return when (mediaType) {
            MediaType.IMAGE -> getImageUriFromId(id)
            MediaType.VIDEO -> getVideoUriFromId(id)
        }
    }
}
