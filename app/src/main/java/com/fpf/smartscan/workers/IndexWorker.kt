package com.fpf.smartscan.workers

import android.app.Application
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.work.*
import com.fpf.smartscan.R
import com.fpf.smartscan.constants.EmbeddingStoresFiles
import com.fpf.smartscan.data.MediaDatabase
import com.fpf.smartscan.data.clusters.ClusterCrossRefRepository
import com.fpf.smartscan.data.clusters.ClusterMetadataRepository
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.media.queryImageIds
import com.fpf.smartscan.media.queryVideoIds
import com.fpf.smartscan.search.clusterMedia
import com.fpf.smartscan.settings.loadSettings
import com.fpf.smartscansdk.core.indexers.ImageIndexer
import com.fpf.smartscansdk.core.indexers.VideoIndexer
import com.fpf.smartscansdk.core.models.ModelAssetSource
import com.fpf.smartscansdk.ml.providers.embeddings.clip.ClipImageEmbedder
import com.fpf.smartscansdk.ml.providers.embeddings.clip.ClipImageEmbedder.Companion.IMAGE_SIZE_X
import com.fpf.smartscansdk.ml.providers.embeddings.clip.ClipImageEmbedder.Companion.IMAGE_SIZE_Y

class IndexWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "IndexWorker"
        private const val PREFS_NAME = "AsyncStorage" // For backward-compatibility changing will break!!!


        fun scheduleWorker(context: Context, frequency: Pair<Long, TimeUnit>, delay: Pair<Long, TimeUnit>? = null) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val workRequestBuilder = PeriodicWorkRequestBuilder<IndexWorker>(frequency.first, frequency.second)
                .setConstraints(constraints)

            if (delay != null) {
                workRequestBuilder.setInitialDelay(delay.first, delay.second)
            }

            val workRequest = workRequestBuilder.build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                TAG,
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
            )
        }
    }

    private val sharedPrefs by lazy { applicationContext.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)    }
    private val imageEmbedder by lazy { ClipImageEmbedder(applicationContext, ModelAssetSource.Resource(R.raw.clip_image_encoder_quant))}

    private val db = MediaDatabase.getDatabase(applicationContext as Application)
    private val clusterMetadataRepository by lazy { ClusterMetadataRepository(db.clusterMetadataDao()) }
    private val clusterCrossRefRepository by lazy { ClusterCrossRefRepository(db.clusterCrossRefDao()) }

    private val imageStore by lazy { FileEmbeddingStore(File(applicationContext.filesDir, EmbeddingStoresFiles.IMAGE), imageEmbedder.embeddingDim)}
    private val imageClusterStore by lazy { FileEmbeddingStore(File(applicationContext.filesDir, EmbeddingStoresFiles.IMAGE_CLUSTER), imageEmbedder.embeddingDim)}
    private val videoStore by lazy { FileEmbeddingStore(File(applicationContext.filesDir, EmbeddingStoresFiles.VIDEO), imageEmbedder.embeddingDim)}

    private val videoClusterStore by lazy { FileEmbeddingStore(File(applicationContext.filesDir, EmbeddingStoresFiles.VIDEO_CLUSTER), imageEmbedder.embeddingDim)}


    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val appSettings = loadSettings(sharedPrefs)
            imageEmbedder.initialize()

            // Prevents doing full indexes. That responsibility should be left to the foreground service
            if(imageStore.exists){
                indexImages(imageStore, appSettings.searchableImageDirectories.map{it.toUri()})
                clusterMedia(clusterCrossRefRepository, imageClusterStore, imageStore, clusterMetadataRepository, MediaType.IMAGE)
            }

            // Prevents doing full indexes. That responsibility should be left to the foreground service
            if(videoStore.exists){
                indexVideos(videoStore, appSettings.searchableVideoDirectories.map { it.toUri() })
                clusterMedia(clusterCrossRefRepository, videoClusterStore, videoStore, clusterMetadataRepository, MediaType.VIDEO)
            }

            return@withContext Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Background indexing errors: ${e.message}", e)
            return@withContext Result.failure()
        }
    }

    private suspend fun indexImages(imageStore: FileEmbeddingStore, allowedDirs: List<Uri> = emptyList()){
        val imageIndexer = ImageIndexer(imageEmbedder, context=applicationContext, listener = null, store = imageStore)
        val ids = queryImageIds(applicationContext, allowedDirs)
        val existingIds = if(imageStore.exists) imageStore.get().map{it.id}.toSet() else emptySet()
        val filteredIds = ids.filterNot { existingIds.contains(it) }
        imageIndexer.run(filteredIds)
    }

    private suspend fun indexVideos(videoStore: FileEmbeddingStore, allowedDirs: List<Uri> = emptyList()){
        val videoIndexer = VideoIndexer(imageEmbedder, context=applicationContext, listener = null, store = videoStore, width = IMAGE_SIZE_X, height = IMAGE_SIZE_Y)
        val ids = queryVideoIds(applicationContext, allowedDirs)
        val existingIds = if(videoStore.exists) videoStore.get().map{it.id}.toSet() else emptySet()
        val filteredIds = ids.filterNot { existingIds.contains(it) }
        videoIndexer.run(filteredIds)
    }
}
