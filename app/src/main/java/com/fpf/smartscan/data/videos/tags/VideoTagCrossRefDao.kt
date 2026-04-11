package com.fpf.smartscan.data.videos.tags

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.fpf.smartscan.data.TagWithCount
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoTagCrossRefDao {
    @Query("SELECT DISTINCT tagId FROM video_tag_crossref WHERE mediaId = :videoId")
    suspend fun getTagsForVideo(videoId: Long): List<Long>

    @Query("SELECT mediaId FROM video_tag_crossref WHERE tagId = :tagId")
    suspend fun getVideoIds(tagId: Long): List<Long>

    @Query("SELECT mediaId FROM video_tag_crossref WHERE tagId = :tagId LIMIT :limit OFFSET :offset")
    suspend fun getVideoIds(tagId: Long, limit: Int, offset: Int): List<Long>

    @Query("SELECT * FROM video_tag_crossref")
    suspend fun getAllCrossRefs(): List<VideoTagCrossRef>

    @Transaction
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun upsert(tags: List<VideoTagCrossRef>)

    @Transaction
    @Query("DELETE FROM video_tag_crossref WHERE mediaId IN (:ids)")
    suspend fun deleteByMediaIds(ids: List<Long>)

    @Transaction
    @Query("DELETE FROM video_tag_crossref WHERE tagId IN (:ids)")
    suspend fun deleteByTagIds(ids: List<Long>)

    @Query("DELETE FROM video_tag_crossref")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM video_tag_crossref WHERE tagId = :tagId")
    suspend fun count(tagId: Long): Int

    @Query("""
    SELECT t.id AS id, t.name AS name, COUNT(c.mediaId) AS count    
    FROM video_tag_crossref c
    JOIN video_tag t ON t.id = c.tagId
    GROUP BY c.tagId
    ORDER BY count DESC
    """)
    fun getTagCounts(): Flow<List<TagWithCount>>

    @Query("""
    UPDATE video_tag_crossref
    SET tagId = :primaryTag
    WHERE tagId IN (:tagsToMerge)
    """)
    suspend fun mergeTags(primaryTag: Long, tagsToMerge: List<Long>)
}