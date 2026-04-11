package com.fpf.smartscan.data.videos.tags

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.fpf.smartscan.data.MediaTag

@Entity(tableName = "video_tag", indices = [Index(value = ["name"], unique = true)])
data class VideoTag(
    @PrimaryKey(autoGenerate = true)
    override val id: Long = 0,
    override val name: String,
    override val lastUsedAt: Long? = null,
): MediaTag
