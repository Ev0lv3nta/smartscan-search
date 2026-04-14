package com.fpf.smartscan.data.old.videos

import android.app.Application
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File


@Database(entities = [VideoTag::class, VideoTagCrossRef::class], version = 1, exportSchema = false)
abstract class VideoTagDatabase : RoomDatabase() {
    abstract fun videoTagCrossRefDao(): VideoTagCrossRefDao
    abstract fun tagDao(): VideoTagDao
    companion object {
        @Volatile
        private var INSTANCE: VideoTagDatabase? = null

        const val DB_NAME = "video_tag_database"


        fun close() {
            INSTANCE?.close()
            INSTANCE = null
        }

        fun getDatabase(application: Application): VideoTagDatabase {
            val dbFile = File(application.filesDir, DB_NAME)
            val dbPath = application.getDatabasePath(DB_NAME)

            if (dbFile.exists()) {
                Log.d("VideoTagDatabase", "Database cache found, restoring...")
                dbFile.copyTo(dbPath, overwrite = true)
                dbFile.delete()
            }
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    application,
                    VideoTagDatabase::class.java,
                    DB_NAME
                ).setJournalMode(JournalMode.TRUNCATE).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

