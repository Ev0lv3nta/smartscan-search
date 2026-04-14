package com.fpf.smartscan.data.old.images

import android.app.Application
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

@Database(entities = [ImageTag::class, ImageTagCrossRef::class], version = 1, exportSchema = false)
abstract class ImageTagDatabase : RoomDatabase() {
    abstract fun imageTagCrossRefDao(): ImageTagCrossRefDao
    abstract fun tagDao(): ImageTagDao

    companion object {
        @Volatile
        private var INSTANCE: ImageTagDatabase? = null
        const val DB_NAME = "image_tag_database"

        fun close() {
            INSTANCE?.close()
            INSTANCE = null
        }

        fun getDatabase(application: Application): ImageTagDatabase {
            val dbFile = File(application.filesDir, DB_NAME)
            val dbPath = application.getDatabasePath(DB_NAME)

            if (dbFile.exists()) {
                Log.d("ImageTagDatabase", "Database cache found, restoring...")
                dbFile.copyTo(dbPath, overwrite = true)
                dbFile.delete()
            }
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    application,
                    ImageTagDatabase::class.java,
                    DB_NAME
                ).setJournalMode(JournalMode.TRUNCATE)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}


