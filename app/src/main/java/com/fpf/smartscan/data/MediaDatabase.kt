package com.fpf.smartscan.data

import android.app.Application
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.fpf.smartscan.data.clusters.ClusterCrossRef
import com.fpf.smartscan.data.clusters.ClusterCrossRefDao
import com.fpf.smartscan.data.clusters.MediaClusterMetadata
import com.fpf.smartscan.data.clusters.ClusterMetadataDao
import com.fpf.smartscan.data.metadata.MediaMetadata
import com.fpf.smartscan.data.metadata.MediaMetadataDao
import com.fpf.smartscan.data.migrations.MIGRATION_1_2
import com.fpf.smartscan.data.migrations.MIGRATION_2_3
import com.fpf.smartscan.data.tags.Tag
import com.fpf.smartscan.data.tags.TagCrossRef
import com.fpf.smartscan.data.tags.TagCrossRefDao
import com.fpf.smartscan.data.tags.TagDao


@Database(
    entities = [
        MediaMetadata::class,
        MediaClusterMetadata::class,
        ClusterCrossRef::class,
        Tag::class,
        TagCrossRef::class
    ],
    version = 3,
    exportSchema = false
)
abstract class MediaDatabase : RoomDatabase() {

    abstract fun clusterCrossRefDao(): ClusterCrossRefDao
    abstract fun clusterMetadataDao(): ClusterMetadataDao

    abstract fun metadataDao(): MediaMetadataDao

    abstract fun tagCrossRefDao(): TagCrossRefDao
    abstract fun tagDao(): TagDao

    companion object {
        @Volatile
        private var INSTANCE: MediaDatabase? = null

        const val OLD_DB_IMAGE_NAME = "image_tag_database"
        const val OLD_DB_VIDEO_NAME = "video_tag_database"
        const val DB_NAME = "media_database"

        const val TAG = "MediaDatabase"

        fun close() {
            INSTANCE?.close()
            INSTANCE = null
        }

        fun getDatabase(application: Application): MediaDatabase {
            return INSTANCE ?: synchronized(this) {

                // Build DB first
                val instance = Room.databaseBuilder(
                    application,
                    MediaDatabase::class.java,
                    DB_NAME
                ).setJournalMode(JournalMode.TRUNCATE)
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}