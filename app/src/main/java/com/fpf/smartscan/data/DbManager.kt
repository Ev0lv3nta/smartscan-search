package com.fpf.smartscan.data

import android.app.Application
import android.util.Log
import com.fpf.smartscan.data.MediaDatabase.Companion.DB_NAME
import com.fpf.smartscan.data.MediaDatabase.Companion.OLD_DB_IMAGE_NAME
import com.fpf.smartscan.data.MediaDatabase.Companion.OLD_DB_VIDEO_NAME
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
import com.fpf.smartscan.media.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File


object DbManager {
    const val TAG = "DbManager"

    fun checkCachedDb(application: Application): File?{
        val cachedDB = File(application.filesDir, DB_NAME)
        return if(cachedDB.exists()) cachedDB else null
    }

    fun checkOldCachedImageDb(application: Application): File?{
        val cachedDB = File(application.filesDir, OLD_DB_IMAGE_NAME)
        return if(cachedDB.exists()) cachedDB else null
    }

    fun checkOldCachedVideoDb(application: Application): File?{
        val cachedDB = File(application.filesDir, OLD_DB_VIDEO_NAME)
        return if(cachedDB.exists()) cachedDB else null
    }

    fun restoreDbFromCache(application: Application, cachedDbFile: File){
        if(!cachedDbFile.exists()) return
        val dbPath = application.getDatabasePath(DB_NAME)
        Log.d(TAG, "Database cache found, restoring...")
        cachedDbFile.copyTo(dbPath, overwrite = true)
        cachedDbFile.delete()
    }

    suspend fun transferIfNeeded(application: Application, oldImageTagDbCachedFile: File, oldVideoTagDbCachedFile: File, newDb: MediaDatabase){
        val oldImageTagDbPath = application.getDatabasePath(OLD_DB_IMAGE_NAME)
        val oldVideoTagDbPath = application.getDatabasePath(OLD_DB_VIDEO_NAME)
        val isTransferNeeded = oldImageTagDbCachedFile.exists() && oldVideoTagDbCachedFile.exists()
        if (!isTransferNeeded) return


        Log.d(MediaDatabase.Companion.TAG, "Old DB detected, transferring...")

        oldImageTagDbCachedFile.copyTo(oldImageTagDbPath, overwrite = true)
        oldVideoTagDbCachedFile.copyTo(oldVideoTagDbPath, overwrite = true)

        transfer(
            application = application,
            newDb = newDb
        )
        oldImageTagDbCachedFile.delete()
        oldVideoTagDbCachedFile.delete()
        oldImageTagDbPath.delete()
        oldVideoTagDbPath.delete()
    }

    private suspend fun transfer(application: Application, newDb: MediaDatabase) = withContext(Dispatchers.IO) {
        val newTagsRepository = TagRepository(newDb.tagDao())
        val newTagsCrossRefRepository = TagCrossRefRepository( newDb.tagCrossRefDao())

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

        val updatedImageCrossRefs = imageCrossRefs.mapNotNull{
            val tagId = nameToImageTagId[it.tag]?: return@mapNotNull  null
            TagCrossRef(mediaId=it.imageId, tagId = tagId, type = MediaType.IMAGE)}

        newTagsCrossRefRepository.upsertTagCrossRefs(updatedImageCrossRefs)
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

        val updatedVideoCrossRefs = videoCrossRefs.mapNotNull{
            val tagId = nameToVideoTagId[it.tag]?: return@mapNotNull  null
            TagCrossRef(mediaId=it.videoId, tagId = tagId, type = MediaType.VIDEO)}

        newTagsCrossRefRepository.upsertTagCrossRefs(updatedVideoCrossRefs)
        oldVideoDb.close()

        Log.d(TAG, "Video transfer complete. ${videoTagIds.size} tags transferred. ${updatedVideoCrossRefs.size} cross refs transferred.")
    }
}