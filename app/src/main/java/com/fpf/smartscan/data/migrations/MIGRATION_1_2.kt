package com.fpf.smartscan.data.migrations


import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS media_metadata (
                id INTEGER NOT NULL PRIMARY KEY,
                type TEXT NOT NULL,
                dateAdded INTEGER NOT NULL
            )
        """)

        db.execSQL("CREATE INDEX IF NOT EXISTS index_media_metadata_dateAdded ON media_metadata(dateAdded)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_media_metadata_type ON media_metadata(type)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_media_metadata_type_dateAdded ON media_metadata(type, dateAdded)")
    }
}