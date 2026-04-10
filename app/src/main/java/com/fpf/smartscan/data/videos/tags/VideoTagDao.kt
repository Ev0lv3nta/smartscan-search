package com.fpf.smartscan.data.videos.tags

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
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

    // MUST use ignore. Using replace will cause cascading deletes of cross refs
    @Transaction
    @Insert(onConflict = OnConflictStrategy.Companion.IGNORE)
    suspend fun insert(videoTags: List<VideoTag>)

    @Update
    suspend fun update(videoTags: List<VideoTag>)

    @Delete
    suspend fun delete(videoTag: VideoTag)

    @Query("DELETE FROM video_tag")
    suspend fun clear()
}