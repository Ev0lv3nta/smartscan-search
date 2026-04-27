package com.fpf.smartscan

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.util.Log
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.request.crossfade
import coil3.video.VideoFrameDecoder
import com.fpf.smartscan.constants.EmbeddingStoresFiles
import com.fpf.smartscan.di.embedStoreModule
import com.fpf.smartscan.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import java.io.File

class App : Application() {

    companion object {
        private const val TAG = "App"
    }
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@App)
            modules(embedStoreModule, viewModelModule)
        }

        SingletonImageLoader.setSafe {
            ImageLoader.Builder(this).components {
                add(VideoFrameDecoder.Factory())
            }
                .crossfade(true)
                .memoryCache { MemoryCache.Builder().maxSizePercent(this, 0.25).build() }
                .diskCache { DiskCache.Builder().directory(cacheDir.resolve("image_cache")).maxSizePercent(0.05).build() }
                .build()
        }

        createNotificationChannel(
            channelId = getString(R.string.worker_channel_id),
            channelName = getString(R.string.worker_channel_name),
            description = getString(R.string.worker_channel_description)
        )

        cleanUpIfRequired()
    }
    private fun createNotificationChannel(channelId: String, channelName: String, description: String) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH
        ).apply { this.description = description }
        notificationManager.createNotificationChannel(channel)
    }

    private fun cleanUpIfRequired(){
        val tagFile = File(applicationContext.filesDir, EmbeddingStoresFiles.TAGS)
        if(tagFile.exists()) {
            Log.d(TAG, "Old tag embed store file removed")
            tagFile.delete()
        }
    }

}