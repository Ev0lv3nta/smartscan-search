package io.github.ev0lv3nta.smartscansearch.workers

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.util.Log
import androidx.core.net.toUri
import androidx.work.*
import io.github.ev0lv3nta.smartscansearch.R
import io.github.ev0lv3nta.smartscansearch.constants.PrefsNames
import io.github.ev0lv3nta.smartscansearch.data.clusters.ClusterCrossRefRepository
import io.github.ev0lv3nta.smartscansearch.data.clusters.ClusterMetadataRepository
import io.github.ev0lv3nta.smartscansearch.data.metadata.MediaMetadataRepository
import io.github.ev0lv3nta.smartscansearch.di.CLUSTER_STORE
import io.github.ev0lv3nta.smartscansearch.di.IMAGE_STORE
import io.github.ev0lv3nta.smartscansearch.di.VIDEO_STORE
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import io.github.ev0lv3nta.smartscansearch.media.MediaType
import io.github.ev0lv3nta.smartscansearch.cluster.ClusterManager
import io.github.ev0lv3nta.smartscansearch.index.indexMedia
import io.github.ev0lv3nta.smartscansearch.services.MediaIndexForegroundService
import io.github.ev0lv3nta.smartscansearch.settings.loadSettings
import io.github.ev0lv3nta.smartscansearch.utils.isServiceRunning
import com.fpf.smartscansdk.core.indexers.ImageIndexer
import com.fpf.smartscansdk.core.indexers.VideoIndexer
import com.fpf.smartscansdk.ml.models.ModelAssetSource
import com.fpf.smartscansdk.ml.embeddings.clip.ClipImageEmbedder
import com.fpf.smartscansdk.ml.embeddings.clip.ClipImageEmbedder.Companion.IMAGE_SIZE_X
import com.fpf.smartscansdk.ml.embeddings.clip.ClipImageEmbedder.Companion.IMAGE_SIZE_Y
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.getValue

class IndexWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams), KoinComponent {

    companion object {
        const val TAG = "IndexWorker"

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

    private val sharedPrefs by lazy { applicationContext.getSharedPreferences(PrefsNames.APP_PREFS, MODE_PRIVATE)    }
    private val imageEmbedder by lazy { ClipImageEmbedder(applicationContext, ModelAssetSource.Resource(R.raw.clip_image_encoder_quant))}

    private val mediaMetadataRepository: MediaMetadataRepository by inject()
    private val clusterMetadataRepository: ClusterMetadataRepository by inject()
    private val clusterCrossRefRepository: ClusterCrossRefRepository by inject()

    private val imageStore: FileEmbeddingStore by inject(IMAGE_STORE)
    private val videoStore: FileEmbeddingStore by inject(VIDEO_STORE)

    private val clusterStore: FileEmbeddingStore by inject(CLUSTER_STORE)



    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val appSettings = loadSettings(sharedPrefs)
            imageEmbedder.initialize()
            val serviceRunning = isServiceRunning(applicationContext, MediaIndexForegroundService::class.java)
            if(serviceRunning) {
                return@withContext Result.success()
            }

            val clusterManager = ClusterManager(
                clusterEmbedStore = clusterStore,
                imageEmbedStore = imageStore,
                videoEmbedStore = videoStore,
                clusterCrossRefRepository = clusterCrossRefRepository,
                clusterMetadataRepository = clusterMetadataRepository,
                mediaMetadataRepository = mediaMetadataRepository,
            )

            // Prevents doing full indexes by checking if embedding stores already exist. That responsibility should be left to the foreground service
            // No listener used (may change to avoid silent errors)
            if(imageStore.exists){
                val imageIndexer = ImageIndexer(imageEmbedder,
                    context=applicationContext,
                    listener = null,
                    store = imageStore,
                    quantize = true
                )
                indexMedia(applicationContext, MediaType.IMAGE, imageStore, imageIndexer, mediaMetadataRepository,appSettings.searchableImageDirectories.map{it.toUri()})
            }

            if(videoStore.exists){
                val videoIndexer = VideoIndexer(imageEmbedder,
                    context=applicationContext,
                    listener = null,
                    store = videoStore,
                    quantize = true,
                    width = IMAGE_SIZE_X,
                    height = IMAGE_SIZE_Y
                )
                indexMedia(applicationContext, MediaType.VIDEO, videoStore,videoIndexer, mediaMetadataRepository,appSettings.searchableVideoDirectories.map{it.toUri()})
            }
            clusterManager.cluster()
            return@withContext Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Background indexing errors: ${e.message}", e)
            return@withContext Result.failure()
        }
    }
}
