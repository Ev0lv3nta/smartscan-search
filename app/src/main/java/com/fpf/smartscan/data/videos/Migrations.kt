package com.fpf.smartscan.data.videos

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2_VIDEO = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE video_tag RENAME TO tmp_video_tag;")
        db.execSQL("""
            CREATE TABLE video_tag (
                name TEXT NOT NULL PRIMARY KEY,
                lastUsedAt INTEGER
            )
        """.trimIndent())
        db.execSQL("""
            INSERT INTO video_tag(name, lastUsedAt)
            SELECT name, lastUsedAt FROM tmp_video_tag;
        """.trimIndent())
        db.execSQL("DROP TABLE tmp_video_tag;")

        db.execSQL("ALTER TABLE video_tag_crossref RENAME TO tmp_video_tag_crossref;")
        db.execSQL("""
            CREATE TABLE video_tag_crossref (
                mediaId INTEGER NOT NULL,
                tag TEXT NOT NULL,
                PRIMARY KEY(mediaId, tag),
                FOREIGN KEY(tag) REFERENCES video_tag(name) ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("""
            INSERT INTO video_tag_crossref(mediaId, tag)
            SELECT videoId, tag FROM tmp_video_tag_crossref;
        """.trimIndent())
        db.execSQL("DROP TABLE tmp_video_tag_crossref;")

        db.execSQL("CREATE INDEX IF NOT EXISTS index_video_tag_crossref_tag ON video_tag_crossref(tag);")
    }
}