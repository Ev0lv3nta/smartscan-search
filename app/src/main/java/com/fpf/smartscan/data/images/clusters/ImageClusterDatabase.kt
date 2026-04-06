package com.fpf.smartscan.data.images.clusters

import android.app.Application
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

@Database(entities = [ImageClusterMetadata::class, ImageClusterCrossRef::class], version = 1, exportSchema = false)
abstract class ImageClusterDatabase : RoomDatabase() {
    abstract fun imageClusterCrossRefDao(): ImageClusterCrossRefDao
    abstract fun imageClusterMetadataDao(): ImageClusterMetadataDao

    companion object {
        @Volatile
        private var INSTANCE: ImageClusterDatabase? = null

        fun getDatabase(application: Application): ImageClusterDatabase {
            val dbName = "image_cluster_database"
            val dbFile = File(application.filesDir, dbName)
            val dbPath = application.getDatabasePath(dbName)

            if (dbFile.exists()) {
                Log.d("VideoClusterDatabase", "Database cache found, restoring...")
                dbFile.copyTo(dbPath, overwrite = true)
                dbFile.delete()
            }
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    application,
                    ImageClusterDatabase::class.java,
                    dbName
                ).setJournalMode(JournalMode.TRUNCATE)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}


