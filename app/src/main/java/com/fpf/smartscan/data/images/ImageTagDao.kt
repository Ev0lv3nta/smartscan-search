package com.fpf.smartscan.data.images

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageTagDao {
    @Query("SELECT * FROM image_tag ORDER BY createdAt")
    fun getAll(): Flow<List<ImageTag>>

    @Query("SELECT * FROM image_tag WHERE name = :name")
    suspend fun get(name: String): ImageTag?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(imageTag: ImageTag)

    @Delete
    suspend fun delete(imageTag: ImageTag)
}
