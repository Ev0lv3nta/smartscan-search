package com.fpf.smartscan

import android.app.Application
import android.content.Context
import androidx.collection.MutableIntSet
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.fpf.smartscan.constants.PrefsKeys
import com.fpf.smartscan.constants.PrefsNames
import com.fpf.smartscan.data.DataSyncHelper
import com.fpf.smartscan.data.MediaDatabase
import com.fpf.smartscan.data.clusters.ClusterCrossRefRepository
import com.fpf.smartscan.data.clusters.ClusterMetadataRepository
import com.fpf.smartscan.data.metadata.MediaMetadataRepository
import com.fpf.smartscan.index.ImageIndexListener
import com.fpf.smartscan.index.VideoIndexListener
import com.fpf.smartscan.index.rebuildIndex
import com.fpf.smartscan.index.refreshIndex
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.settings.loadSettings
import com.fpf.smartscan.ui.permissions.StorageAccess
import com.fpf.smartscan.ui.permissions.getStorageAccess
import com.fpf.smartscan.utils.isWorkScheduled
import com.fpf.smartscan.workers.IndexWorker
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

class MainViewModel(
    application: Application,
    private val db: MediaDatabase,
    private val imageStore: FileEmbeddingStore,
    private val videoStore: FileEmbeddingStore,
    private val clusterCrossRefRepository: ClusterCrossRefRepository,
    private val clusterMetadataRepository: ClusterMetadataRepository,
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "MainViewModel"
    }

    private val sharedPrefs = application.getSharedPreferences(PrefsNames.APP_PREFS, Context.MODE_PRIVATE)

    // Global indexing state
    val imageIndexProgress = ImageIndexListener.progress
    val imageIndexStatus = ImageIndexListener.indexingStatus
    val videoIndexProgress = VideoIndexListener.progress
    val videoIndexStatus = VideoIndexListener.indexingStatus

    private val _hasIndexedImages = MutableStateFlow(imageStore.exists)
    private val _hasIndexedVideos = MutableStateFlow(videoStore.exists)
    val hasIndexedImages: StateFlow<Boolean> = _hasIndexedImages
    val hasIndexedVideos: StateFlow<Boolean> = _hasIndexedVideos
    private val _runningMediaTypes = MutableStateFlow<Set<MediaType>>(setOf())
    val runningMediaTypes: StateFlow<Set<MediaType>> = _runningMediaTypes

    val versionName: String? = try {
        val packageInfo = application.packageManager.getPackageInfo(application.packageName, 0)
        packageInfo.versionName
    } catch (e: Exception) {
        null
    }

    private val _isUpdatePopUpVisible = MutableStateFlow(!hasShownUpdatePopUp)
    val  isUpdatePopUpVisible: StateFlow<Boolean> = _isUpdatePopUpVisible

    val hasShownUpdatePopUp: Boolean
        get() = sharedPrefs.getString(PrefsKeys.UPDATES, null) == versionName

    fun closeUpdatePopUp(){
        _isUpdatePopUpVisible.value = false
        sharedPrefs.edit { putString(PrefsKeys.UPDATES, versionName.toString()) }
    }

    fun getUpdates(): List<String> {
        return listOf(
            application.getString(R.string.update_hide_duplicates),
            application.getString(R.string.update_merge_auto_collections),
            application.getString(R.string.update_move_media_auto_collections),
            application.getString(R.string.update_mixed_media_collections),
            application.getString(R.string.update_select_all_search_collections),
            application.getString(R.string.update_scan),
            application.getString(R.string.update_ui_improvements_bug_fixes),
        )
    }

    fun prepareApp(onAppReady: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val appSettings = loadSettings(sharedPrefs)

            // Always run on app start to handle media that may have been deleted from the device
            DataSyncHelper.sync(
                application, imageStore = imageStore,
                videoStore=videoStore,
                allowedImageDirs = appSettings.searchableImageDirectories.map{it.toUri()},
                allowedVideoDirs = appSettings.searchableVideoDirectories.map{it.toUri()},
                mediaMetadataRepository = MediaMetadataRepository(db.metadataDao())
            )

            val oldImageCachedDb = DataSyncHelper.checkOldCachedImageDb(application)
            val oldVideoCachedDb = DataSyncHelper.checkOldCachedVideoDb(application)
            val transferNeeded = oldImageCachedDb != null && oldVideoCachedDb != null
            if (transferNeeded) {
                DataSyncHelper.transferOldDbToNew(application, oldImageCachedDb, oldVideoCachedDb, db)
            }

            if(!isWorkScheduled(context = application, workName = IndexWorker.TAG)) scheduleIndexWorker()
            onAppReady()
        }
    }

    fun refreshMediaIndex(mediaTypes: List<MediaType>){
        val storageAccess = getStorageAccess(getApplication())
        if (storageAccess != StorageAccess.Denied) {
            _runningMediaTypes.update { mediaTypes.toSet()}
            refreshIndex(getApplication(), mediaTypes)
        }
    }

    fun rebuildMediaIndex(mediaTypes: List<MediaType>){
        val storageAccess = getStorageAccess(getApplication())
        if (storageAccess != StorageAccess.Denied) {
            val mediaTypeToEmbedStore = mediaTypes.map{
                when(it) {
                    MediaType.IMAGE -> it to imageStore
                    MediaType.VIDEO -> it to videoStore
                }
            }
            viewModelScope.launch {
                _runningMediaTypes.update { mediaTypes.toSet()}
                rebuildIndex(getApplication(), mediaTypeToEmbedStore, clusterCrossRefRepository, clusterMetadataRepository)
            }
        }
    }

    fun onIndexingFinished(mediaType: MediaType) {
        when(mediaType){
            MediaType.IMAGE -> _hasIndexedImages.value = imageStore.exists
            MediaType.VIDEO -> _hasIndexedVideos.value = videoStore.exists
        }
        resetIndexingState(mediaType)
        _runningMediaTypes.update { it - mediaType}
    }

    private fun resetIndexingState(mediaType: MediaType){
        when(mediaType){
            MediaType.IMAGE -> ImageIndexListener.reset()
            MediaType.VIDEO -> VideoIndexListener.reset()
        }
    }

    private fun scheduleIndexWorker(){
        if (!imageStore.exists && !videoStore.exists) return
        // Delay is required to prevent race condition issues on first index
        IndexWorker.scheduleWorker(getApplication(), Pair(1L, TimeUnit.DAYS), Pair(1L, TimeUnit.DAYS))
    }

    override fun onCleared() {
        runBlocking {
            imageStore.save()
            videoStore.save()
        }
        super.onCleared()
    }
}