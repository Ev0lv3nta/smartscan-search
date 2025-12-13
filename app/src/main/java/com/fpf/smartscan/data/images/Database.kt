package com.fpf.smartscan.data.images

import android.app.Application
import androidx.room.*

@Database(entities = [ImageMetadata::class], version = 1, exportSchema = false)
abstract class ImageMetadataDatabase : RoomDatabase() {
    abstract fun imageMetadataDao(): ImageMetadataDao

    companion object {
        @Volatile
        private var INSTANCE: ImageMetadataDatabase? = null

        fun getDatabase(application: Application): ImageMetadataDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    application,
                    ImageMetadataDatabase::class.java,
                    "image_metadata_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}


