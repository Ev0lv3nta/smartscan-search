package com.fpf.smartscan.services

import android.content.Context
import android.content.Intent
import android.util.Log
import com.fpf.smartscan.constants.EmbeddingStoresFiles
import com.fpf.smartscan.data.clusters.ClusterCrossRefRepository
import com.fpf.smartscan.data.clusters.ClusterMetadataRepository
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.utils.isServiceRunning
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
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

suspend fun rebuildIndex(context: Context, mediaEmbeddingStores: List<Pair<MediaType, FileEmbeddingStore>>, clusterCrossRefRepository: ClusterCrossRefRepository, clusterMetadataRepository: ClusterMetadataRepository) {
    mediaEmbeddingStores.forEach { typeToStore ->
        when(typeToStore.first){
            MediaType.IMAGE -> {
                typeToStore.second.clear()
                File(context.filesDir, EmbeddingStoresFiles.IMAGE).delete()
                File(context.filesDir, EmbeddingStoresFiles.IMAGE_CLUSTER).delete()
            }
            MediaType.VIDEO -> {
                typeToStore.second.clear()
                File(context.filesDir, EmbeddingStoresFiles.VIDEO).delete()
                File(context.filesDir, EmbeddingStoresFiles.VIDEO_CLUSTER).delete()
            }
        }
    }
    clusterCrossRefRepository.clear()
    clusterMetadataRepository.clear()

    val running = isServiceRunning(context.applicationContext, MediaIndexForegroundService::class.java)
    if(running){
        context.applicationContext.stopService(Intent(context.applicationContext, MediaIndexForegroundService::class.java))
    }
    startIndexing(context.applicationContext, mediaEmbeddingStores.map{it.first})
}