package com.fpf.smartscan.data.videos


import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "video_tag")
data class VideoTag(
    @PrimaryKey
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long? = null,
    val cohesionScore: Float? = null,
)
