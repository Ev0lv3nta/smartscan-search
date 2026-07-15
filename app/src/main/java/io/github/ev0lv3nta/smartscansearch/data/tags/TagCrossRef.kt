package io.github.ev0lv3nta.smartscansearch.data.tags

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import io.github.ev0lv3nta.smartscansearch.data.metadata.MediaMetadata
import io.github.ev0lv3nta.smartscansearch.media.MediaType

@Entity(
    tableName = "tag_crossref",
    primaryKeys = ["mediaId", "mediaType", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = Tag::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MediaMetadata::class,
            parentColumns = ["id", "type"],
            childColumns = ["mediaId", "mediaType"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["tagId"]),
    ]
)
data class TagCrossRef(
    val mediaId: Long,
    val mediaType: MediaType,
    val tagId: Long
)