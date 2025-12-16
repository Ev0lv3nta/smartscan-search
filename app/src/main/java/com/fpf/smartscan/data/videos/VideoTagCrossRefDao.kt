package com.fpf.smartscan.data.videos

import androidx.room.*

@Dao
interface VideoTagCrossRefDao {
    @Query("SELECT videoId FROM video_tag_crossref WHERE tag = :tag")
    suspend fun getVideoIds(tag: String): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tags: List<VideoTagCrossRef>)

    @Query("DELETE FROM video_tag_crossref WHERE videoId IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM video_tag_crossref WHERE tag IN (:tags)")
    suspend fun deleteByTags(tags: List<String>)

    @Query("DELETE FROM video_tag_crossref")
    suspend fun clear()
}
