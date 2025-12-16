package com.fpf.smartscan.data.videos

import android.app.Application
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase


@Database(entities = [VideoTag::class, VideoTagCrossRef::class], version = 1, exportSchema = false)
abstract class VideoTagsDatabase : RoomDatabase() {
    abstract fun videoTagDao(): VideoTagCrossRefDao
    abstract fun tagDao(): VideoTagDao
    companion object {
        @Volatile
        private var INSTANCE: VideoTagsDatabase? = null

        fun getDatabase(application: Application): VideoTagsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    application,
                    VideoTagsDatabase::class.java,
                    "video_tag_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

