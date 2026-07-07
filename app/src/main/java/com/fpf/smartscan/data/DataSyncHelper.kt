package com.fpf.smartscan.data

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import com.fpf.smartscan.constants.EmbeddingStoresFiles
import com.fpf.smartscan.data.MediaDatabase.Companion.OLD_DB_IMAGE_NAME
import com.fpf.smartscan.data.MediaDatabase.Companion.OLD_DB_VIDEO_NAME
import com.fpf.smartscan.data.metadata.MediaMetadata
import com.fpf.smartscan.data.metadata.MediaMetadataRepository
import com.fpf.smartscan.data.old.images.ImageTagCrossRefRepository
import com.fpf.smartscan.data.old.images.ImageTagDatabase
import com.fpf.smartscan.data.old.images.ImageTagRepository
import com.fpf.smartscan.data.tags.TagCrossRefRepository
import com.fpf.smartscan.data.tags.TagRepository
import com.fpf.smartscan.data.old.videos.VideoTagCrossRefRepository
import com.fpf.smartscan.data.old.videos.VideoTagDatabase
import com.fpf.smartscan.data.old.videos.VideoTagRepository
import com.fpf.smartscan.data.tags.Tag
import com.fpf.smartscan.data.tags.TagCrossRef
import com.fpf.smartscan.media.MediaStoreHelper
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.media.removeStaleMedia
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import com.fpf.smartscansdk.core.embeddings.StoredEmbedding
import com.fpf.smartscansdk.core.embeddings.toQInt8Embed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object DataSyncHelper {
    const val TAG = "DataSyncHelper"
    private const val EMBED_DIM: Int = 512

    suspend fun migrateToQuantEmbedStoreIfNeeded(oldFileToQuantStoreMap: Map<File, FileEmbeddingStore>){
        oldFileToQuantStoreMap.entries.forEach {
            if (!it.key.exists()) return@forEach
            quantizeEmbedStore(it.key, it.value)
        }
    }

    suspend fun sync(
        context: Context,
        imageStore: FileEmbeddingStore,
        videoStore: FileEmbeddingStore,
        allowedImageDirs: List<Uri> = emptyList(),
        allowedVideoDirs: List<Uri> = emptyList(),
        mediaMetadataRepository: MediaMetadataRepository
        ){
        syncEmbedStoreAndMetadata(context,
            store = imageStore,
            allowedDirs = allowedImageDirs,
            mediaMetadataRepository = mediaMetadataRepository,
            mediaType = MediaType.IMAGE
        )
        syncEmbedStoreAndMetadata(context,
            store = videoStore,
            allowedDirs = allowedVideoDirs,
            mediaMetadataRepository = mediaMetadataRepository,
            mediaType = MediaType.VIDEO
        )
        Log.d(TAG, "Data sync completed successfully")
    }

    suspend fun transferOldDbIfNeeded(application: Application, newDb: MediaDatabase){
        val oldImageTagDbCachedFile = checkOldCachedImageDb(application)
        val oldVideoTagDbCachedFile = checkOldCachedVideoDb(application)
        val transferNeeded = oldImageTagDbCachedFile != null && oldVideoTagDbCachedFile != null
        if (!transferNeeded) return

        val oldImageTagDbPath = application.getDatabasePath(OLD_DB_IMAGE_NAME)
        val oldVideoTagDbPath = application.getDatabasePath(OLD_DB_VIDEO_NAME)

        Log.d(TAG, "Old DB detected, transferring...")

        oldImageTagDbCachedFile.copyTo(oldImageTagDbPath, overwrite = true)
        oldVideoTagDbCachedFile.copyTo(oldVideoTagDbPath, overwrite = true)

        transfer(application = application, newDb = newDb)

        oldImageTagDbCachedFile.delete()
        oldVideoTagDbCachedFile.delete()
        oldImageTagDbPath.delete()
        oldVideoTagDbPath.delete()
    }

    private suspend fun quantizeEmbedStore( oldEmbedStoreFile: File, quantStore: FileEmbeddingStore){
        val oldEmbedStore = FileEmbeddingStore(oldEmbedStoreFile, EMBED_DIM)
        val embeds = oldEmbedStore.get().map { it.copy(embedding = it.embedding.toQInt8Embed()) }
        quantStore.add(embeds)
        oldEmbedStore.clear()
        oldEmbedStoreFile.delete()
        Log.d(TAG, "Successfully added quantized embeddings from: ${oldEmbedStoreFile.name}")
    }

    private fun checkOldCachedImageDb(application: Application): File?{
        val cachedDB = File(application.filesDir, OLD_DB_IMAGE_NAME)
        return if(cachedDB.exists()) cachedDB else null
    }

    private fun checkOldCachedVideoDb(application: Application): File?{
        val cachedDB = File(application.filesDir, OLD_DB_VIDEO_NAME)
        return if(cachedDB.exists()) cachedDB else null
    }

    private suspend fun transfer(application: Application, newDb: MediaDatabase) = withContext(Dispatchers.IO) {
        val newTagsRepository = TagRepository(newDb.tagDao())
        val newTagsCrossRefRepository = TagCrossRefRepository( newDb.tagCrossRefDao())
        val metadataRepo = MediaMetadataRepository(newDb.metadataDao())

        // Images
        val oldImageDb = ImageTagDatabase.getDatabase(application)
        val oldImageTagRepository = ImageTagRepository(oldImageDb.tagDao())
        val oldImageTagCrossRefRepository = ImageTagCrossRefRepository( oldImageDb.imageTagCrossRefDao())
        val imageTags = oldImageTagRepository.getAllTags()
        val imageCrossRefs = oldImageTagCrossRefRepository.getAllCrossRefs()

        val imageTagIds = newTagsRepository.insertTags(imageTags.map{ Tag(name=it.name, lastUsedAt = it.lastUsedAt)})
        val nameToImageTagId = imageTagIds
            .zip(imageTags.map { it.name })
            .filter { it.first != -1L }
            .associate { it.second to it.first }

        val validImageIds = metadataRepo.getByType(MediaType.IMAGE).map{it.id}.toSet()
        val updatedImageCrossRefs = imageCrossRefs.mapNotNull {
            val tagId = nameToImageTagId[it.tag] ?: return@mapNotNull null
            if (it.imageId !in validImageIds) return@mapNotNull null

            TagCrossRef(
                mediaId = it.imageId,
                tagId = tagId,
                mediaType = MediaType.IMAGE
            )
        }

        newTagsCrossRefRepository.insertTagCrossRefs(updatedImageCrossRefs)
        oldImageDb.close()

        Log.d(TAG, "Image transfer complete. ${imageTagIds.size} tags transferred. ${updatedImageCrossRefs.size} cross refs transferred.")


        // Videos
        val oldVideoDb = VideoTagDatabase.getDatabase(application)
        val oldVideoTagRepository = VideoTagRepository(oldVideoDb.tagDao())
        val oldVideoTagCrossRefRepository = VideoTagCrossRefRepository( oldVideoDb.videoTagCrossRefDao())
        val videoTags = oldVideoTagRepository.getAllTags()
        val videoCrossRefs = oldVideoTagCrossRefRepository.getAllCrossRefs()

        val videoTagIds = newTagsRepository.insertTags(videoTags.map{ Tag(name=it.name, lastUsedAt = it.lastUsedAt)})
        val nameToVideoTagId = videoTagIds
            .zip(videoTags.map { it.name })
            .filter { it.first != -1L }
            .associate { it.second to it.first }

        val validVideoIds = metadataRepo.getByType(MediaType.VIDEO).map{it.id}.toSet()
        val updatedVideoCrossRefs = videoCrossRefs.mapNotNull {
            val tagId = nameToVideoTagId[it.tag] ?: return@mapNotNull null
            if (it.videoId !in validVideoIds) return@mapNotNull null

            TagCrossRef(
                mediaId = it.videoId,
                tagId = tagId,
                mediaType = MediaType.VIDEO
            )
        }

        newTagsCrossRefRepository.insertTagCrossRefs(updatedVideoCrossRefs)
        oldVideoDb.close()

        Log.d(TAG, "Video transfer complete. ${videoTagIds.size} tags transferred. ${updatedVideoCrossRefs.size} cross refs transferred.")
    }

    private suspend fun syncMediaMetadataFromEmbedStore(
        mediaMetadataRepository: MediaMetadataRepository,
        store: FileEmbeddingStore,
        mediaType: MediaType
    ){
        val storedEmbeds = store.get()
        if(storedEmbeds.isEmpty()) return

        val newMetadataList = storedEmbeds.map{ MediaMetadata(id=it.id, dateAdded = it.date, type = mediaType) }
        mediaMetadataRepository.insert(newMetadataList)
        Log.d(TAG, "${mediaType.name} metadata sync complete. ${newMetadataList.size} synced.")

    }

    private suspend fun syncEmbedStoreAndMetadata(
        context: Context,
        store: FileEmbeddingStore,
        allowedDirs: List<Uri> = emptyList(),
        mediaMetadataRepository: MediaMetadataRepository,
        mediaType: MediaType
    ){
        val existingIdsFromMetadata = mediaMetadataRepository.getByType(mediaType).map { it.id }.toMutableSet()
        val existingIdsFromEmbedStore = store.get().map{it.id}.toMutableSet()
        if(existingIdsFromEmbedStore.isEmpty()) return

        val accessibleMediaIds = when(mediaType){
            MediaType.IMAGE -> MediaStoreHelper.queryImageIds(context, allowedDirs).toSet()
            MediaType.VIDEO -> MediaStoreHelper.queryVideoIds(context, allowedDirs).toSet()
        }

        // Purge stale media - Embed store used as source of truth
        val mediaToPurge = existingIdsFromEmbedStore.filterNot {it in accessibleMediaIds}
        if(mediaToPurge.isNotEmpty()){
            removeStaleMedia(mediaToPurge, mediaType, store = store, mediaMetadataRepository)
            existingIdsFromEmbedStore.removeAll(mediaToPurge)
            existingIdsFromMetadata.removeAll(mediaToPurge)
            store.save()
            Log.d(TAG, "${mediaType.name}: Removed ${mediaToPurge.size} stale items and save index file")
        }

        // Check if store date need syncing
        val storedEmbed = existingIdsFromEmbedStore.firstOrNull()?.let {store.get(listOf(it)).firstOrNull()}
        storedEmbed?.let {
            val mediaIdToDateMap = when(mediaType){
                MediaType.IMAGE -> MediaStoreHelper.queryImageIdDateMap(context.applicationContext)
                MediaType.VIDEO -> MediaStoreHelper.queryVideoIdDateMap(context.applicationContext)
            }

            if(it.date != mediaIdToDateMap[it.id] ){
                updateStoreDates(
                    context=context,
                    embeds = store.get(),
                    mediaTpe = mediaType
                )
                store.clear() // ensure internal consistency as precaution
            }
        }

        if(existingIdsFromEmbedStore.any{it !in existingIdsFromMetadata}) {
            syncMediaMetadataFromEmbedStore( mediaMetadataRepository, store, mediaType )
        }
    }

    private suspend fun updateStoreDates(
        context: Context,
        embeds: List<StoredEmbedding>,
        mediaTpe: MediaType
    ) {
        val tempFileName = when(mediaTpe){
            MediaType.IMAGE -> "${EmbeddingStoresFiles.IMAGE}.tmp"
            MediaType.VIDEO -> "${EmbeddingStoresFiles.VIDEO}.tmp"
        }
        val outputFileName =  when(mediaTpe){
            MediaType.IMAGE -> EmbeddingStoresFiles.IMAGE
            MediaType.VIDEO -> EmbeddingStoresFiles.VIDEO
        }

        val tempFile = File(context.applicationContext.cacheDir, tempFileName)
        val tempStore = FileEmbeddingStore(tempFile, EMBED_DIM)

        val dateMap = when(mediaTpe){
            MediaType.IMAGE -> MediaStoreHelper.getImageToDateMap(context.applicationContext, embeds.map { it.id })
            MediaType.VIDEO -> MediaStoreHelper.getVideoToDateMap(context.applicationContext, embeds.map { it.id })
        }
        val updated = embeds.mapNotNull {
            val date = dateMap[it.id] ?: return@mapNotNull null
            it.copy(date = date)
        }
        tempStore.add(updated)

        val finalFile = File(context.applicationContext.filesDir, outputFileName)
        if (finalFile.exists()) finalFile.delete()
        tempFile.renameTo(finalFile)

        Log.d(TAG, "${mediaTpe.name}: Date sync completed successfully")
    }
}