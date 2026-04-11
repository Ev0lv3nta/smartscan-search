package com.fpf.smartscan.data.videos.tags

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.fpf.smartscan.data.TagCount
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoTagCrossRefDao {
    @Query("SELECT DISTINCT tag FROM video_tag_crossref WHERE mediaId = :videoId")
    suspend fun getTagsForVideo(videoId: Long): List<String>

    @Query("SELECT mediaId FROM video_tag_crossref WHERE tag = :tag")
    suspend fun getVideoIds(tag: String): List<Long>

    @Query("SELECT mediaId FROM video_tag_crossref WHERE tag = :tag LIMIT :limit OFFSET :offset")
    suspend fun getVideoIds(tag: String, limit: Int, offset: Int): List<Long>

    @Query("SELECT * FROM video_tag_crossref")
    suspend fun getAllCrossRefs(): List<VideoTagCrossRef>

    @Transaction
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun upsert(tags: List<VideoTagCrossRef>)

    @Transaction
    @Query("DELETE FROM video_tag_crossref WHERE mediaId IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Transaction
    @Query("DELETE FROM video_tag_crossref WHERE tag IN (:tags)")
    suspend fun deleteByTags(tags: List<String>)

    @Query("DELETE FROM video_tag_crossref")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM video_tag_crossref WHERE tag = :tag")
    suspend fun count(tag: String): Int

    @Query("SELECT tag, COUNT(mediaId) AS count FROM video_tag_crossref GROUP BY tag ORDER BY count DESC")
    fun getTagCounts(): Flow<List<TagCount>>
}