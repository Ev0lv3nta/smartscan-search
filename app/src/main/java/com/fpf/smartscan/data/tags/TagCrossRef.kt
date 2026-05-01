package com.fpf.smartscan.data.tags

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.TypeConverters
import com.fpf.smartscan.data.MediaTypeConverter
import com.fpf.smartscan.data.metadata.MediaMetadata
import com.fpf.smartscan.media.MediaType

@Entity(
    tableName = "tag_crossref",
    primaryKeys = ["mediaId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = Tag::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MediaMetadata::class,
            parentColumns = ["id"],
            childColumns = ["mediaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tagId")]
)
@TypeConverters(MediaTypeConverter::class)
data class TagCrossRef(
    val mediaId: Long,
    val tagId: Long,
)
