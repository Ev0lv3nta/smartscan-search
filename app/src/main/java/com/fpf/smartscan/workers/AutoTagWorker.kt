package com.fpf.smartscan.workers

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.work.*
import com.fpf.smartscan.R
import com.fpf.smartscan.data.images.ImageTag
import com.fpf.smartscan.data.images.ImageTagCrossRefRepository
import com.fpf.smartscan.data.images.ImageTagDatabase
import com.fpf.smartscan.data.images.ImageTagRepository
import com.fpf.smartscan.data.videos.VideoTag
import com.fpf.smartscan.data.videos.VideoTagCrossRefRepository
import com.fpf.smartscan.data.videos.VideoTagDatabase
import com.fpf.smartscan.data.videos.VideoTagRepository
import com.fpf.smartscan.search.AutoTagger
import com.fpf.smartscan.utils.stringToLong
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import com.fpf.smartscansdk.core.indexers.ImageIndexer
import com.fpf.smartscansdk.core.indexers.VideoIndexer
import com.fpf.smartscansdk.ml.models.loaders.ResourceId
import com.fpf.smartscansdk.ml.providers.embeddings.clip.ClipTextEmbedder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

class AutoTagWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "AutoTagWorker"
        const val N_PROTOTYPE = 10

        fun scheduleWorker(context: Context, frequency: Pair<Long, TimeUnit>, delay: Pair<Long, TimeUnit>? = null) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val workRequestBuilder = PeriodicWorkRequestBuilder<AutoTagWorker>(frequency.first, frequency.second)
                .setConstraints(constraints)

            if (delay != null) {
                workRequestBuilder.setInitialDelay(delay.first, delay.second)
            }

            val workRequest = workRequestBuilder.build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                AutoTagWorker.TAG,
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
            )
        }
    }

    val imageTagsRepository by lazy { ImageTagRepository(ImageTagDatabase.getDatabase(applicationContext as Application).tagDao())}
    val videoTagsRepository by lazy { VideoTagRepository(VideoTagDatabase.getDatabase(applicationContext as Application).tagDao())}
    val imageTagsCrossRefRepository by lazy { ImageTagCrossRefRepository( ImageTagDatabase.getDatabase(applicationContext as Application).imageTagCrossRefDao(), ImageTagDatabase.getDatabase(applicationContext as Application).tagDao())}
    val videoTagsCrossRefRepository by lazy { VideoTagCrossRefRepository(VideoTagDatabase.getDatabase(applicationContext as Application).videoTagCrossRefDao(), VideoTagDatabase.getDatabase(applicationContext as Application).tagDao())}

    val textEmbedder by lazy { ClipTextEmbedder(applicationContext, ResourceId(R.raw.clip_text_encoder_quant))}

    val imageStore = FileEmbeddingStore(File(applicationContext.filesDir, ImageIndexer.INDEX_FILENAME), textEmbedder.embeddingDim)
    val videoStore = FileEmbeddingStore(File(applicationContext.filesDir, VideoIndexer.INDEX_FILENAME), textEmbedder.embeddingDim )
    val tagStore by lazy { FileEmbeddingStore(File(applicationContext.filesDir, "tags_store.bin"), textEmbedder.embeddingDim)}

    val autoTagger by lazy { AutoTagger(tagStore, textEmbedder)}

//    val sharedPrefs by lazy { applicationContext.getSharedPreferences(TAG, Context.MODE_PRIVATE) }

    // TODO: track last number of tags to determine if update required
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val imageTags = imageTagsRepository.getAll()
            val videoTags = videoTagsRepository.getAll()

//            val imageTagIds = imageTags.map { stringToLong(it.name) }
//            val videoTagIds = imageTags.map { stringToLong(it.name) }

            for (tag in imageTags){
                updateImageTag((tag))
            }

            for (tag in videoTags){
                updateVideoTag((tag))
            }

            return@withContext Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error during classification orchestration: ${e.message}", e)
            return@withContext Result.failure()
        }
    }

    private suspend fun updateImageTag(tag: ImageTag){
        val nTaggedImages = imageTagsCrossRefRepository.count(tag.name)
        if(nTaggedImages < N_PROTOTYPE ) return

        val imageIds = imageTagsCrossRefRepository.getImageIds(tag.name, N_PROTOTYPE)
        val storedImageEmbeddings = imageStore.get(imageIds)
        if(storedImageEmbeddings.isEmpty()) return

        val nPrototypeNew = autoTagger.updateTagPrototype(tag, storedImageEmbeddings)
        val cohesionScore = autoTagger.calculateCohesionScore(tag.name, storedImageEmbeddings)
        imageTagsRepository.upsert(tag.copy(nPrototype = nPrototypeNew, cohesionScore = cohesionScore))
    }

    private suspend fun updateVideoTag(tag: VideoTag){
        val nTaggedVideos = videoTagsCrossRefRepository.count(tag.name)
        if(nTaggedVideos < N_PROTOTYPE ) return

        val videoIds = videoTagsCrossRefRepository.getVideoIds(tag.name, N_PROTOTYPE)
        val storedVideosEmbeddings = videoStore.get(videoIds)
        if(storedVideosEmbeddings.isEmpty()) return

        val nPrototypeNew = autoTagger.updateTagPrototype(tag, storedVideosEmbeddings)
        val cohesionScore = autoTagger.calculateCohesionScore(tag.name, storedVideosEmbeddings)
        videoTagsRepository.upsert(tag.copy(nPrototype = nPrototypeNew, cohesionScore=cohesionScore))
    }

}
