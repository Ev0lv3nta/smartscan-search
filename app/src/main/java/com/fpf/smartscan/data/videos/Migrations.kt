package com.fpf.smartscan.data.videos

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2_VIDEO = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {

        // video_tag rebuild
        db.execSQL("ALTER TABLE video_tag RENAME TO tmp_video_tag;")

        db.execSQL("""
            CREATE TABLE video_tag (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                lastUsedAt INTEGER
            )
        """.trimIndent())

        db.execSQL("""
            INSERT INTO video_tag(name, lastUsedAt)
            SELECT name, lastUsedAt FROM tmp_video_tag;
        """.trimIndent())

        db.execSQL("DROP TABLE tmp_video_tag;")

        db.execSQL("""
            CREATE UNIQUE INDEX IF NOT EXISTS index_video_tag_name
            ON video_tag(name);
        """.trimIndent())

        // video_tag_crossref rebuild
        db.execSQL("ALTER TABLE video_tag_crossref RENAME TO tmp_video_tag_crossref;")

        db.execSQL("""
            CREATE TABLE video_tag_crossref (
                mediaId INTEGER NOT NULL,
                tagId INTEGER NOT NULL,
                PRIMARY KEY(mediaId, tagId),
                FOREIGN KEY(tagId) REFERENCES video_tag(id) ON DELETE CASCADE
            )
        """.trimIndent())

        db.execSQL("""
            INSERT INTO video_tag_crossref(mediaId, tagId)
            SELECT c.videoId, t.id
            FROM tmp_video_tag_crossref c
            JOIN video_tag t ON t.name = c.tag;
        """.trimIndent())

        db.execSQL("DROP TABLE tmp_video_tag_crossref;")

        db.execSQL("""
            CREATE INDEX IF NOT EXISTS index_video_tag_crossref_tagId
            ON video_tag_crossref(tagId);
        """.trimIndent())
    }
}