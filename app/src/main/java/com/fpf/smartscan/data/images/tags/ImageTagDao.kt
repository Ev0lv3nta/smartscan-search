package com.fpf.smartscan.data.images.tags

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
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

    // MUST use ignore. Using replace will cause cascading deletes of cross refs
    @Transaction
    @Insert(onConflict = OnConflictStrategy.Companion.IGNORE)
    suspend fun insert(imageTags: List<ImageTag>)

    @Update
    suspend fun update(imageTags: List<ImageTag>)

    @Delete
    suspend fun delete(imageTag: ImageTag)

    @Query("DELETE FROM image_tag WHERE name = :name")
    suspend fun deleteByName(name: String)

    @Query("DELETE FROM image_tag")
    suspend fun clear()
}