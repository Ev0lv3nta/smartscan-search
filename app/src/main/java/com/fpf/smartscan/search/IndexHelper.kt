package com.fpf.smartscan.search

import android.content.Context
import android.net.Uri
import com.fpf.smartscan.data.metadata.MediaMetadata
import com.fpf.smartscan.data.metadata.MediaMetadataRepository
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.media.queryImageIdDateMap
import com.fpf.smartscan.media.queryVideoIdDateMap
import com.fpf.smartscansdk.core.processors.BatchProcessor


suspend fun indexMedia(context: Context, indexer: BatchProcessor<Long, Pair<Long, FloatArray>>, metadataRepo: MediaMetadataRepository, mediaType: MediaType, allowedDirs: List<Uri> = emptyList()){
    val idToDateMap = when(mediaType){
        MediaType.IMAGE -> queryImageIdDateMap(context, allowedDirs)
        MediaType.VIDEO ->  queryVideoIdDateMap(context, allowedDirs)
    }
    val existingMediaMap = metadataRepo.getByType(mediaType).associateBy { it.id }
    val newMediaIds = idToDateMap.keys.filterNot { existingMediaMap.containsKey(it) }
    val newMedia = newMediaIds.mapNotNull{
        val date = idToDateMap[it]?: return@mapNotNull null
        MediaMetadata(it, mediaType, date)
    }
    metadataRepo.upsert(newMedia)
    indexer.run(newMediaIds)
}