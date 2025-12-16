package com.fpf.smartscan.data.videos

import android.app.Application
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase


@Database(entities = [VideoTag::class, VideoTagCrossRef::class], version = 1, exportSchema = false)
abstract class VideoTagDatabase : RoomDatabase() {
    abstract fun videoTagDao(): VideoTagCrossRefDao
    abstract fun tagDao(): VideoTagDao
    companion object {
        @Volatile
        private var INSTANCE: VideoTagDatabase? = null

        fun getDatabase(application: Application): VideoTagDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    application,
                    VideoTagDatabase::class.java,
                    "video_tag_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

