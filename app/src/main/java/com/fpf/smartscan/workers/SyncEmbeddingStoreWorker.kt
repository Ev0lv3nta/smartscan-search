package com.fpf.smartscan.workers

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.util.Log
import androidx.work.*
import com.fpf.smartscan.constants.EmbeddingStoresFiles
import com.fpf.smartscan.media.getImageToDateMap
import com.fpf.smartscan.media.getVideoToDateMap
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import androidx.core.content.edit
import com.fpf.smartscan.constants.PrefsKeys
import com.fpf.smartscan.constants.PrefsNames
import com.fpf.smartscan.search.ImageIndexListener
import com.fpf.smartscan.search.VideoIndexListener
import com.fpf.smartscansdk.core.processors.Metrics

class SyncEmbeddingStoreWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "SyncEmbeddingStoreWorker"

        fun scheduleWorker(context: Context) {
            val workRequestBuilder = OneTimeWorkRequestBuilder<SyncEmbeddingStoreWorker>()
            val workRequest = workRequestBuilder.build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                TAG,
                ExistingWorkPolicy.KEEP,
                workRequest
            )
        }
    }

    private val sharedPrefs by lazy { applicationContext.getSharedPreferences(PrefsNames.APP_PREFS, MODE_PRIVATE)    }
    private val embedDim = 512
    private val imageStore by lazy { FileEmbeddingStore(File(applicationContext.filesDir, EmbeddingStoresFiles.IMAGE), embedDim)}
    private val videoStore by lazy { FileEmbeddingStore(File(applicationContext.filesDir, EmbeddingStoresFiles.VIDEO), embedDim)}


    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            syncStore(
                store = imageStore,
                tempFileName = "${EmbeddingStoresFiles.IMAGE}.tmp",
                finalFileName = EmbeddingStoresFiles.IMAGE,
                idToDate = { ids -> getImageToDateMap(applicationContext, ids) }
            )

            syncStore(
                store = videoStore,
                tempFileName = "${EmbeddingStoresFiles.VIDEO}.tmp",
                finalFileName = EmbeddingStoresFiles.VIDEO,
                idToDate = { ids -> getVideoToDateMap(applicationContext, ids) }
            )
            sharedPrefs.edit {
                putBoolean(PrefsKeys.KEY_SYNC_COMPLETE, true)
            }
            Log.d(TAG, "Sync complete successfully")
            // force trigger refresh on index
            ImageIndexListener.onComplete(applicationContext, Metrics.Success())
            VideoIndexListener.onComplete(applicationContext, Metrics.Success())

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Sync error: ${e.message}", e)
            Result.failure()
        }
    }

    private suspend fun syncStore(
        store: FileEmbeddingStore,
        tempFileName: String,
        finalFileName: String,
        idToDate: suspend (List<Long>) -> Map<Long, Long>,
    ) {
        val embeds = store.get()
        val dateMap = idToDate(embeds.map { it.id })
        val updated = embeds.mapNotNull {
            val date = dateMap[it.id] ?: return@mapNotNull null
            it.copy(date = date)
        }

        val tempFile = File(applicationContext.cacheDir, tempFileName)
        val tempStore = FileEmbeddingStore(tempFile, embedDim)
        tempStore.add(updated)

        val finalFile = File(applicationContext.filesDir, finalFileName)
        if (finalFile.exists()) finalFile.delete()
        tempFile.renameTo(finalFile)
    }
}
