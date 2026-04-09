package com.fpf.smartscan.data.videos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoTagDao {
    @Query("SELECT * FROM video_tag")
    fun getAllFlow(): Flow<List<VideoTag>>

    @Query("SELECT * FROM video_tag")
    fun getAll(): List<VideoTag>

    @Query("SELECT * FROM video_tag WHERE name = :name")
    suspend fun get(name: String): VideoTag?


    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(videoTag: VideoTag)


    @Update
    suspend fun update(videoTag: VideoTag)

    @Delete
    suspend fun delete(videoTag: VideoTag)
}
