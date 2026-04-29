package com.fpf.smartscan.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.fpf.smartscan.R
import com.fpf.smartscan.MainActivity
import com.fpf.smartscan.constants.PrefsNames
import com.fpf.smartscan.data.clusters.ClusterCrossRefRepository
import com.fpf.smartscan.data.clusters.ClusterMetadataRepository
import com.fpf.smartscan.data.metadata.MediaMetadataRepository
import com.fpf.smartscan.di.IMAGE_CLUSTER_STORE
import com.fpf.smartscan.di.IMAGE_STORE
import com.fpf.smartscan.di.VIDEO_CLUSTER_STORE
import com.fpf.smartscan.di.VIDEO_STORE
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.search.ImageIndexListener
import com.fpf.smartscan.search.VideoIndexListener
import com.fpf.smartscan.settings.loadSettings
import com.fpf.smartscan.search.clusterMedia
import com.fpf.smartscan.search.indexMedia
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import com.fpf.smartscansdk.core.indexers.ImageIndexer
import com.fpf.smartscansdk.core.indexers.VideoIndexer
import com.fpf.smartscansdk.ml.models.ModelAssetSource
import com.fpf.smartscansdk.ml.providers.embeddings.clip.ClipImageEmbedder
import com.fpf.smartscansdk.ml.providers.embeddings.clip.ClipImageEmbedder.Companion.IMAGE_SIZE_X
import com.fpf.smartscansdk.ml.providers.embeddings.clip.ClipImageEmbedder.Companion.IMAGE_SIZE_Y
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinComponent
import kotlin.collections.map

class MediaIndexForegroundService : Service(), KoinComponent {
    companion object {
        const val EXTRA_MEDIA_TYPES = "extra_media_types"
        private const val NOTIFICATION_ID = 200
        private const val TAG = "MediaIndexService"
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + Dispatchers.Default)
    private val sharedPrefs by lazy { application.getSharedPreferences(PrefsNames.APP_PREFS, MODE_PRIVATE)    }
    private val imageEmbedder by lazy { ClipImageEmbedder(application, ModelAssetSource.Resource(R.raw.clip_image_encoder_quant))}

    private val metadataRepo: MediaMetadataRepository by inject()
    private val clusterMetadataRepository: ClusterMetadataRepository by inject()
    private val clusterCrossRefRepository: ClusterCrossRefRepository by inject()

    private val imageStore: FileEmbeddingStore by inject(IMAGE_STORE)
    private val videoStore: FileEmbeddingStore by inject(VIDEO_STORE)

    private val imageClusterStore: FileEmbeddingStore by inject(IMAGE_CLUSTER_STORE)
    private val videoClusterStore: FileEmbeddingStore by inject(VIDEO_CLUSTER_STORE)


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
        val mediaTypes = intent?.getStringArrayListExtra(EXTRA_MEDIA_TYPES)
                ?.map(MediaType::valueOf)
                ?: MediaType.entries

        serviceScope.launch {
            try {
                val appSettings = loadSettings(sharedPrefs)
                imageEmbedder.initialize()

                mediaTypes.forEach { mediaType ->
                    when(mediaType){
                        MediaType.IMAGE -> {
                            val imageIndexer = ImageIndexer(imageEmbedder, context=application, listener = ImageIndexListener, store = imageStore)
                            indexMedia(application, MediaType.IMAGE, imageStore, imageIndexer, metadataRepo,appSettings.searchableImageDirectories.map{it.toUri()})
                            clusterMedia(clusterCrossRefRepository, imageClusterStore, imageStore, clusterMetadataRepository, metadataRepo, MediaType.IMAGE)
                        }
                        MediaType.VIDEO -> {
                            val videoIndexer = VideoIndexer(imageEmbedder, context=application, listener = VideoIndexListener, store = videoStore, width = IMAGE_SIZE_X, height = IMAGE_SIZE_Y)
                            indexMedia(application, MediaType.VIDEO, videoStore, videoIndexer, metadataRepo,appSettings.searchableImageDirectories.map{it.toUri()})
                            clusterMedia(clusterCrossRefRepository, videoClusterStore, videoStore, clusterMetadataRepository, metadataRepo, MediaType.VIDEO)
                        }
                    }
                }
            } catch (e: CancellationException) {
                // cancelled
            } catch (e: Exception) {
                Log.e(TAG, "Indexing failed:", e)
            } finally {
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
}
