package com.fpf.smartscan.data.images

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageTagDao {
    @Query("SELECT imageId FROM image_tag WHERE tag = :tag")
    suspend fun getImageIds(tag: String): List<Long>

    @Query("SELECT DISTINCT tag FROM image_tag")
    suspend fun getTags(): List<String>

    @Query("SELECT DISTINCT tag FROM image_tag")
    fun getTagsFlow(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(tags: List<ImageTag>)

    @Query("DELETE FROM image_tag WHERE imageId IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM image_tag WHERE tag IN (:tags)")
    suspend fun deleteByTags(tags: List<String>)

    @Query("DELETE FROM image_tag")
    suspend fun clear()
}
