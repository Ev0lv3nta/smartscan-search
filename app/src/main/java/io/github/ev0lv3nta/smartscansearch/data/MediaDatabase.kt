package io.github.ev0lv3nta.smartscansearch.data

import android.app.Application
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.github.ev0lv3nta.smartscansearch.data.clusters.ClusterCrossRef
import io.github.ev0lv3nta.smartscansearch.data.clusters.ClusterCrossRefDao
import io.github.ev0lv3nta.smartscansearch.data.clusters.MediaClusterMetadata
import io.github.ev0lv3nta.smartscansearch.data.clusters.ClusterMetadataDao
import io.github.ev0lv3nta.smartscansearch.data.metadata.MediaMetadata
import io.github.ev0lv3nta.smartscansearch.data.metadata.MediaMetadataDao
import io.github.ev0lv3nta.smartscansearch.data.migrations.MIGRATION_1_2
import io.github.ev0lv3nta.smartscansearch.data.migrations.MIGRATION_2_3
import io.github.ev0lv3nta.smartscansearch.data.migrations.MIGRATION_3_4
import io.github.ev0lv3nta.smartscansearch.data.tags.Tag
import io.github.ev0lv3nta.smartscansearch.data.tags.TagCrossRef
import io.github.ev0lv3nta.smartscansearch.data.tags.TagCrossRefDao
import io.github.ev0lv3nta.smartscansearch.data.tags.TagDao


@Database(
    entities = [
        MediaMetadata::class,
        MediaClusterMetadata::class,
        ClusterCrossRef::class,
        Tag::class,
        TagCrossRef::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(MediaTypeConverter::class)
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}