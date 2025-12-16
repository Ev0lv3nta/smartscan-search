package com.fpf.smartscan.data.videos

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fpf.smartscan.search.MediaTag

@Entity(tableName = "video_tag")
data class VideoTag(
    @PrimaryKey
    override val name: String,
    override val createdAt: Long = System.currentTimeMillis(),
    override val lastUsedAt: Long? = null,
    override val cohesionScore: Float? = null,
    override val nPrototype: Int = 0
): MediaTag
