package com.fpf.smartscan.data.tags

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query("SELECT * FROM media_tag")
    fun getAllFlow(): Flow<List<Tag>>

    @Query("SELECT * FROM media_tag")
    suspend fun getAll(): List<Tag>

    @Query("SELECT * FROM media_tag WHERE name in (:names)")
    suspend fun getByNames(names: List<String>): List<Tag>

    @Query("SELECT * FROM media_tag WHERE id in (:ids)")
    suspend fun getByIds(ids: List<Long>): List<Tag>

    // MUST use ignore. Using replace will cause cascading deletes of cross refs
    @Transaction
    @Insert(onConflict = OnConflictStrategy.Companion.IGNORE)
    suspend fun insert(imageTags: List<Tag>): List<Long>

    @Update
    suspend fun update(imageTags: List<Tag>)

    @Delete
    suspend fun delete(imageTags: List<Tag>)

    @Query("DELETE FROM media_tag WHERE name in (:names)")
    suspend fun deleteByNames(names: List<String>)

    @Query("DELETE FROM media_tag WHERE id in (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM media_tag")
    suspend fun clear()
}