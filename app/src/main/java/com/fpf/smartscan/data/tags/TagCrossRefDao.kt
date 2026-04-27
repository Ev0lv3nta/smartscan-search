package com.fpf.smartscan.data.tags

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TagCrossRefDao {

    @Query("SELECT DISTINCT tagId FROM tag_crossref WHERE mediaId = :mediaId")
    suspend fun getTagsForMedia(mediaId: Long): List<Long>

    @Query("SELECT * FROM tag_crossref")
    suspend fun getAllCrossRefs(): List<TagCrossRef>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tags: List<TagCrossRef>)

    @Query("DELETE FROM tag_crossref WHERE mediaId IN (:mediaIds)")
    suspend fun deleteByMediaIds(mediaIds: List<Long>)

    @Query("DELETE FROM tag_crossref WHERE mediaId IN (:mediaIds) AND tagId = :tagId")
    suspend fun deleteMediaMatchingTag(mediaIds: List<Long>, tagId: Long)

    @Query("DELETE FROM tag_crossref WHERE tagId IN (:tagIds)")
    suspend fun deleteByTags(tagIds: List<Long>)

    @Query("DELETE FROM tag_crossref")
    suspend fun clear()

    @Query("""
        SELECT t.*, COUNT(c.mediaId) AS count
        FROM tag_crossref c
        JOIN media_tag t ON t.id = c.tagId
        GROUP BY c.tagId
        ORDER BY count DESC
    """)
    fun getTagCounts(): Flow<List<TagWithCount>>
}