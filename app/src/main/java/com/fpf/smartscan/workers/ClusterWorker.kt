package com.fpf.smartscan.workers

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.work.*
import com.fpf.smartscan.R
import com.fpf.smartscan.constants.EmbeddingStoresFiles
import com.fpf.smartscan.data.MediaDatabase
import com.fpf.smartscan.data.clusters.ClusterCrossRefRepository
import com.fpf.smartscan.data.clusters.ClusterMetadataRepository
import com.fpf.smartscan.data.metadata.MediaMetadataRepository
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.search.clusterMedia
import com.fpf.smartscansdk.ml.models.ModelAssetSource
import com.fpf.smartscansdk.ml.providers.embeddings.clip.ClipImageEmbedder

class ClusterWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "ClusterWorker"

        fun startWorker(context: Context, mediaTypes: List<MediaType> = MediaType.entries) {
            val constraints = Constraints.Builder().build()
            val workRequestBuilder = OneTimeWorkRequestBuilder<ClusterWorker>()
                .setInputData(
                    workDataOf(
                        "media_types" to mediaTypes.map { it.name }.toTypedArray()
                    )
                )
                .setConstraints(constraints)

            val workRequest = workRequestBuilder.build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                TAG,
                ExistingWorkPolicy.KEEP,
                workRequest
            )
        }
    }

    private val imageEmbedder by lazy { ClipImageEmbedder(applicationContext, ModelAssetSource.Resource(R.raw.clip_image_encoder_quant))}

    private val db = MediaDatabase.getDatabase(applicationContext as Application)
    private val clusterMetadataRepository by lazy { ClusterMetadataRepository(db.clusterMetadataDao()) }
    private val clusterCrossRefRepository by lazy { ClusterCrossRefRepository(db.clusterCrossRefDao()) }
    private val metadataRepo by lazy { MediaMetadataRepository(db.metadataDao()) }

    private val imageStore by lazy { FileEmbeddingStore(File(applicationContext.filesDir, EmbeddingStoresFiles.IMAGE), imageEmbedder.embeddingDim)}
    private val imageClusterStore by lazy { FileEmbeddingStore(File(applicationContext.filesDir, EmbeddingStoresFiles.IMAGE_CLUSTER), imageEmbedder.embeddingDim)}
    private val videoStore by lazy { FileEmbeddingStore(File(applicationContext.filesDir, EmbeddingStoresFiles.VIDEO), imageEmbedder.embeddingDim)}

    private val videoClusterStore by lazy { FileEmbeddingStore(File(applicationContext.filesDir, EmbeddingStoresFiles.VIDEO_CLUSTER), imageEmbedder.embeddingDim)}


    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val mediaTypes = inputData.getStringArray("media_types")?.map { MediaType.valueOf(it) } ?: MediaType.entries

            mediaTypes.forEach { mediaType ->
                when(mediaType){
                    MediaType.IMAGE -> {
                        if(imageStore.exists){
                            clusterMedia(clusterCrossRefRepository, imageClusterStore, imageStore, clusterMetadataRepository, metadataRepo, MediaType.IMAGE)
                            Log.d(TAG, "Clustered images successfully")

                        }
                    }
                    MediaType.VIDEO -> {
                        if(videoStore.exists){
                            clusterMedia(clusterCrossRefRepository, videoClusterStore, videoStore, clusterMetadataRepository, metadataRepo, MediaType.VIDEO)
                            Log.d(TAG, "Clustered videos successfully")
                        }
                    }
                }
            }

            return@withContext Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Background clustered errors: ${e.message}", e)
            return@withContext Result.failure()
        }
    }
}
