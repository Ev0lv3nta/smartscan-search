package com.fpf.smartscan.services

import android.content.Context
import android.content.Intent
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.utils.isServiceRunning

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