package com.fpf.smartscan.data.videos

import android.app.Application
import androidx.room.*

@Database(entities = [VideoMetadata::class], version = 1, exportSchema = false)
abstract class VideoMetadataDatabase : RoomDatabase() {
    abstract fun videoMetadataDao(): VideoMetadataDao

    companion object {
        @Volatile
        private var INSTANCE: VideoMetadataDatabase? = null

        fun getDatabase(application: Application): VideoMetadataDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    application,
                    VideoMetadataDatabase::class.java,
                    "video_metadata_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

