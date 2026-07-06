package com.fpf.smartscan.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            ALTER TABLE media_metadata
            ADD COLUMN description TEXT
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE media_cluster_crossref_new (
                mediaId INTEGER NOT NULL,
                clusterId INTEGER NOT NULL,
                PRIMARY KEY(mediaId),
                FOREIGN KEY(clusterId)
                    REFERENCES cluster_metadata(clusterId)
                    ON DELETE CASCADE,
                FOREIGN KEY(mediaId)
                    REFERENCES media_metadata(id)
                    ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO media_cluster_crossref_new (mediaId, clusterId)
            SELECT mediaId, clusterId
            FROM media_cluster_crossref
            """.trimIndent()
        )

        db.execSQL("DROP TABLE media_cluster_crossref")
        db.execSQL(
            "ALTER TABLE media_cluster_crossref_new RENAME TO media_cluster_crossref"
        )

        db.execSQL(
            """
            CREATE INDEX index_media_cluster_crossref_clusterId
            ON media_cluster_crossref(clusterId)
            """.trimIndent()
        )
    }
}