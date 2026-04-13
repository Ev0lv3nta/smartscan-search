package com.fpf.smartscan.data.images

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2_IMAGE = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {

        // image_tag rebuild
        db.execSQL("ALTER TABLE image_tag RENAME TO tmp_image_tag;")

        db.execSQL("""
            CREATE TABLE image_tag (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                lastUsedAt INTEGER
            )
        """.trimIndent())

        db.execSQL("""
            INSERT INTO image_tag(name, lastUsedAt)
            SELECT name, lastUsedAt FROM tmp_image_tag;
        """.trimIndent())

        db.execSQL("DROP TABLE tmp_image_tag;")

        db.execSQL("""
            CREATE UNIQUE INDEX IF NOT EXISTS index_image_tag_name
            ON image_tag(name);
        """.trimIndent())

        // image_tag_crossref rebuild
        db.execSQL("ALTER TABLE image_tag_crossref RENAME TO tmp_image_tag_crossref;")

        db.execSQL("""
            CREATE TABLE image_tag_crossref (
                mediaId INTEGER NOT NULL,
                tagId INTEGER NOT NULL,
                PRIMARY KEY(mediaId, tagId),
                FOREIGN KEY(tagId) REFERENCES image_tag(id) ON DELETE CASCADE
            )
        """.trimIndent())

        db.execSQL("""
            INSERT INTO image_tag_crossref(mediaId, tagId)
            SELECT c.imageId, t.id
            FROM tmp_image_tag_crossref c
            JOIN image_tag t ON t.name = c.tag;
        """.trimIndent())

        db.execSQL("DROP TABLE tmp_image_tag_crossref;")

        db.execSQL("""
            CREATE INDEX IF NOT EXISTS index_image_tag_crossref_tagId
            ON image_tag_crossref(tagId);
        """.trimIndent())
    }
}