package com.fpf.smartscan.data.tags

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "tag_crossref",
    primaryKeys = ["mediaId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = Tag::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tagId")]
)
data class TagCrossRef(
    val mediaId: Long,
    val tagId: Long
)
