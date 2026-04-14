package com.fpf.smartscan.data

import android.app.Application
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

//
//object DBTransferHelper {
//    const val TAG = "DBTransferHelper"
//
//    suspend fun <T: MediaTag, K: MediaTagCrossRef>transfer(
//        oldTagsRepository: MediaTagRepository<T>,
//        oldTagsCrossRefRepository: MediaTagCrossRefRepository<K>,
//        newTagsRepository: MediaTagRepository<T>,
//        newTagsCrossRefRepository: MediaTagCrossRefRepository<K>,
//    ) {
//        val tags = oldTagsRepository.getAllTags()
//        val crossRefs = oldTagsCrossRefRepository.getAllCrossRefs()
//
//        newTagsRepository.insertTags(tags)
//        newTagsCrossRefRepository.upsertTagCrossRefs(crossRefs)
//
//        Log.d(TAG, "Transfer complete. ${tags.size} tags transferred. ${crossRefs.size} cross refs transferred.")
//    }
//
//    suspend fun transferImages(application: Application, newDb: VideoMediaDatabase)= withContext(
//        Dispatchers.IO) {
////        val oldDb = OldImageDB.getDatabase(application)
////        val oldTagsRepository = TagRepository(oldDb.tagDao())
////        val oldTagsCrossRefRepository = TagCrossRefRepository( oldDb.imageTagCrossRefDao())
////
////        val newTagsRepository = TagRepository(newDb.tagDao())
////        val newTagsCrossRefRepository = TagCrossRefRepository( newDb.imageTagCrossRefDao())
////
////        transfer(
////            oldTagsRepository=oldTagsRepository,
////            oldTagsCrossRefRepository=oldTagsCrossRefRepository,
////            newTagsRepository = newTagsRepository,
////            newTagsCrossRefRepository = newTagsCrossRefRepository,
////        )
//    }
//
//    suspend fun transferVideos(application: Application, newDb: VideoMediaDatabase)= withContext(
//        Dispatchers.IO) {
//        val oldDb = OldVideoDB.getDatabase(application)
//        val oldTagsRepository = VideoTagRepository(oldDb.tagDao())
//        val oldTagsCrossRefRepository = VideoTagCrossRefRepository( oldDb.videoTagCrossRefDao())
//
//        val newTagsRepository = VideoTagRepository(newDb.tagDao())
//        val newTagsCrossRefRepository = VideoTagCrossRefRepository( newDb.videoTagCrossRefDao())
//
//        transfer(
//            oldTagsRepository=oldTagsRepository,
//            oldTagsCrossRefRepository=oldTagsCrossRefRepository,
//            newTagsRepository = newTagsRepository,
//            newTagsCrossRefRepository = newTagsCrossRefRepository,
//        )
//    }
//}