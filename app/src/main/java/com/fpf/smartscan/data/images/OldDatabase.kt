package com.fpf.smartscan.data.images

import android.app.Application
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.fpf.smartscan.data.images.tags.ImageTag
import com.fpf.smartscan.data.images.tags.ImageTagCrossRef
import com.fpf.smartscan.data.images.tags.ImageTagCrossRefDao
import com.fpf.smartscan.data.images.tags.ImageTagDao
import java.io.File

@Database(entities = [ImageTag::class, ImageTagCrossRef::class], version = 2, exportSchema = false)
abstract class OldImageDB : RoomDatabase() {
    abstract fun imageTagCrossRefDao(): ImageTagCrossRefDao
    abstract fun tagDao(): ImageTagDao

    companion object {
        @Volatile
        private var INSTANCE: OldImageDB? = null

        fun getDatabase(application: Application): OldImageDB {
            val dbName = "image_tag_database"
            val dbFile = File(application.filesDir, dbName)
            val dbPath = application.getDatabasePath(dbName)

            if (dbFile.exists()) {
                Log.d("OldImageDB", "Database cache found, restoring...")
                dbFile.copyTo(dbPath, overwrite = true)
                dbFile.delete()
            }
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    application,
                    OldImageDB::class.java,
                    dbName
                ).setJournalMode(JournalMode.TRUNCATE)
                    .addMigrations(MIGRATION_1_2_IMAGE)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}


