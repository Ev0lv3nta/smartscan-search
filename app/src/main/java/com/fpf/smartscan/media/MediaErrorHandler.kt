package com.fpf.smartscan.media

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import coil3.compose.AsyncImagePainter
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import android.util.Log
import com.fpf.smartscan.data.clusters.ClusterCrossRefRepository
import com.fpf.smartscan.data.tags.TagCrossRefRepository
import java.io.FileNotFoundException
import java.lang.SecurityException

suspend fun onMediaLoadingError(context: Context, error: AsyncImagePainter.State.Error, imageEmbedStore: FileEmbeddingStore, videoEmbedStore: FileEmbeddingStore, tagsCrossRefRepository: TagCrossRefRepository, clusterCrossRefRepository: ClusterCrossRefRepository) {
    when (val throwable = error.result.throwable) {
        is SecurityException,
        is FileNotFoundException -> {
            val uri = error.result.request.data
            val id = ContentUris.parseId(uri as Uri)
            val idsToRemove = listOf(id)
            val type = context.contentResolver.getType(uri)


            if(type?.startsWith("image/") == true){
               removeStaleMedia(idsToRemove, imageEmbedStore, tagsCrossRefRepository, clusterCrossRefRepository)
            }else if(type?.startsWith("video/") == true){
                removeStaleMedia(idsToRemove, imageEmbedStore, tagsCrossRefRepository, clusterCrossRefRepository)
            }
            Log.e("MediaError", "Inaccessible URI deleted: $uri", throwable)
        }

        else -> {
            Log.e("MediaError", "Unhandled media error", throwable)
        }
    }
}

suspend fun removeStaleMedia(idsToPurge: List<Long>, store: FileEmbeddingStore, tagsCrossRefRepository: TagCrossRefRepository, clusterCrossRefRepository: ClusterCrossRefRepository){
    store.remove(idsToPurge)
    tagsCrossRefRepository.deleteByMediaIds(idsToPurge)
    clusterCrossRefRepository.deleteByMediaIds(idsToPurge)
}

