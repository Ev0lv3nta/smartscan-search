package com.fpf.smartscan.data.images

import android.app.Application
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ImageTag::class, ImageTagCrossRef::class], version = 1, exportSchema = false)
abstract class TagDatabase : RoomDatabase() {
    abstract fun imageTagCrossRefDao(): ImageTagCrossRefDao
    abstract fun tagDao(): ImageTagDao

    companion object {
        @Volatile
        private var INSTANCE: TagDatabase? = null

        fun getDatabase(application: Application): TagDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    application,
                    TagDatabase::class.java,
                    "image_tag_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}


