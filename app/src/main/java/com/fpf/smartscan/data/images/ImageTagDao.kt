package com.fpf.smartscan.data.images

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageTagDao {
    @Query("SELECT * FROM image_tag")
    fun getAllFlow(): Flow<List<ImageTag>>

    @Query("SELECT * FROM image_tag")
    suspend fun getAll(): List<ImageTag>

    @Query("SELECT * FROM image_tag WHERE name = :name")
    suspend fun get(name: String): ImageTag?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(imageTag: ImageTag)

    @Update
    suspend fun update(imageTag: ImageTag)

    @Delete
    suspend fun delete(imageTag: ImageTag)
}
