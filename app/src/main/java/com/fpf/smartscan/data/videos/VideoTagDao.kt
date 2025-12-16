package com.fpf.smartscan.data.videos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoTagDao {
    @Query("SELECT * FROM video_tag ORDER BY createdAt")
    fun getAll(): Flow<List<VideoTag>>

    @Query("SELECT * FROM video_tag WHERE name = :name")
    suspend fun get(name: String): VideoTag?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(videoTag: VideoTag)

    @Delete
    suspend fun delete(videoTag: VideoTag)
}
