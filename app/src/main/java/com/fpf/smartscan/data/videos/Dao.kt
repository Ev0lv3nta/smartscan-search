package com.fpf.smartscan.data.videos

import androidx.room.*

@Dao
interface VideoTagDao {
    @Query("SELECT videoId FROM video_tag WHERE tag = :tag")
    suspend fun getVideoIds(tag: String): List<Long>

    @Query("SELECT DISTINCT tag FROM video_tag")
    suspend fun getTags(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(tags: List<VideoTag>)

    @Query("DELETE FROM video_tag WHERE videoId IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM video_tag WHERE tag IN (:tags)")
    suspend fun deleteByTags(tags: List<String>)

    @Query("DELETE FROM video_tag")
    suspend fun clear()
}
