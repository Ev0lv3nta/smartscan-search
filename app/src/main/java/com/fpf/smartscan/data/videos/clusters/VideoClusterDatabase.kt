package com.fpf.smartscan.data.videos.clusters

import android.app.Application
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

@Database(entities = [VideoClusterMetadata::class, VideoClusterCrossRef::class], version = 1, exportSchema = false)
abstract class VideoClusterDatabase : RoomDatabase() {
    abstract fun videoClusterCrossRefDao(): VideoClusterCrossRefDao
    abstract fun videoClusterMetadataDao(): VideoClusterMetadataDao

    companion object {
        @Volatile
        private var INSTANCE: VideoClusterDatabase? = null

        fun getDatabase(application: Application): VideoClusterDatabase {
            val dbName = "video_cluster_database"
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
                    VideoClusterDatabase::class.java,
                    dbName
                ).setJournalMode(JournalMode.TRUNCATE)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}


