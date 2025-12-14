package com.fpf.smartscan.data.images

import android.app.Application
import androidx.room.*

@Database(entities = [ImageTag::class], version = 1, exportSchema = false)
abstract class ImageTagsDatabase : RoomDatabase() {
    abstract fun imageTagsDao(): ImageTagDao

    companion object {
        @Volatile
        private var INSTANCE: ImageTagsDatabase? = null

        fun getDatabase(application: Application): ImageTagsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    application,
                    ImageTagsDatabase::class.java,
                    "image_tags_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}


