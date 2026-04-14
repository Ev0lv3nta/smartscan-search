package com.fpf.smartscan.data

import android.app.Application
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.fpf.smartscan.data.clusters.ClusterCrossRef
import com.fpf.smartscan.data.clusters.ClusterCrossRefDao
import com.fpf.smartscan.data.clusters.MediaClusterMetadata
import com.fpf.smartscan.data.clusters.ClusterMetadataDao
import com.fpf.smartscan.data.tags.Tag
import com.fpf.smartscan.data.tags.TagCrossRef
import com.fpf.smartscan.data.tags.TagCrossRefDao
import com.fpf.smartscan.data.tags.TagDao
import kotlinx.coroutines.runBlocking
import java.io.File

@Database(
    entities = [
        MediaClusterMetadata::class,
        ClusterCrossRef::class,
        Tag::class,
        TagCrossRef::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MediaDatabase : RoomDatabase() {

    abstract fun clusterCrossRefDao(): ClusterCrossRefDao
    abstract fun clusterMetadataDao(): ClusterMetadataDao

    abstract fun tagCrossRefDao(): TagCrossRefDao
    abstract fun tagDao(): TagDao

    companion object {
        @Volatile
        private var INSTANCE: MediaDatabase? = null

        const val OLD_DB_IMAGE_NAME = "image_tag_database"
        const val OLD_DB_VIDEO_NAME = "video_tag_database"
        const val DB_NAME = "media_database"

        const val TAG = "MediaDatabase"

        fun close() {
            INSTANCE?.close()
            INSTANCE = null
        }

        fun getDatabase(application: Application): MediaDatabase {
            return INSTANCE ?: synchronized(this) {

                val dbFile = File(application.filesDir, DB_NAME)
                val dbPath = application.getDatabasePath(DB_NAME)

                // Restore cached DB
                if (dbFile.exists()) {
                    Log.d("MediaDatabase", "MediaDatabase cache found, restoring...")
                    dbFile.copyTo(dbPath, overwrite = true)
                    dbFile.delete()
                }

                // Build DB first
                val instance = Room.databaseBuilder(
                    application,
                    MediaDatabase::class.java,
                    DB_NAME
                ).setJournalMode(JournalMode.TRUNCATE)
                    .build()

                INSTANCE = instance


                val oldImageTagDbStagedFile = File(application.filesDir, OLD_DB_IMAGE_NAME)
                val oldVideoTagDbStagedFile = File(application.filesDir, OLD_DB_VIDEO_NAME)

                if (oldImageTagDbStagedFile.exists() && oldVideoTagDbStagedFile.exists()) {
                    val oldImageTagDbPath = application.getDatabasePath(OLD_DB_IMAGE_NAME)
                    val oldVideoTagDbPath = application.getDatabasePath(OLD_DB_VIDEO_NAME)

                    Log.d(TAG, "Old DB detected, transferring...")

                    oldImageTagDbStagedFile.copyTo(oldImageTagDbPath, overwrite = true)
                    oldVideoTagDbStagedFile.copyTo(oldVideoTagDbPath, overwrite = true)

                    runBlocking {
                        DBTransferHelper.transfer(
                            application = application,
                            newDb = instance
                        )
                    }
                    oldImageTagDbStagedFile.delete()
                    oldVideoTagDbStagedFile.delete()
                    oldImageTagDbPath.delete()
                    oldVideoTagDbPath.delete()
                }
                instance
            }
        }
    }
}