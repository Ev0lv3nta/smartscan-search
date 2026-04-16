package com.fpf.smartscan.data.tags

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.fpf.smartscan.media.MediaType
import kotlinx.coroutines.flow.Flow

@Dao
interface TagCrossRefDao {
    @Query("SELECT DISTINCT tagId FROM tag_crossref WHERE mediaId = :mediaId")
    suspend fun getTagsForMedia(mediaId: Long): List<Long>
    @Query("SELECT * FROM tag_crossref WHERE tagId = :tagId")
    suspend fun getByTag(tagId: Long): List<TagCrossRef>

    @Query("SELECT * FROM tag_crossref WHERE tagId = :tagId LIMIT :limit OFFSET :offset")
    suspend fun getByTag(tagId: Long, limit: Int, offset: Int): List<TagCrossRef>

    @Query("SELECT * FROM tag_crossref WHERE tagId = :tagId AND type =:type")
    suspend fun getByTagAndType(tagId: Long, type: MediaType): List<TagCrossRef>

    @Query("SELECT * FROM tag_crossref WHERE tagId = :tagId  AND type =:type LIMIT :limit OFFSET :offset")
    suspend fun getByTagAndType(tagId: Long, type: MediaType, limit: Int, offset: Int): List<TagCrossRef>


    @Query("SELECT mediaId FROM tag_crossref WHERE tagId = :tagId")
    fun getMediaIdsFlow(tagId: Long): Flow<List<Long>>

    @Query("SELECT * FROM tag_crossref")
    suspend fun getAllCrossRefs(): List<TagCrossRef>

    @Transaction
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun upsert(tags: List<TagCrossRef>)

    @Transaction
    @Query("DELETE FROM tag_crossref WHERE mediaId IN (:ids)")
    suspend fun deleteByMediaIds(ids: List<Long>)

    @Transaction
    @Query("DELETE FROM tag_crossref WHERE mediaId IN (:ids) AND tagId = :tagId")
    suspend fun deleteMediaMatchingTag(ids: List<Long>, tagId: Long)

    @Transaction
    @Query("DELETE FROM tag_crossref WHERE tagId IN (:tagIds)")
    suspend fun deleteByTags(tagIds: List<Long>)

    @Query("DELETE FROM tag_crossref")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM tag_crossref WHERE tagId = :tagId ")
    suspend fun count(tagId: Long): Int

    @Query("""
    SELECT t.*, COUNT(c.mediaId) AS count
    FROM tag_crossref c
    JOIN media_tag t ON t.id = c.tagId
    GROUP BY c.tagId
    ORDER BY count DESC
    """)
    fun getTagCounts(): Flow<List<TagWithCount>>
}