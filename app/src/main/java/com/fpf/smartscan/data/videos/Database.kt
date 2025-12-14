package com.fpf.smartscan.data.videos

import android.app.Application
import androidx.room.*

@Database(entities = [VideoTag::class], version = 1, exportSchema = false)
abstract class VideoTagsDatabase : RoomDatabase() {
    abstract fun videoTagDao(): VideoTagDao

    companion object {
        @Volatile
        private var INSTANCE: VideoTagsDatabase? = null

        fun getDatabase(application: Application): VideoTagsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    application,
                    VideoTagsDatabase::class.java,
                    "video_tags_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

