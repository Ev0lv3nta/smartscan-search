package com.fpf.smartscan.data

import android.app.Application
import android.util.Log
import com.fpf.smartscan.data.images.tags.ImageTagCrossRefRepository
import com.fpf.smartscan.data.images.OldImageDB
import com.fpf.smartscan.data.images.tags.ImageTagRepository
import com.fpf.smartscan.data.images.ImageDatabase
import com.fpf.smartscan.data.videos.tags.VideoTagCrossRefRepository
import com.fpf.smartscan.data.videos.OldVideoDB
import com.fpf.smartscan.data.videos.VideoDatabase
import com.fpf.smartscan.data.videos.tags.VideoTagRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


object DBTransferHelper {
    const val TAG = "DBTransferHelper"

    suspend fun transfer(
        oldTagsRepository: MediaTagRepository,
        oldTagsCrossRefRepository: MediaTagCrossRefRepository,
        newTagsRepository: MediaTagRepository,
        newTagsCrossRefRepository: MediaTagCrossRefRepository,
    ) {
        val tags = oldTagsRepository.getAllTags()
        val crossRefs = oldTagsCrossRefRepository.getAllCrossRefs()

        newTagsRepository.insertTags(tags)
        newTagsCrossRefRepository.upsertTagCrossRefs(crossRefs)

        Log.d(TAG, "Transfer complete. ${tags.size} tags transferred. ${crossRefs.size} cross refs transferred.")
    }

    suspend fun transferImages(application: Application, newDb: ImageDatabase)= withContext(
        Dispatchers.IO) {
        val oldDb = OldImageDB.getDatabase(application)
        val oldTagsRepository = ImageTagRepository(oldDb.tagDao())
        val oldTagsCrossRefRepository = ImageTagCrossRefRepository( oldDb.imageTagCrossRefDao())

        val newTagsRepository = ImageTagRepository(newDb.tagDao())
        val newTagsCrossRefRepository = ImageTagCrossRefRepository( newDb.imageTagCrossRefDao())

        transfer(
            oldTagsRepository=oldTagsRepository,
            oldTagsCrossRefRepository=oldTagsCrossRefRepository,
            newTagsRepository = newTagsRepository,
            newTagsCrossRefRepository = newTagsCrossRefRepository,
        )
    }

    suspend fun transferVideos(application: Application, newDb: VideoDatabase)= withContext(
        Dispatchers.IO) {
        val oldDb = OldVideoDB.getDatabase(application)
        val oldTagsRepository = VideoTagRepository(oldDb.tagDao())
        val oldTagsCrossRefRepository = VideoTagCrossRefRepository( oldDb.videoTagCrossRefDao())

        val newTagsRepository = VideoTagRepository(newDb.tagDao())
        val newTagsCrossRefRepository = VideoTagCrossRefRepository( newDb.videoTagCrossRefDao())

        transfer(
            oldTagsRepository=oldTagsRepository,
            oldTagsCrossRefRepository=oldTagsCrossRefRepository,
            newTagsRepository = newTagsRepository,
            newTagsCrossRefRepository = newTagsCrossRefRepository,
        )
    }
}