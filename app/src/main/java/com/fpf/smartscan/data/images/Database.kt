package com.fpf.smartscan.data.images

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

        fun getDatabase(application: Application): ImageTagDatabase {
            val dbName = "image_tag_database"
            val dbFile = File(application.filesDir, dbName)
            val dbPath = application.getDatabasePath(dbName)

            if (dbFile.exists()) {
                Log.d("ImageTagDatabase", "Database cache found, restoring...")
                dbFile.copyTo(dbPath, overwrite = true)
                dbFile.delete()
            }
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    application,
                    ImageTagDatabase::class.java,
                    dbName
                ).setJournalMode(JournalMode.TRUNCATE)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}


