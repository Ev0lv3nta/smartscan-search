package com.fpf.smartscan.ui.screens.collections

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fpf.smartscan.data.MediaClusterMetadata
import com.fpf.smartscan.data.MediaTag
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn


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

    val allImageTags: StateFlow<List<MediaTag>> = imageTagsRepository.allTags.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val allVideoTags: StateFlow<List<MediaTag>> = videoTagsRepository.allTags.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val allImageClusters: StateFlow<List<MediaClusterMetadata>> = imageClusterMetadataRepository.allMetadata.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val allVideoCluster: StateFlow<List<MediaClusterMetadata>> = videoClusterMetadataRepository.allMetadata.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

}
