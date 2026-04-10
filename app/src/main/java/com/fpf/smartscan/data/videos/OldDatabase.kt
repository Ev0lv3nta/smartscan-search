package com.fpf.smartscan.data.videos

import android.app.Application
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.fpf.smartscan.data.videos.tags.VideoTag
import com.fpf.smartscan.data.videos.tags.VideoTagCrossRef
import com.fpf.smartscan.data.videos.tags.VideoTagCrossRefDao
import com.fpf.smartscan.data.videos.tags.VideoTagDao
import java.io.File


@Database(entities = [VideoTag::class, VideoTagCrossRef::class], version = 2, exportSchema = false)
abstract class OldVideoDB : RoomDatabase() {
    abstract fun videoTagCrossRefDao(): VideoTagCrossRefDao
    abstract fun tagDao(): VideoTagDao
    companion object {
        @Volatile
        private var INSTANCE: OldVideoDB? = null

        fun getDatabase(application: Application): OldVideoDB {
            val dbName = "video_tag_database"
            val dbFile = File(application.filesDir, dbName)
            val dbPath = application.getDatabasePath(dbName)

            if (dbFile.exists()) {
                Log.d("OldVideoDB", "Database cache found, restoring...")
                dbFile.copyTo(dbPath, overwrite = true)
                dbFile.delete()
            }
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    application,
                    OldVideoDB::class.java,
                    dbName
                ).setJournalMode(JournalMode.TRUNCATE)
                    .addMigrations(MIGRATION_1_2_VIDEO)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

