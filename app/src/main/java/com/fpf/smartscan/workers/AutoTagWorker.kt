package com.fpf.smartscan.workers

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.work.*
import com.fpf.smartscan.constants.EmbeddingStoresFiles
import com.fpf.smartscan.data.images.tags.ImageTag
import com.fpf.smartscan.data.images.tags.ImageTagCrossRefRepository
import com.fpf.smartscan.data.images.tags.ImageTagRepository
import com.fpf.smartscan.data.videos.tags.VideoTag
import com.fpf.smartscan.data.videos.tags.VideoTagCrossRefRepository
import com.fpf.smartscan.data.videos.tags.VideoTagRepository
import com.fpf.smartscan.search.AutoTagger
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit
import com.fpf.smartscan.data.MediaTag
import com.fpf.smartscan.data.images.ImageDatabase
import com.fpf.smartscan.data.videos.VideoDatabase

//TODO: update to use workers
// Worker updates tag prototypes, cohesion score, and nPrototype, periodically for auto-tagging functionality
// These periodic updates allow suggested tags to dynamically adapt as the user tags new media

class AutoTagWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "AutoTagWorker"
        const val N_PROTOTYPE = 10
        private const val NOTIFICATION_ID = 2000
        private const val MIN_CONFIDENCE_MARGIN = 0.2
        private const val EMBED_DIM = 512


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
                TAG,
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
            )
        }
    }

    val imageTagsRepository by lazy { ImageTagRepository(ImageDatabase.getDatabase(applicationContext as Application).tagDao())}
    val videoTagsRepository by lazy { VideoTagRepository(VideoDatabase.getDatabase(applicationContext as Application).tagDao())}
    val imageTagsCrossRefRepository by lazy { ImageTagCrossRefRepository(ImageDatabase.getDatabase(applicationContext as Application).imageTagCrossRefDao())}
    val videoTagsCrossRefRepository by lazy { VideoTagCrossRefRepository(VideoDatabase.getDatabase(applicationContext as Application).videoTagCrossRefDao())}
    val imageStore = FileEmbeddingStore(File(applicationContext.filesDir, EmbeddingStoresFiles.IMAGE), EMBED_DIM)
    val videoStore = FileEmbeddingStore(File(applicationContext.filesDir, EmbeddingStoresFiles.VIDEO), EMBED_DIM )
    val tagStore = FileEmbeddingStore(File(applicationContext.filesDir, EmbeddingStoresFiles.TAGS), EMBED_DIM)
    val autoTagger by lazy { AutoTagger(tagStore)}


    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val imageTags = imageTagsRepository.getAllTags()
            val videoTags = videoTagsRepository.getAllTags()

            for (tag in imageTags){
                updateImageTag((tag))
            }

            for (tag in videoTags){
                updateVideoTag((tag))
            }

//            val completionTime = measureTimeMillis {
//                val nSuggestedImageTags = tagImages(imageTags)
//                val nSuggestedVideoTags = tagVideos(videoTags)
//                if (nSuggestedImageTags > 0 || nSuggestedVideoTags > 0) {
//                    val title = applicationContext.getString(R.string.notif_title_auto_tag)
//                    val message = buildString {
//                        if (nSuggestedImageTags > 0) append("Tagged $nSuggestedImageTags image(s). ")
//                        if (nSuggestedVideoTags > 0) append("Tagged $nSuggestedVideoTags video(s). ")
//                    }
//                    showNotification(applicationContext, title, message, NOTIFICATION_ID)
//                }
//            }

//            Log.d(TAG, "Completion time: ${completionTime} ms")
            return@withContext Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating tags: ${e.message}", e)
            return@withContext Result.failure()
        }
    }

    private suspend fun updateImageTag(tag: MediaTag){
//        val nTaggedImages = imageTagsCrossRefRepository.count(tag.name)
//        if(nTaggedImages < N_PROTOTYPE || tag.nPrototype >= nTaggedImages ) return
//
//        val imageIds = imageTagsCrossRefRepository.getImageIds(tag.name, N_PROTOTYPE, tag.nPrototype)
//        val storedImageEmbeddings = imageStore.get(imageIds)
//        if(storedImageEmbeddings.isEmpty()) return
//
//        val nPrototypeNew = autoTagger.updateTagPrototype(tag, storedImageEmbeddings.map{it.embedding})
//        val cohesionScore = autoTagger.calculateCohesionScore(tag, storedImageEmbeddings.map{it.embedding})
//        imageTagsRepository.upsert(tag.copy(nPrototype = nPrototypeNew, cohesionScore = cohesionScore))
    }

    private suspend fun updateVideoTag(tag: MediaTag){
//        val nTaggedVideos = videoTagsCrossRefRepository.count(tag.name)
//        if(nTaggedVideos < N_PROTOTYPE || tag.nPrototype >= nTaggedVideos) return
//
//        val videoIds = videoTagsCrossRefRepository.getVideoIds(tag.name, N_PROTOTYPE, tag.nPrototype)
//        val storedVideosEmbeddings = videoStore.get(videoIds)
//        if(storedVideosEmbeddings.isEmpty()) return
//
//        val nPrototypeNew = autoTagger.updateTagPrototype(tag, storedVideosEmbeddings.map{it.embedding})
//        val cohesionScore = autoTagger.calculateCohesionScore(tag, storedVideosEmbeddings.map{it.embedding})
//        videoTagsRepository.upsert(tag.copy(nPrototype = nPrototypeNew, cohesionScore=cohesionScore))
    }

    private suspend fun tagImages(tags: List<ImageTag>): Int{
//        val storedEmbeddings = imageStore.get().map { it }.toSet()
//        val imageTagsToAdd: MutableList<ImageTagCrossRef> = emptyList<ImageTagCrossRef>().toMutableList()
//
//        for (storedEmbed in storedEmbeddings){
//            val result = autoTagger.getSuggestedTags(tags,  storedEmbed.embedding)
//            result.bestMatch?.let{
//                if(result.confidence < MIN_CONFIDENCE_MARGIN) continue
//                val existingTags = imageTagsCrossRefRepository.getTagsForImage(storedEmbed.id).toSet()
//                if (it.name in existingTags) continue
//                imageTagsToAdd.add(ImageTagCrossRef(storedEmbed.id, it.name))
//            }
//        }
//        if(imageTagsToAdd.isNotEmpty()) imageTagsCrossRefRepository.addTags(imageTagsToAdd)
//        return imageTagsToAdd.size
        return 0
    }

    private suspend fun tagVideos( tags: List<VideoTag>): Int{
//        val storedEmbeddings = videoStore.get().map { it }.toSet()
//        val videoTagsToAdd: MutableList<VideoTagCrossRef> = emptyList<VideoTagCrossRef>().toMutableList()
//
//        for (storedEmbed in storedEmbeddings){
//            val result = autoTagger.getSuggestedTags(tags,  storedEmbed.embedding)
//            result.bestMatch?.let{
//                if(result.confidence < MIN_CONFIDENCE_MARGIN) continue
//                val existingTags = videoTagsCrossRefRepository.getTagsForVideo(storedEmbed.id).toSet()
//                if (it.name in existingTags) continue
//                videoTagsToAdd.add(VideoTagCrossRef(storedEmbed.id, it.name))
//            }
//        }
//        if(videoTagsToAdd.isNotEmpty()) videoTagsCrossRefRepository.addTags(videoTagsToAdd)
//        return videoTagsToAdd.size
        return 0
    }

}
