package com.fpf.smartscan.search

import android.content.Context
import android.net.Uri
import com.fpf.smartscan.data.metadata.MediaMetadata
import com.fpf.smartscan.data.metadata.MediaMetadataRepository
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.media.queryImageIdDateMap
import com.fpf.smartscan.media.queryVideoIdDateMap
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import com.fpf.smartscansdk.core.processors.BatchProcessor
import kotlin.collections.map


suspend fun indexMedia(context: Context,  mediaType: MediaType, store: FileEmbeddingStore, indexer: BatchProcessor<Long, Pair<Long, FloatArray>>, metadataRepo: MediaMetadataRepository, allowedDirs: List<Uri> = emptyList()){
    val idToDateMap = when(mediaType){
        MediaType.IMAGE -> queryImageIdDateMap(context, allowedDirs)
        MediaType.VIDEO ->  queryVideoIdDateMap(context, allowedDirs)
    }
    val existingMediaIdsInEmbedStore =( if(store.exists) store.get() else emptyList()).map{it.id}.toSet()
    val existingMediaMap = metadataRepo.getByType(mediaType).associateBy { it.id }
    val newMediaIds = idToDateMap.keys.filterNot { existingMediaMap.containsKey(it) && existingMediaIdsInEmbedStore.contains(it) }
    val newMedia = newMediaIds.mapNotNull{
        val date = idToDateMap[it]?: return@mapNotNull null
        MediaMetadata(it, mediaType, date)
    }
    metadataRepo.insert(newMedia)
    indexer.run(newMediaIds)
}