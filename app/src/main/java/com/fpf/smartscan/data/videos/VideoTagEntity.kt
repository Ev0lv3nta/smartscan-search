package com.fpf.smartscan.data.videos

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fpf.smartscan.data.MediaTag

@Entity(tableName = "video_tag")
data class VideoTag(
    @PrimaryKey
    override val name: String,
    override val lastUsedAt: Long? = null,
): MediaTag(name, lastUsedAt)
