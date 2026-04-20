package com.fpf.smartscan.data.old.videos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoTagDao {
    @Query("SELECT * FROM video_tag ORDER BY createdAt")
    suspend fun getAll(): List<VideoTag>
}
