package com.fpf.smartscan.data.videos

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

        fun getDatabase(application: Application): VideoTagDatabase {
            val dbName = "video_tag_database"
            val dbFile = File(application.filesDir, dbName)
            val dbPath = application.getDatabasePath(dbName)

            if (dbFile.exists()) {
                Log.d("VideoTagDatabase", "Database cache found, restoring...")
                dbFile.copyTo(dbPath, overwrite = true)
                dbFile.delete()
            }
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    application,
                    VideoTagDatabase::class.java,
                    dbName
                ).setJournalMode(JournalMode.TRUNCATE).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

