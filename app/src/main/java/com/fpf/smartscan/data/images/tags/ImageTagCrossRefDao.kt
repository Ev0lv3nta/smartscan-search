package com.fpf.smartscan.data.images.tags

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.fpf.smartscan.data.TagWithCount
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageTagCrossRefDao {
    @Query("SELECT DISTINCT tagId FROM image_tag_crossref WHERE mediaId = :imageId")
    suspend fun getTagsForImage(imageId: Long): List<Long>
    @Query("SELECT mediaId FROM image_tag_crossref WHERE tagId = :tagId")
    suspend fun getImageIds(tagId: Long): List<Long>

    @Query("SELECT * FROM image_tag_crossref")
    suspend fun getAllCrossRefs(): List<ImageTagCrossRef>

    @Query("SELECT mediaId FROM image_tag_crossref WHERE tagId = :tagId LIMIT :limit OFFSET :offset")
    suspend fun getImageIds(tagId: Long, limit: Int, offset: Int): List<Long>

    @Transaction
    @Insert(onConflict = OnConflictStrategy.Companion.IGNORE)
    suspend fun insert(tags: List<ImageTagCrossRef>)

    @Transaction
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun upsert(tags: List<ImageTagCrossRef>)

    @Transaction
    @Query("DELETE FROM image_tag_crossref WHERE mediaId IN (:ids)")
    suspend fun deleteByMediaIds(ids: List<Long>)

    @Transaction
    @Query("DELETE FROM image_tag_crossref WHERE tagId IN (:tagIds)")
    suspend fun deleteByTags(tagIds: List<Long>)

    @Query("DELETE FROM image_tag_crossref")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM image_tag_crossref WHERE tagId = :tagId ")
    suspend fun count(tagId: Long): Int

    @Query("""
    SELECT t.*, COUNT(c.mediaId) AS count
    FROM image_tag_crossref c
    JOIN image_tag t ON t.id = c.tagId
    GROUP BY c.tagId
    ORDER BY count DESC
    """)
    fun getTagCounts(): Flow<List<TagWithCount>>
}