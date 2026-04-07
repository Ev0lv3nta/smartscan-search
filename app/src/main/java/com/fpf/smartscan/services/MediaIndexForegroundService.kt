package com.fpf.smartscan.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import com.fpf.smartscan.R
import com.fpf.smartscan.MainActivity
import com.fpf.smartscan.constants.EmbeddingStoresFiles
import com.fpf.smartscan.data.images.clusters.ImageClusterCrossRef
import com.fpf.smartscan.data.images.clusters.ImageClusterCrossRefRepository
import com.fpf.smartscan.data.images.clusters.ImageClusterDatabase
import com.fpf.smartscan.data.images.clusters.ImageClusterMetadata
import com.fpf.smartscan.data.images.clusters.ImageClusterMetadataRepository
import com.fpf.smartscan.data.videos.clusters.VideoClusterCrossRef
import com.fpf.smartscan.data.videos.clusters.VideoClusterCrossRefRepository
import com.fpf.smartscan.data.videos.clusters.VideoClusterDatabase
import com.fpf.smartscan.data.videos.clusters.VideoClusterMetadata
import com.fpf.smartscan.data.videos.clusters.VideoClusterMetadataRepository
import com.fpf.smartscan.search.ImageIndexListener
import com.fpf.smartscan.search.VideoIndexListener
import com.fpf.smartscan.settings.loadSettings
import com.fpf.smartscan.media.queryImageIds
import com.fpf.smartscan.media.queryVideoIds
import com.fpf.smartscansdk.core.cluster.IncrementalClusterer
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import com.fpf.smartscansdk.core.embeddings.StoredEmbedding
import com.fpf.smartscansdk.core.indexers.ImageIndexer
import com.fpf.smartscansdk.core.indexers.VideoIndexer
import com.fpf.smartscansdk.core.models.ModelAssetSource
import com.fpf.smartscansdk.ml.providers.embeddings.clip.ClipImageEmbedder
import com.fpf.smartscansdk.ml.providers.embeddings.clip.ClipImageEmbedder.Companion.IMAGE_SIZE_X
import com.fpf.smartscansdk.ml.providers.embeddings.clip.ClipImageEmbedder.Companion.IMAGE_SIZE_Y
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import kotlin.collections.filterNot
import kotlin.collections.map

class MediaIndexForegroundService : Service() {
    companion object {
        const val EXTRA_MEDIA_TYPE = "extra_media_type"
        const val TYPE_IMAGE = "image"
        const val TYPE_VIDEO = "video"
        const val TYPE_BOTH = "both"
        private const val NOTIFICATION_ID = 200
        private const val TAG = "MediaIndexService"
        private const val PREFS_NAME = "AsyncStorage" // For backward-compatibility changing will break!!!
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + Dispatchers.Default)
    private val sharedPrefs by lazy { application.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)    }
    private val imageEmbedder by lazy { ClipImageEmbedder(application, ModelAssetSource.Resource(R.raw.clip_image_encoder_quant))}

    private val imageClusterMetadataRepository by lazy { ImageClusterMetadataRepository(ImageClusterDatabase.getDatabase(application).imageClusterMetadataDao()) }
    private val imageClusterCrossRefRepository by lazy { ImageClusterCrossRefRepository(ImageClusterDatabase.getDatabase(application).imageClusterCrossRefDao()) }

    private val videoClusterMetadataRepository by lazy { VideoClusterMetadataRepository(VideoClusterDatabase.getDatabase(application).videoClusterMetadataDao()) }
    private val videoClusterCrossRefRepository by lazy { VideoClusterCrossRefRepository(VideoClusterDatabase.getDatabase(application).videoClusterCrossRefDao()) }


    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundServiceNotification()
    }

    private fun startForegroundServiceNotification() {
        val activityIntent = Intent(this, MainActivity::class.java)
        val activityPendingIntent = PendingIntent.getActivity(
            this, 0, activityIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            this, getString(R.string.service_media_index_channel_id)
        )
            .setContentTitle(getString(R.string.notif_title_media_index_service))
            .setContentText(getString(R.string.notif_content_media_index_service))
            .setSmallIcon(R.drawable.smartscan_logo)
            .setContentIntent(activityPendingIntent)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            getString(R.string.service_media_index_channel_id),
            getString(R.string.service_media_index_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val mediaType = intent?.getStringExtra(EXTRA_MEDIA_TYPE) ?: TYPE_BOTH

        serviceScope.launch {
            try {
                val appSettings = loadSettings(sharedPrefs)

                imageEmbedder.initialize()

                if (mediaType == TYPE_IMAGE || mediaType == TYPE_BOTH) {
                    val imageStore = FileEmbeddingStore(File(application.filesDir, EmbeddingStoresFiles.IMAGE), imageEmbedder.embeddingDim)
                    indexImages(imageStore, appSettings.searchableImageDirectories.map{it.toUri()})
                    clusterImages(imageStore)
                }

                if (mediaType == TYPE_VIDEO || mediaType == TYPE_BOTH) {
                    val videoStore = FileEmbeddingStore(File(application.filesDir,  EmbeddingStoresFiles.VIDEO), imageEmbedder.embeddingDim )
                    indexVideos(videoStore, appSettings.searchableVideoDirectories.map { it.toUri() })
                    clusterVideos(videoStore)
                }
            } catch (e: CancellationException) {
                // cancelled
            } catch (e: Exception) {
                Log.e(TAG, "Indexing failed:", e)
            } finally {
                sharedPrefs.edit { putString("lastIndexed", System.currentTimeMillis().toString()) } //putString used for backward compat
                imageEmbedder.closeSession()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        serviceJob.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null


    private suspend fun indexImages(imageStore: FileEmbeddingStore, allowedDirs: List<Uri> = emptyList()){
        val imageIndexer = ImageIndexer(imageEmbedder, context=application, listener = ImageIndexListener, store = imageStore)
                val ids = queryImageIds(application, allowedDirs)
        val existingIds = if(imageStore.exists) imageStore.get().map{it.id}.toSet() else emptySet()
        val filteredIds = ids.filterNot { existingIds.contains(it) }
        imageIndexer.run(filteredIds)
    }

    private suspend fun indexVideos(videoStore: FileEmbeddingStore, allowedDirs: List<Uri> = emptyList()){
        val videoIndexer = VideoIndexer(imageEmbedder, context=application, listener = VideoIndexListener, store = videoStore, width = IMAGE_SIZE_X, height = IMAGE_SIZE_Y)
        val ids = queryVideoIds(application, allowedDirs)
        val existingIds = if(videoStore.exists) videoStore.get().map{it.id}.toSet() else emptySet()
        val filteredIds = ids.filterNot { existingIds.contains(it) }
        videoIndexer.run(filteredIds)
    }

    private suspend fun clusterImages(imageStore: FileEmbeddingStore){
        val imageClusterStore = FileEmbeddingStore(File(application.filesDir, EmbeddingStoresFiles.IMAGE_CLUSTER), imageEmbedder.embeddingDim)
        val clusterer = IncrementalClusterer()
        val assignedIds = imageClusterCrossRefRepository.getAllImages()

        var storedEmbeds = if(imageStore.exists) imageStore.get() else emptyList()
        storedEmbeds = storedEmbeds.filterNot {it.id in assignedIds}

        val result = clusterer.cluster(storedEmbeds)

        val clusterMetadatas = result.clusters.values.map{ ImageClusterMetadata(
            clusterId = it.prototypeId,
            prototypeSize = it.metadata.prototypeSize,
            meanSimilarity = it.metadata.meanSimilarity,
            stdSimilarity = it.metadata.stdSimilarity,
            label = it.metadata.label
        ) }
        imageClusterMetadataRepository.upsertMetadatas(clusterMetadatas)

        val clusterEmbeddings = result.clusters.values.map { StoredEmbedding(id = it.prototypeId, embedding = it.embedding, date = System.currentTimeMillis()) }
        imageClusterStore.add(clusterEmbeddings)

        val crossRefs = result.assignments.map { ImageClusterCrossRef(clusterId = it.value, imageId = it.key) }
        imageClusterCrossRefRepository.addImages(crossRefs)
    }

    private suspend fun clusterVideos(videoStore: FileEmbeddingStore){
        val videoClusterStore = FileEmbeddingStore(File(application.filesDir, EmbeddingStoresFiles.VIDEO_CLUSTER), imageEmbedder.embeddingDim)
        val clusterer = IncrementalClusterer()
        val assignedIds = videoClusterCrossRefRepository.getAllVideos()

        var storedEmbeds = if(videoStore.exists) videoStore.get() else emptyList()
        storedEmbeds = storedEmbeds.filterNot {it.id in assignedIds}

        val result = clusterer.cluster(storedEmbeds)

        val clusterMetadatas = result.clusters.values.map{ VideoClusterMetadata(
            clusterId = it.prototypeId,
            prototypeSize = it.metadata.prototypeSize,
            meanSimilarity = it.metadata.meanSimilarity,
            stdSimilarity = it.metadata.stdSimilarity,
            label = it.metadata.label
        ) }
        videoClusterMetadataRepository.upsertMetadatas(clusterMetadatas)

        val clusterEmbeddings = result.clusters.values.map { StoredEmbedding(id = it.prototypeId, embedding = it.embedding, date = System.currentTimeMillis()) }
        videoClusterStore.add(clusterEmbeddings)

        val crossRefs = result.assignments.map { VideoClusterCrossRef(clusterId = it.value, videoId = it.key) }
        videoClusterCrossRefRepository.addVideos(crossRefs)
    }
}
