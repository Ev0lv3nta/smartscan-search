package com.fpf.smartscan.media

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import coil3.compose.AsyncImagePainter
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import android.util.Log
import com.fpf.smartscan.data.clusters.ClusterCrossRefRepository
import com.fpf.smartscan.data.tags.TagCrossRefRepository
import java.io.FileNotFoundException
import java.lang.SecurityException

const val TAG = "MediaError"
suspend fun onMediaLoadingError( error: AsyncImagePainter.State.Error, imageEmbedStore: FileEmbeddingStore, videoEmbedStore: FileEmbeddingStore, tagsCrossRefRepository: TagCrossRefRepository, clusterCrossRefRepository: ClusterCrossRefRepository) {
    when (val throwable = error.result.throwable) {
        is SecurityException,
        is FileNotFoundException -> {
            val uri = error.result.request.data
            val id = ContentUris.parseId(uri as Uri)
            val idsToRemove = listOf(id)
            if(uri.toString().startsWith(MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString())){
               removeStaleMedia(idsToRemove, imageEmbedStore, tagsCrossRefRepository, clusterCrossRefRepository)
                Log.e("MediaError", "Inaccessible Image URI deleted: $uri ", throwable)
            }else if(uri.toString().startsWith(MediaStore.Video.Media.EXTERNAL_CONTENT_URI.toString())){
                removeStaleMedia(idsToRemove, videoEmbedStore, tagsCrossRefRepository, clusterCrossRefRepository)
                Log.e(TAG, "Inaccessible Video URI deleted: $uri ", throwable)
            }
        }

        else -> {
            Log.e(TAG, "Unhandled media error", throwable)
        }
    }
}

suspend fun removeStaleMedia(idsToPurge: List<Long>, store: FileEmbeddingStore, tagsCrossRefRepository: TagCrossRefRepository, clusterCrossRefRepository: ClusterCrossRefRepository){
    store.remove(idsToPurge)
    tagsCrossRefRepository.deleteByMediaIds(idsToPurge)
    clusterCrossRefRepository.deleteByMediaIds(idsToPurge)
}

