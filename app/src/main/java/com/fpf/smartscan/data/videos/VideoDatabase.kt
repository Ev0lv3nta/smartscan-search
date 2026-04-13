package com.fpf.smartscan.data.videos

import android.app.Application
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.fpf.smartscan.data.DBTransferHelper
import com.fpf.smartscan.data.videos.clusters.VideoClusterCrossRef
import com.fpf.smartscan.data.videos.clusters.VideoClusterCrossRefDao
import com.fpf.smartscan.data.videos.clusters.VideoClusterMetadata
import com.fpf.smartscan.data.videos.clusters.VideoClusterMetadataDao
import com.fpf.smartscan.data.videos.tags.VideoTag
import com.fpf.smartscan.data.videos.tags.VideoTagCrossRef
import com.fpf.smartscan.data.videos.tags.VideoTagCrossRefDao
import com.fpf.smartscan.data.videos.tags.VideoTagDao
import kotlinx.coroutines.runBlocking
import java.io.File

@Database(
    entities = [
        VideoClusterMetadata::class,
        VideoClusterCrossRef::class,
        VideoTag::class,
        VideoTagCrossRef::class
    ],
    version = 1,
    exportSchema = false
)
abstract class VideoDatabase : RoomDatabase() {

    abstract fun videoClusterCrossRefDao(): VideoClusterCrossRefDao
    abstract fun videoClusterMetadataDao(): VideoClusterMetadataDao

    abstract fun videoTagCrossRefDao(): VideoTagCrossRefDao
    abstract fun tagDao(): VideoTagDao

    companion object {
        @Volatile
        private var INSTANCE: VideoDatabase? = null

        const val OLD_DB_NAME = "video_tag_database"
        const val DB_NAME = "video_database"

        fun close() {
            INSTANCE?.close()
            INSTANCE = null
        }

        fun getDatabase(application: Application): VideoDatabase {
            return INSTANCE ?: synchronized(this) {

                val dbFile = File(application.filesDir, DB_NAME)
                val dbPath = application.getDatabasePath(DB_NAME)

                // Restore cached DB
                if (dbFile.exists()) {
                    Log.d("VideoDatabase", "Database cache found, restoring...")
                    dbFile.copyTo(dbPath, overwrite = true)
                    dbFile.delete()
                }

                // Build DB first
                val instance = Room.databaseBuilder(
                    application,
                    VideoDatabase::class.java,
                    DB_NAME
                ).setJournalMode(JournalMode.TRUNCATE)
                    .build()

                INSTANCE = instance

                val oldTagDbFile = application.getDatabasePath(OLD_DB_NAME)
                if (oldTagDbFile.exists()) {
                    Log.d("VideoDatabase", "Old tag DB detected, transferring...")

                    runBlocking {
                        DBTransferHelper.transferVideos(
                            application = application,
                            newDb = instance
                        )
                    }
                    oldTagDbFile.delete()
                }
                instance
            }
        }
    }
}