package com.fpf.smartscan.workers

import android.content.Context
import android.util.Log
import androidx.work.*
import com.fpf.smartscan.data.clusters.ClusterCrossRefRepository
import com.fpf.smartscan.data.clusters.ClusterMetadataRepository
import com.fpf.smartscan.data.metadata.MediaMetadataRepository
import com.fpf.smartscan.di.IMAGE_CLUSTER_STORE
import com.fpf.smartscan.di.IMAGE_STORE
import com.fpf.smartscan.di.VIDEO_CLUSTER_STORE
import com.fpf.smartscan.di.VIDEO_STORE
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.search.clusterMedia
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.getValue

class ClusterWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams), KoinComponent {

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

    private val metadataRepo: MediaMetadataRepository by inject()
    private val clusterMetadataRepository: ClusterMetadataRepository by inject()
    private val clusterCrossRefRepository: ClusterCrossRefRepository by inject()

    private val imageStore: FileEmbeddingStore by inject(IMAGE_STORE)
    private val videoStore: FileEmbeddingStore by inject(VIDEO_STORE)

    private val imageClusterStore: FileEmbeddingStore by inject(IMAGE_CLUSTER_STORE)
    private val videoClusterStore: FileEmbeddingStore by inject(VIDEO_CLUSTER_STORE)



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
