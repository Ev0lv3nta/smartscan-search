package com.fpf.smartscan.data.images

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2_IMAGE = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE image_tag RENAME TO tmp_image_tag;")
        db.execSQL("""
            CREATE TABLE image_tag (
                name TEXT NOT NULL PRIMARY KEY,
                lastUsedAt INTEGER
            )
        """.trimIndent())
        db.execSQL("""
            INSERT INTO image_tag(name, lastUsedAt)
            SELECT name, lastUsedAt FROM tmp_image_tag;
        """.trimIndent())
        db.execSQL("DROP TABLE tmp_image_tag;")

        db.execSQL("ALTER TABLE image_tag_crossref RENAME TO tmp_image_tag_crossref;")
        db.execSQL("""
            CREATE TABLE image_tag_crossref (
                mediaId INTEGER NOT NULL,
                tag TEXT NOT NULL,
                PRIMARY KEY(mediaId, tag),
                FOREIGN KEY(tag) REFERENCES image_tag(name) ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("""
            INSERT INTO image_tag_crossref(mediaId, tag)
            SELECT imageId, tag FROM tmp_image_tag_crossref;
        """.trimIndent())
        db.execSQL("DROP TABLE tmp_image_tag_crossref;")

        db.execSQL("CREATE INDEX IF NOT EXISTS index_image_tag_crossref_tag ON image_tag_crossref(tag);")
    }
}