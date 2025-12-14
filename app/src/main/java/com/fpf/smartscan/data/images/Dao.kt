package com.fpf.smartscan.data.images

import androidx.room.*

@Dao
interface ImageTagDao {
    @Query("SELECT imageId FROM image_tag WHERE tag = :tag")
    suspend fun getImageIds(tag: String): List<Long>

    @Query("SELECT DISTINCT tag FROM image_tag")
    suspend fun getTags(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(tags: List<ImageTag>)

    @Query("DELETE FROM image_tag WHERE imageId IN (:ids)")
    suspend fun delete(ids: List<Long>)

    @Query("DELETE FROM image_tag WHERE tag IN (:tags)")
    suspend fun delete(tags: List<String>)

    @Query("DELETE FROM image_tag")
    suspend fun clear()
}
