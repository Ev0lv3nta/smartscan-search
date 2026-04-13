package com.fpf.smartscan.data.images

import android.app.Application
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.fpf.smartscan.data.DBTransferHelper
import com.fpf.smartscan.data.images.clusters.ImageClusterCrossRef
import com.fpf.smartscan.data.images.clusters.ImageClusterCrossRefDao
import com.fpf.smartscan.data.images.clusters.ImageClusterMetadata
import com.fpf.smartscan.data.images.clusters.ImageClusterMetadataDao
import com.fpf.smartscan.data.images.tags.ImageTag
import com.fpf.smartscan.data.images.tags.ImageTagCrossRef
import com.fpf.smartscan.data.images.tags.ImageTagCrossRefDao
import com.fpf.smartscan.data.images.tags.ImageTagDao
import kotlinx.coroutines.runBlocking
import java.io.File

@Database(
    entities = [
        ImageClusterMetadata::class,
        ImageClusterCrossRef::class,
        ImageTag::class,
        ImageTagCrossRef::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ImageDatabase : RoomDatabase() {

    abstract fun imageClusterCrossRefDao(): ImageClusterCrossRefDao
    abstract fun imageClusterMetadataDao(): ImageClusterMetadataDao

    abstract fun imageTagCrossRefDao(): ImageTagCrossRefDao
    abstract fun tagDao(): ImageTagDao

    companion object {
        @Volatile
        private var INSTANCE: ImageDatabase? = null

        const val OLD_DB_NAME = "image_tag_database"
        const val DB_NAME = "image_database"

        fun close() {
            INSTANCE?.close()
            INSTANCE = null
        }

        fun getDatabase(application: Application): ImageDatabase {
            return INSTANCE ?: synchronized(this) {

                val dbFile = File(application.filesDir, DB_NAME)
                val dbPath = application.getDatabasePath(DB_NAME)

                // Restore cached DB
                if (dbFile.exists()) {
                    Log.d("ImageDatabase", "Database cache found, restoring...")
                    dbFile.copyTo(dbPath, overwrite = true)
                    dbFile.delete()
                }

                val instance = Room.databaseBuilder(
                    application,
                    ImageDatabase::class.java,
                    DB_NAME
                ).setJournalMode(JournalMode.TRUNCATE)
                    .build()

                INSTANCE = instance

                // Run migration AFTER DB is ready
                val oldTagDbFile = application.getDatabasePath(OLD_DB_NAME)
                if (oldTagDbFile.exists()) {
                    Log.d("ImageDatabase", "Old tag DB detected, transferring...")

                    runBlocking {
                        DBTransferHelper.transferImages(
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