package com.fpf.smartscan.services

import android.content.Context
import android.content.Intent
import com.fpf.smartscan.constants.EmbeddingStoresFiles
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.utils.isServiceRunning
import java.io.File

fun startIndexing(context: Context, mediaTypes: List<MediaType>) {
    Intent(context.applicationContext, MediaIndexForegroundService::class.java)
        .putStringArrayListExtra(
            MediaIndexForegroundService.EXTRA_MEDIA_TYPES,
            ArrayList(mediaTypes.map { it.name })
        )
        .also { intent -> context.applicationContext.startForegroundService(intent) }
}

fun refreshIndex(context: Context, mediaTypes: List<MediaType>) {
    val running = isServiceRunning(context.applicationContext, MediaIndexForegroundService::class.java)
    if(running){
        context.applicationContext.stopService(Intent(context.applicationContext, MediaIndexForegroundService::class.java))
    }
    startIndexing(context.applicationContext, mediaTypes)
}

fun rebuildIndex(context: Context, mediaTypes: List<MediaType>) {
    mediaTypes.forEach { type ->
        when(type){
            MediaType.IMAGE -> {
                File(context.filesDir, EmbeddingStoresFiles.IMAGE).delete()
                File(context.filesDir, EmbeddingStoresFiles.IMAGE_CLUSTER).delete()
            }
            MediaType.VIDEO -> {
                File(context.filesDir, EmbeddingStoresFiles.VIDEO).delete()
                File(context.filesDir, EmbeddingStoresFiles.VIDEO_CLUSTER).delete()
            }
        }
    }
    val running = isServiceRunning(context.applicationContext, MediaIndexForegroundService::class.java)
    if(running){
        context.applicationContext.stopService(Intent(context.applicationContext, MediaIndexForegroundService::class.java))
    }
    startIndexing(context.applicationContext, mediaTypes)
}